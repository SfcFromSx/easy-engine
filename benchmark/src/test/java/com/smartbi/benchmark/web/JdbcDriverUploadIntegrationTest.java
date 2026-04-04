package com.smartbi.benchmark.web;

import com.smartbi.benchmark.support.BenchmarkSpringTestOverrides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JdbcDriverUploadIntegrationTest {

    private static final Path DRIVER_DIR = Paths.get("target/test-drivers/uploaded").toAbsolutePath().normalize();
    private static final Path BUILD_DIR = Paths.get("target/test-drivers/build").toAbsolutePath().normalize();
    private static final Path LEGACY_DRIVER_DIR = Paths.get("benchmark/target/test-drivers/uploaded").toAbsolutePath().normalize();
    private static final Path LEGACY_BUILD_DIR = Paths.get("benchmark/target/test-drivers/build").toAbsolutePath().normalize();
    private static final String FILE_FIELD = "file";
    private static final String UPLOADED_DRIVER_JAR = "uploaded-h2-driver.jar";
    private static final String JAVA_ARCHIVE_MEDIA_TYPE = "application/java-archive";
    private static final String DRIVERS_UPLOAD_ENDPOINT = "/api/v1/drivers/upload";
    private static final String DRIVERS_ENDPOINT = "/api/v1/drivers";
    private static final String DATASOURCE_TEST_ENDPOINT = "/api/v1/datasources/test";
    private static final String UPLOADED_STATUS = "UPLOADED";
    private static final String SUCCESS = "SUCCESS";
    private static final String SOURCE_DIR = "src/com/example/uploaded";
    private static final String CLASSES_DIR = "classes";
    private static final String SOURCE_FILE = "UploadedH2Driver.java";
    private static final String DRIVER_CLASS_SOURCE =
            "package com.example.uploaded;\n"
                    + "public class UploadedH2Driver extends org.h2.Driver {\n"
                    + "}\n";
    private static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";
    private static final String JAVA_RELEASE_FLAG = "--release";
    private static final String JAVA_SOURCE_FLAG = "-source";
    private static final String JAVA_TARGET_FLAG = "-target";
    private static final String JAVA_VERSION = "8";
    private static final String CLASSPATH_FLAG = "-cp";
    private static final String OUTPUT_DIR_FLAG = "-d";
    private static final String JAVA_CLASS_PATH = "java.class.path";
    private static final String COMPILE_ERROR = "Failed to compile uploaded JDBC driver test fixture";
    private static final String CLASS_ENTRY = "com/example/uploaded/UploadedH2Driver.class";
    private static final String DRIVER_SERVICE_ENTRY = "META-INF/services/java.sql.Driver";
    private static final String DRIVER_SERVICE_CONTENT = "com.example.uploaded.UploadedH2Driver\n";
    private static final String JDK_COMPILER_REQUIRED = "JDK compiler is required for JDBC upload integration test";
    private static final String DELETE_ERROR_PREFIX = "Failed to delete ";

    @Autowired
    private MockMvc mockMvc;

    @Value("${benchmark.test.jdbc-upload.test-datasource.name}")
    private String datasourceName;

    @Value("${benchmark.test.jdbc-upload.test-datasource.driver-class}")
    private String datasourceDriverClass;

    @Value("${benchmark.test.jdbc-upload.test-datasource.jdbc-url}")
    private String datasourceJdbcUrl;

    @Value("${benchmark.test.jdbc-upload.test-datasource.jdbc-user}")
    private String datasourceJdbcUser;

    @Value("${benchmark.test.jdbc-upload.test-datasource.jdbc-password:}")
    private String datasourceJdbcPassword;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        BenchmarkSpringTestOverrides.register(registry);
    }

    @BeforeEach
    void setUp() throws Exception {
        deleteDirectory(LEGACY_DRIVER_DIR);
        deleteDirectory(LEGACY_BUILD_DIR);
        deleteDirectory(DRIVER_DIR);
        deleteDirectory(BUILD_DIR);
        Files.createDirectories(DRIVER_DIR);
        Files.createDirectories(BUILD_DIR);
    }

    // Covers DriverController#uploadDriver, #listDrivers, and DataSourceController#testConnection with uploaded JDBC jars.
    @Test
    void shouldUploadListAndUseDriverJar() throws Exception {
        Path driverJar = buildUploadedDriverJar();

        MockMultipartFile file = new MockMultipartFile(
                FILE_FIELD,
                UPLOADED_DRIVER_JAR,
                JAVA_ARCHIVE_MEDIA_TYPE,
                Files.readAllBytes(driverJar)
        );

        mockMvc.perform(multipart(DRIVERS_UPLOAD_ENDPOINT).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(UPLOADED_STATUS))
                .andExpect(jsonPath("$.fileName").value(UPLOADED_DRIVER_JAR));

        mockMvc.perform(get(DRIVERS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(UPLOADED_DRIVER_JAR));

        mockMvc.perform(post(DATASOURCE_TEST_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(datasourceJson()))
                .andExpect(status().isOk())
                .andExpect(content().string(SUCCESS));
    }

    private String datasourceJson() {
        return "{"
                + "\"name\":\"" + datasourceName + "\","
                + "\"driverClass\":\"" + datasourceDriverClass + "\","
                + "\"jdbcUrl\":\"" + datasourceJdbcUrl + "\","
                + "\"jdbcUser\":\"" + datasourceJdbcUser + "\","
                + "\"jdbcPassword\":\"" + datasourceJdbcPassword + "\""
                + "}";
    }

    private Path buildUploadedDriverJar() throws Exception {
        Path sourceDir = BUILD_DIR.resolve(SOURCE_DIR);
        Path classesDir = BUILD_DIR.resolve(CLASSES_DIR);
        Files.createDirectories(sourceDir);
        Files.createDirectories(classesDir);

        Path sourceFile = sourceDir.resolve(SOURCE_FILE);
        Files.write(
                sourceFile,
                DRIVER_CLASS_SOURCE.getBytes(StandardCharsets.UTF_8)
        );

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, JDK_COMPILER_REQUIRED);

        List<String> compilerArgs = compilerArgs(classesDir, sourceFile);
        int compileResult = compiler.run(null, null, null, compilerArgs.toArray(new String[0]));
        if (compileResult != 0) {
            throw new IllegalStateException(COMPILE_ERROR);
        }

        Path jarPath = BUILD_DIR.resolve(UPLOADED_DRIVER_JAR);
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            addClassEntry(classesDir, jarOutputStream, CLASS_ENTRY);
            jarOutputStream.putNextEntry(new JarEntry(DRIVER_SERVICE_ENTRY));
            jarOutputStream.write(DRIVER_SERVICE_CONTENT.getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
        return jarPath;
    }

    private void addClassEntry(Path classesDir, JarOutputStream jarOutputStream, String relativePath) throws IOException {
        jarOutputStream.putNextEntry(new JarEntry(relativePath));
        jarOutputStream.write(Files.readAllBytes(classesDir.resolve(relativePath)));
        jarOutputStream.closeEntry();
    }

    private List<String> compilerArgs(Path classesDir, Path sourceFile) {
        List<String> args = new ArrayList<>();
        if (isJava8Compiler()) {
            args.add(JAVA_SOURCE_FLAG);
            args.add(JAVA_VERSION);
            args.add(JAVA_TARGET_FLAG);
            args.add(JAVA_VERSION);
        } else {
            args.add(JAVA_RELEASE_FLAG);
            args.add(JAVA_VERSION);
        }
        args.add(CLASSPATH_FLAG);
        args.add(System.getProperty(JAVA_CLASS_PATH));
        args.add(OUTPUT_DIR_FLAG);
        args.add(classesDir.toString());
        args.add(sourceFile.toString());
        return args;
    }

    private boolean isJava8Compiler() {
        return "1.8".equals(System.getProperty(JAVA_SPECIFICATION_VERSION));
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(current -> {
                        try {
                            Files.deleteIfExists(current);
                        } catch (IOException e) {
                            throw new IllegalStateException(DELETE_ERROR_PREFIX + current, e);
                        }
                    });
        }
    }
}
