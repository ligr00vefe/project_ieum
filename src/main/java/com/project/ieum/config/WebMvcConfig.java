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
                .addResourceLocations("file:" + Paths.get("uploads/profiles").toAbsolutePath() + "/");
        registry.addResourceHandler("/uploads/profile_custom/**")
                .addResourceLocations("file:" + Paths.get("uploads/profile_custom").toAbsolutePath() + "/");
        registry.addResourceHandler("/uploads/profile_thumb/**")
                .addResourceLocations("file:" + Paths.get("uploads/profile_thumb").toAbsolutePath() + "/");
        registry.addResourceHandler("/uploads/notices/**")
                .addResourceLocations("file:" + Paths.get("uploads/notices").toAbsolutePath() + "/");
    }
}
