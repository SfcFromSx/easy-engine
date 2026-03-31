package com.smartbi.engine.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestExceptionHandlerWebTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExplodingController())
            .setControllerAdvice(new RestExceptionHandler())
            .build();

    @Test
    // Covers RestExceptionHandler#handleGeneral through a controller request path.
    void checkedExceptionMapsToInternalServerErrorPayload() throws Exception {
        mockMvc.perform(get("/test/boom").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("boom"));
    }

    @RestController
    static class ExplodingController {

        @GetMapping("/test/boom")
        public String boom() throws Exception {
            throw new Exception("boom");
        }
    }
}
