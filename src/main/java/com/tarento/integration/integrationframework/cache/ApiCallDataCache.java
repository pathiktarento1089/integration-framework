package com.tarento.integration.integrationframework.cache;

import com.tarento.integration.integrationframework.model.ResponseDTO;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;

@Component
public class ApiCallDataCache {

    private final ReactiveRedisConnectionFactory factory;
    private final ReactiveRedisOperations<String, ResponseDTO> cacheOps;

    public ApiCallDataCache(ReactiveRedisConnectionFactory factory, ReactiveRedisOperations<String, ResponseDTO> cacheOps) {
        this.factory = factory;
        this.cacheOps = cacheOps;
    }
}

