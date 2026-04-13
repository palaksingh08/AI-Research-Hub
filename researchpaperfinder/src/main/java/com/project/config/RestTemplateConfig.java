package com.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RestTemplateConfig {
    
    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Value("${resttemplate.connect-timeout:10000}")
    private int connectTimeout;
    
    @Value("${resttemplate.read-timeout:30000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        log.info("⚙️ RestTemplate bean created with timeouts: connect={}ms, read={}ms", connectTimeout, readTimeout);
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        ClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
        
        RestTemplate restTemplate = new RestTemplate(bufferingFactory);
        log.info("✅ RestTemplate configured with proper timeouts");
        return restTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        log.info("⚙️ ObjectMapper bean created");
        return new ObjectMapper();
    }
}
