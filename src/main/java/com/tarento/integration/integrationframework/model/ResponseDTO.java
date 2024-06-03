package com.tarento.integration.integrationframework.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

@RedisHash
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO {

    @JsonProperty("responseData")
    private Object responseData = null;

    @JsonProperty("id")
    private String id = null;
}
