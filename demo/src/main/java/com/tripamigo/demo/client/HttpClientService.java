package com.tripamigo.demo.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpClientService {
    
    private final RestTemplate restTemplate;
    
    public <T> T get(String url, Class<T> responseType) {
        return get(url, responseType, null);
    }
    
    public <T> T get(String url, Class<T> responseType, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            
            log.debug("Sending GET request to: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            
            log.debug("Received response with status: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP Client Error for GET request to {}: {}", url, e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            log.error("HTTP Server Error for GET request to {}: {}", url, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error for GET request to {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to execute GET request", e);
        }
    }
    
    public <T> T post(String url, Object requestBody, Class<T> responseType) {
        return post(url, requestBody, responseType, null);
    }
    
    public <T> T post(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(requestBody, httpHeaders);
            
            log.debug("Sending POST request to: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            
            log.debug("Received response with status: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP Client Error for POST request to {}: {}", url, e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            log.error("HTTP Server Error for POST request to {}: {}", url, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error for POST request to {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to execute POST request", e);
        }
    }
    
    public <T> T put(String url, Object requestBody, Class<T> responseType) {
        return put(url, requestBody, responseType, null);
    }
    
    public <T> T put(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(requestBody, httpHeaders);
            
            log.debug("Sending PUT request to: {}", url);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
            
            log.debug("Received response with status: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP Client Error for PUT request to {}: {}", url, e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            log.error("HTTP Server Error for PUT request to {}: {}", url, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error for PUT request to {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to execute PUT request", e);
        }
    }
    
    public void delete(String url) {
        delete(url, null);
    }
    
    public void delete(String url, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            
            log.debug("Sending DELETE request to: {}", url);
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            
            log.debug("Received response with status: {}", response.getStatusCode());
            
        } catch (HttpClientErrorException e) {
            log.error("HTTP Client Error for DELETE request to {}: {}", url, e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            log.error("HTTP Server Error for DELETE request to {}: {}", url, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error for DELETE request to {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to execute DELETE request", e);
        }
    }
} 