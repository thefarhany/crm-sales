package com.thefarhany.crmsales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/contracts}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:4200",
                        "http://localhost:8080",
                        "http://52.220.35.232:4200",
                        "http://52.220.35.232"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/uploads/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:4200",
                        "http://localhost:8080",
                        "http://52.220.35.232:4200",
                        "http://52.220.35.232"
                )
                .allowedMethods("GET")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir)
                .toAbsolutePath()
                .getParent()
                .normalize()
                .toString()
                .replace("\\", "/");

        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }

        System.out.println("📁 Serving uploads from: file:///" + absolutePath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + absolutePath);
    }
}
