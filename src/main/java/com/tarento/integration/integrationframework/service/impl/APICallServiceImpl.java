package com.tarento.integration.integrationframework.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.integration.integrationframework.exception.CustomException;
import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;
import com.tarento.integration.integrationframework.model.ResponseDTO;
import com.tarento.integration.integrationframework.service.APICallService;
import com.tarento.integration.integrationframework.util.JWTTokenGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
public class APICallServiceImpl implements APICallService {
    private final ReactiveRedisOperations<String, ResponseDTO> cacheOps;

    public APICallServiceImpl(ReactiveRedisOperations<String, ResponseDTO> cacheOps) {
        this.cacheOps = cacheOps;
    }

    @Autowired
    private JWTTokenGeneratorUtil tokenGeneratorUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${cache.data.ttl}")
    public Long cacheDataTtl;
    @Value("${max.response.memory.size}")
    public int maxResponseMemorySize;

    @Override
    public Mono makeExternalUploadApiCall(Flux<FilePart> files, ExternalApiIntegrationDTO externalApiIntegrationDTO, String fileKeys) {
        log.info("APICallServiceImpl::makeExternalUploadApiCall");
        Flux<String> fileKeysFlux = Flux.fromArray(fileKeys.split(","));

        WebClient client = WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(maxResponseMemorySize))
                        .build())
                .build();

        HttpMethod httpMethod = HttpMethod.valueOf(externalApiIntegrationDTO.getRequestMethod().toString());
        String url = externalApiIntegrationDTO.getUrl();
        log.info("makeExternalUploadApiCall External API url {}", url);
        MultiValueMap<String, String> headers = convertToMultiValueMap(externalApiIntegrationDTO.getRequestHeader());
        log.info("makeExternalUploadApiCall External API headers {}", headers);
        Mono<ResponseDTO> responseMono = null;

        if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {
            MultipartBodyBuilder updatedBuilder = processFilesAndSendRequest(files, fileKeysFlux, externalApiIntegrationDTO);
            log.info("APICallServiceImpl::makeExternalUploadApiCall, builder value {}", updatedBuilder.build());
            responseMono = client.method(httpMethod)
                    .uri(url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(BodyInserters.fromMultipartData(updatedBuilder.build()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        ResponseDTO responseDTO = new ResponseDTO();
                        responseDTO.setResponseData(jsonNode);
                        log.info("makeExternalUploadApiCall External API responseDTO {}", responseDTO.getResponseData());
                        return responseDTO;
                    });

            updatedBuilder = new MultipartBodyBuilder();
        }
        return responseMono
                .doOnSuccess(responseDTO -> {
                    // Process the response body asynchronously

                    String token = tokenGeneratorUtil.generateRedisJwtTokenKeyForFile(files, externalApiIntegrationDTO.getRequestBody()
                            , externalApiIntegrationDTO.getUrl()
                            , externalApiIntegrationDTO.getOperationType().name());
                    log.info("makeExternalUploadApiCall token: " + token);
                    saveToRedis(externalApiIntegrationDTO, token, responseDTO);
                    log.info("makeExternalUploadApiCall successfully got response: " + responseDTO);
                })
                .doOnError(error -> {
                    // Handle any error that occurred during the request
                    log.error("error occurred while calling external API: " + url);

                    String httpStatusCode = null;
                    String updatedError = error.toString();
                    if (error instanceof WebClientResponseException) {
                        httpStatusCode = String.valueOf(((WebClientResponseException) error).getRawStatusCode());
                        updatedError = ((WebClientResponseException) error).getResponseBodyAsString();
                    }
                    throw new CustomException("EXTERNAL_SERVICE_CALL_ERROR", updatedError, httpStatusCode);
                });
    }

    private MultipartBodyBuilder processFilesAndSendRequest(Flux<FilePart> files, Flux<String> fileKeys, ExternalApiIntegrationDTO integrationDTO) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        if (integrationDTO.getRequestBody() != null) {
            JsonNode jsonNode = objectMapper.valueToTree(integrationDTO.getRequestBody());

            // Iterate over the JSON fields and add them to the bodyBuilder
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                String fieldValue = field.getValue().asText();

                // Add the field as a form field in the bodyBuilder
                bodyBuilder.part(fieldName, fieldValue);
            }
        }
        Flux<Tuple2<FilePart, String>> filePartsWithKeys = Flux.zip(files, fileKeys);
        // Process each pair (FilePart, key)
        filePartsWithKeys.subscribe(tuple -> {
            FilePart filePart = tuple.getT1();
            String key = tuple.getT2();
            log.info("processFilesAndSendRequest Key: " + key);
            log.info("processFilesAndSendRequest Filename: " + filePart.filename());

            // Add the file part and filename to the body builder
            bodyBuilder.part(key, filePart).filename(filePart.filename());
        });
        log.info("bodyBuilder {}", bodyBuilder);
        return bodyBuilder;
    }


    @Override
    public Mono<ResponseDTO> makeExternalApiCall(ExternalApiIntegrationDTO externalApiIntegrationDTO) {
        log.info("APICallServiceImpl::makeExternalApiCall");
        WebClient client = WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(maxResponseMemorySize))
                        .build())
                .build();

        HttpMethod httpMethod = HttpMethod.valueOf(externalApiIntegrationDTO.getRequestMethod().toString());
        String url = externalApiIntegrationDTO.getUrl();
        MultiValueMap<String, String> headers = convertToMultiValueMap(externalApiIntegrationDTO.getRequestHeader());
        Object requestBody = externalApiIntegrationDTO.getRequestBody();

        Mono<ResponseDTO> responseMono;
        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            responseMono = client.method(httpMethod)
                    .uri(url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        ResponseDTO responseDTO = new ResponseDTO();
                        responseDTO.setResponseData(jsonNode);
                        return responseDTO;
                    });
        } else {
            responseMono = client.method(httpMethod)
                    .uri(url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        ResponseDTO responseDTO = new ResponseDTO();
                        responseDTO.setResponseData(jsonNode);
                        return responseDTO;
                    });
        }


        return responseMono
                .doOnSuccess(responseDTO -> {
                    // Process the response body asynchronously
                    String token = tokenGeneratorUtil.generateRedisJwtTokenKey(externalApiIntegrationDTO.getRequestBody()
                            , externalApiIntegrationDTO.getUrl()
                            , externalApiIntegrationDTO.getOperationType().name());
                    log.info("token: " + token);
                    saveToRedis(externalApiIntegrationDTO, token, responseDTO);
                    log.info("successfully got response: " + responseDTO);
                })/*.map(exApiDto -> {
                    return re;
                })*/
                .doOnError(error -> {
                    // Handle any error that occurred during the request
                    log.error("error occurred while calling external API: " + url);

                    String httpStatusCode = null;
                    String updatedError = error.toString();
                    if (error instanceof WebClientResponseException) {
                        httpStatusCode = String.valueOf(((WebClientResponseException) error).getRawStatusCode());
                        updatedError = ((WebClientResponseException) error).getResponseBodyAsString();
                    }
                    throw new CustomException("EXTERNAL_SERVICE_CALL_ERROR", updatedError, httpStatusCode);
                });
    }

    private MultiValueMap<String, String> convertToMultiValueMap(Map<String, String> requestHeader) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.setAll(requestHeader);
        return multiValueMap;
    }

    public Mono<Boolean> saveToRedis(ExternalApiIntegrationDTO externalApiIntegrationDTO, String key, ResponseDTO responseDTO) {
        log.info("IntegrationServiceImpl::saveToRedis");
        Mono<Boolean> monoCacheData;
        if (externalApiIntegrationDTO.getStrictCacheTimeInMinutes() > 0 && externalApiIntegrationDTO.getStrictCacheTimeInMinutes() != -1) {
            log.info("cacheData value from user input {}",externalApiIntegrationDTO.getStrictCacheTimeInMinutes());
            monoCacheData = cacheOps.opsForValue().set(key, responseDTO, Duration.ofMinutes(externalApiIntegrationDTO.getStrictCacheTimeInMinutes()));
        } else {
            log.info("cacheData value from properties {}",cacheDataTtl);
            monoCacheData = cacheOps.opsForValue().set(key, responseDTO, Duration.ofMillis(cacheDataTtl));
        }
        monoCacheData.subscribe();
        return monoCacheData.doOnSuccess(
                ex -> {
                    responseDTO.getResponseData();
                    log.info("Data cached successfully! {}",responseDTO.getResponseData());
                }
        ).doOnError(ext -> {
            log.error("data didn't cache, Error occurred {}!", ext.toString());
        });
    }


}
