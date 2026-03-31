package com.smartbi.benchmark.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:benchmarkdriverupload;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "benchmark.jdbc.driver-dir=${user.dir}/target/test-drivers/uploaded"
})
@AutoConfigureMockMvc
class JdbcDriverUploadIntegrationTest {

    private static final Path DRIVER_DIR = Path.of("target/test-drivers/uploaded").toAbsolutePath().normalize();
    private static final Path BUILD_DIR = Path.of("target/test-drivers/build").toAbsolutePath().normalize();
    private static final Path LEGACY_DRIVER_DIR = Path.of("benchmark/target/test-drivers/uploaded").toAbsolutePath().normalize();
    private static final Path LEGACY_BUILD_DIR = Path.of("benchmark/target/test-drivers/build").toAbsolutePath().normalize();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        deleteDirectory(LEGACY_DRIVER_DIR);
        deleteDirectory(LEGACY_BUILD_DIR);
        deleteDirectory(DRIVER_DIR);
        deleteDirectory(BUILD_DIR);
        Files.createDirectories(DRIVER_DIR);
        Files.createDirectories(BUILD_DIR);
    }

    @Test
    void shouldUploadListAndUseDriverJar() throws Exception {
        Path driverJar = buildUploadedDriverJar();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "uploaded-h2-driver.jar",
                "application/java-archive",
                Files.readAllBytes(driverJar)
        );

        mockMvc.perform(multipart("/api/v1/drivers/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.fileName").value("uploaded-h2-driver.jar"));

        mockMvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("uploaded-h2-driver.jar"));

        String dsJson = "{"
                + "\"name\":\"uploaded-driver\","
                + "\"driverClass\":\"com.example.uploaded.UploadedH2Driver\","
                + "\"jdbcUrl\":\"jdbc:h2:mem:uploadeddriver;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE\","
                + "\"jdbcUser\":\"sa\","
                + "\"jdbcPassword\":\"\""
                + "}";

        mockMvc.perform(post("/api/v1/datasources/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dsJson))
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS"));
    }

    private Path buildUploadedDriverJar() throws Exception {
        Path sourceDir = BUILD_DIR.resolve("src/com/example/uploaded");
        Path classesDir = BUILD_DIR.resolve("classes");
        Files.createDirectories(sourceDir);
        Files.createDirectories(classesDir);

        Path sourceFile = sourceDir.resolve("UploadedH2Driver.java");
        Files.writeString(
                sourceFile,
                "package com.example.uploaded;\n"
                        + "public class UploadedH2Driver extends org.h2.Driver {\n"
                        + "}\n",
                StandardCharsets.UTF_8
        );

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for JDBC upload integration test");

        int compileResult = compiler.run(
                null,
                null,
                null,
                "-cp",
                System.getProperty("java.class.path"),
                "-d",
                classesDir.toString(),
                sourceFile.toString()
        );
        if (compileResult != 0) {
            throw new IllegalStateException("Failed to compile uploaded JDBC driver test fixture");
        }

        Path jarPath = BUILD_DIR.resolve("uploaded-h2-driver.jar");
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            addClassEntry(classesDir, jarOutputStream, "com/example/uploaded/UploadedH2Driver.class");
            jarOutputStream.putNextEntry(new JarEntry("META-INF/services/java.sql.Driver"));
            jarOutputStream.write("com.example.uploaded.UploadedH2Driver\n".getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
        return jarPath;
    }

    private void addClassEntry(Path classesDir, JarOutputStream jarOutputStream, String relativePath) throws IOException {
        jarOutputStream.putNextEntry(new JarEntry(relativePath));
        jarOutputStream.write(Files.readAllBytes(classesDir.resolve(relativePath)));
        jarOutputStream.closeEntry();
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
                            throw new IllegalStateException("Failed to delete " + current, e);
                        }
                    });
        }
    }
}
