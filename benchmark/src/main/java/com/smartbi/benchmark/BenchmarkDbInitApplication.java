package com.smartbi.benchmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class BenchmarkDbInitApplication {

    private BenchmarkDbInitApplication() {
    }

    public static void main(String[] args) {
        String[] effectiveArgs = new String[args.length + 3];
        System.arraycopy(args, 0, effectiveArgs, 0, args.length);
        effectiveArgs[args.length] = "--spring.flyway.enabled=true";
        effectiveArgs[args.length + 1] = "--spring.jpa.hibernate.ddl-auto=none";
        effectiveArgs[args.length + 2] = "--spring.main.web-application-type=none";
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BenchmarkApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.lazy-initialization=true")
                .run(effectiveArgs);
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
