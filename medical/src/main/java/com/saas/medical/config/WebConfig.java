package com.saas.medical.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.format.Formatter;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS configuration moved to SecurityConfig to avoid conflicts
    /*
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                    "http://localhost:3000",    // React dev server
                    "http://localhost:5173",    // Vite dev server
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:5173",
                    "https://your-domain.com"   // Production domain
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight response for 1 hour
    }
    */

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new LocalDateFormatter());
        registry.addFormatter(new LocalTimeFormatter());
    }

    public static class LocalDateFormatter implements Formatter<LocalDate> {
        @Override
        public LocalDate parse(String text, Locale locale) {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        }

        @Override
        public String print(LocalDate object, Locale locale) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(object);
        }
    }

    public static class LocalTimeFormatter implements Formatter<LocalTime> {
        @Override
        public LocalTime parse(String text, Locale locale) {
            return LocalTime.parse(text, DateTimeFormatter.ISO_LOCAL_TIME);
        }

        @Override
        public String print(LocalTime object, Locale locale) {
            return DateTimeFormatter.ISO_LOCAL_TIME.format(object);
        }
    }
}
