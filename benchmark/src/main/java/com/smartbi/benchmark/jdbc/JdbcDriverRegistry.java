package com.smartbi.benchmark.jdbc;

import com.smartbi.benchmark.config.BenchmarkJdbcProperties;
import com.smartbi.benchmark.domain.BenchmarkDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class JdbcDriverRegistry {

    private final BenchmarkJdbcProperties properties;
    private final ConcurrentMap<Path, URLClassLoader> jarLoaders = new ConcurrentHashMap<Path, URLClassLoader>();
    private final Set<String> registeredDrivers = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, Driver> driverDelegates = new ConcurrentHashMap<String, Driver>();

    public JdbcDriverRegistry(BenchmarkJdbcProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        ensureDriverDirectory();
        for (Path jar : listDriverPaths()) {
            loadDriversFromJar(jar);
        }
    }

    public List<String> listDriverJarNames() {
        return listDriverPaths().stream()
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
    }

    public String storeDriverJar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Driver upload requires a non-empty .jar file");
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new IllegalArgumentException("Only .jar driver uploads are supported");
        }

        Path target = ensureDriverDirectory().resolve(fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded driver jar " + fileName, e);
        }

        loadDriversFromJar(target);
        return fileName;
    }

    public DataSource createDataSource(BenchmarkDataSource definition) {
        return new DriverAwareDataSource(this, definition);
    }

    public Connection openConnection(BenchmarkDataSource definition) throws SQLException {
        return openConnection(
                definition.getDriverClass(),
                definition.getJdbcUrl(),
                definition.getJdbcUser(),
                definition.getJdbcPassword()
        );
    }

    public Connection openConnection(String driverClass, String jdbcUrl, String jdbcUser, String jdbcPassword)
            throws SQLException {
        ensureDriverAvailable(driverClass);
        Driver driver = resolveDriver(driverClass);
        if (driver == null) {
            throw new SQLException("Resolved JDBC driver is unavailable: " + driverClass);
        }

        Connection connection = driver.connect(
                jdbcUrl,
                buildConnectionProperties(jdbcUser, jdbcPassword)
        );
        if (connection == null) {
            throw new SQLException("JDBC driver " + driverClass + " does not accept URL " + jdbcUrl);
        }
        return connection;
    }

    public void ensureDriverAvailable(String driverClass) {
        if (driverClass == null || driverClass.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver class is required");
        }
        if (isDriverRegistered(driverClass)) {
            return;
        }

        try {
            Class.forName(driverClass);
            if (isDriverRegistered(driverClass)) {
                return;
            }
        } catch (ClassNotFoundException ignored) {
            // Continue with uploaded jars.
        }

        for (Path jar : listDriverPaths()) {
            loadDriversFromJar(jar);
            if (isDriverRegistered(driverClass)) {
                return;
            }
            registerDriverClassFromJar(jar, driverClass);
            if (isDriverRegistered(driverClass)) {
                return;
            }
        }

        throw new IllegalStateException("JDBC driver class not found on classpath or uploaded jars: " + driverClass);
    }

    private boolean isDriverRegistered(String driverClass) {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            if (driverClass.equals(drivers.nextElement().getClass().getName())) {
                return true;
            }
        }
        return registeredDrivers.contains(driverClass);
    }

    private void loadDriversFromJar(Path jar) {
        URLClassLoader loader = loaderFor(jar);
        try {
            ServiceLoader<Driver> serviceLoader = ServiceLoader.load(Driver.class, loader);
            for (Driver driver : serviceLoader) {
                registerDriver(driver);
            }
        } catch (ServiceConfigurationError e) {
            throw new IllegalStateException("Failed to load JDBC drivers from " + jar.getFileName(), e);
        }
    }

    private void registerDriverClassFromJar(Path jar, String driverClass) {
        URLClassLoader loader = loaderFor(jar);
        try {
            Class<?> candidate = Class.forName(driverClass, true, loader);
            if (!Driver.class.isAssignableFrom(candidate)) {
                throw new IllegalStateException("Uploaded class is not a JDBC driver: " + driverClass);
            }
            Driver driver = (Driver) candidate.getDeclaredConstructor().newInstance();
            registerDriver(driver);
        } catch (ClassNotFoundException ignored) {
            // This jar does not contain the requested driver class.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate uploaded driver " + driverClass, e);
        }
    }

    private void registerDriver(Driver driver) {
        String driverClass = driver.getClass().getName();
        if (!registeredDrivers.add(driverClass)) {
            return;
        }
        driverDelegates.put(driverClass, driver);
        try {
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (SQLException e) {
            registeredDrivers.remove(driverClass);
            driverDelegates.remove(driverClass);
            throw new IllegalStateException("Failed to register JDBC driver " + driverClass, e);
        }
    }

    private Driver resolveDriver(String driverClass) {
        Driver uploadedDriver = driverDelegates.get(driverClass);
        if (uploadedDriver != null) {
            return uploadedDriver;
        }

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driverClass.equals(driver.getClass().getName())) {
                return driver;
            }
        }

        try {
            Class<?> candidate = Class.forName(driverClass);
            if (!Driver.class.isAssignableFrom(candidate)) {
                return null;
            }
            return (Driver) candidate.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private URLClassLoader loaderFor(Path jar) {
        Path normalized = jar.toAbsolutePath().normalize();
        return jarLoaders.computeIfAbsent(normalized, this::newClassLoader);
    }

    private URLClassLoader newClassLoader(Path jar) {
        try {
            return new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid driver jar path " + jar, e);
        }
    }

    private List<Path> listDriverPaths() {
        Path driverDir = ensureDriverDirectory();
        if (!Files.exists(driverDir)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(driverDir)) {
            return stream
                    .filter(this::isJarFile)
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list driver jars under " + driverDir, e);
        }
    }

    private boolean isJarFile(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    private Path ensureDriverDirectory() {
        Path path = Path.of(properties.getDriverDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create driver directory " + path, e);
        }
        return path;
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver upload is missing a filename");
        }
        String fileName = Path.of(originalFileName).getFileName().toString();
        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver upload is missing a valid filename");
        }
        return fileName;
    }

    private java.util.Properties buildConnectionProperties(String jdbcUser, String jdbcPassword) {
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("user", jdbcUser != null ? jdbcUser : "");
        properties.setProperty("password", jdbcPassword != null ? jdbcPassword : "");
        return properties;
    }
}
