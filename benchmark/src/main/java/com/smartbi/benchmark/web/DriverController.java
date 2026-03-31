package com.smartbi.benchmark.web;

import com.smartbi.benchmark.jdbc.JdbcDriverRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final JdbcDriverRegistry driverRegistry;

    public DriverController(JdbcDriverRegistry driverRegistry) {
        this.driverRegistry = driverRegistry;
    }

    @GetMapping
    public List<String> listDrivers() {
        return driverRegistry.listDriverJarNames();
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadDriver(@RequestParam("file") MultipartFile file) {
        String fileName = driverRegistry.storeDriverJar(file);
        Map<String, String> response = new LinkedHashMap<String, String>();
        response.put("fileName", fileName);
        response.put("status", "UPLOADED");
        return response;
    }
}
