package com.tarento.integration.integrationframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.util.Map;

@RedisHash
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Builder
public class ExternalApiIntegrationDTO<T> {

    @JsonProperty("url")
    private String url = null;

    @JsonProperty("requestMethod")
    private RequestMethod requestMethod = null;

    @JsonProperty("requestHeader")
    private Map<String,String> requestHeader = null;

    @JsonProperty("requestBody")
    private Object requestBody = null;

    @JsonProperty("responseClassType")
    private T responseClassType = null;

    @JsonProperty("serviceCode")
    private String serviceCode = null;

    @JsonProperty("serviceName")
    private String serviceName = null;

    @JsonProperty("serviceDescription")
    private String serviceDescription = null;

    @JsonProperty("responseData")
    private Object responseData = null;

    @JsonProperty("operationType")
    private OperationType operationType = null;

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("strictCache")
    private boolean strictCache;

    @JsonProperty("strictCacheTimeInMinutes")
    private long strictCacheTimeInMinutes;

    @JsonProperty("alwaysDataReadFromCache")
    private boolean alwaysDataReadFromCache;

    public enum OperationType{
        PEER_TO_PEER("PEER_TO_PEER"),
        FIRE_AND_FORGET("FIRE_AND_FORGET");

        private String value;

        OperationType(String value) {
            this.value = value;
        }

        @JsonCreator
        public static OperationType fromValue(String text) {
            for (OperationType b : OperationType.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum RequestMethod {
        GET("GET"),
        HEAD("HEAD"),
        POST("POST"),
        PUT("PUT"),
        PATCH("PATCH"),
        DELETE("DELETE"),
        OPTIONS("OPTIONS"),
        TRACE("TRACE");

        private String value;

        RequestMethod(String value) {
            this.value = value;
        }

        @JsonCreator
        public static RequestMethod fromValue(String text) {
            for (RequestMethod b : RequestMethod.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }
}
