package com.egr.snookerrank.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class JacksonConfig {

//    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Double.class, new TwoDecimalSerializer()); // Apply to all double fields
        module.addSerializer(Float.class, new TwoDecimalSerializer());  // Apply to all float fields
        mapper.registerModule(module);
        return mapper;
    }
}
