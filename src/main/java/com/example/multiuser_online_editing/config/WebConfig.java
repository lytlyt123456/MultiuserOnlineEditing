package com.example.multiuser_online_editing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        String absolutePath = Paths.get(uploadPath.substring(2)).toAbsolutePath().toString();

        // System.out.println("配置静态资源映射:");
        // System.out.println("上传路径: " + uploadPath);
        // System.out.println("绝对路径: " + absolutePath);

        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + absolutePath + "/");

        // 同时添加一个备用映射（如果上面不行的话）
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}