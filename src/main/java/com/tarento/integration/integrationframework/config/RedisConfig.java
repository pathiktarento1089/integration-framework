package com.tarento.integration.integrationframework.config;

import com.tarento.integration.integrationframework.model.ResponseDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, ResponseDTO> redisOperations(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<ResponseDTO> serializer = new Jackson2JsonRedisSerializer<>(ResponseDTO.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, ResponseDTO> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, ResponseDTO> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}
