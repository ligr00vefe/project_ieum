package com.project.ieum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profiles/**")
                .addResourceLocations("file:" + toUrl("uploads/profiles"));
        registry.addResourceHandler("/uploads/profile_custom/**")
                .addResourceLocations("file:" + toUrl("uploads/profile_custom"));
        registry.addResourceHandler("/uploads/profile_thumb/**")
                .addResourceLocations("file:" + toUrl("uploads/profile_thumb"));
        registry.addResourceHandler("/uploads/notices/**")
                .addResourceLocations("file:" + toUrl("uploads/notices"));
    }

    private String toUrl(String relativePath) {
        return Paths.get(relativePath).toAbsolutePath() + "/";
    }
}
