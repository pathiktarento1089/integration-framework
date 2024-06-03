package com.tarento.integration.integrationframework.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import com.tarento.integration.integrationframework.exception.CustomException;
import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;
import com.tarento.integration.integrationframework.model.ResponseDTO;
import com.tarento.integration.integrationframework.producer.Producer;
import com.tarento.integration.integrationframework.service.APICallService;
import com.tarento.integration.integrationframework.service.EnrichmentService;
import com.tarento.integration.integrationframework.service.IntegrationService;
import com.tarento.integration.integrationframework.util.JWTTokenGeneratorUtil;
import com.tarento.integration.integrationframework.validator.IntegrationValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
public class IntegrationServiceImpl implements IntegrationService {

    @Autowired
    private IntegrationValidator integrationValidator;

    @Autowired
    private APICallService apiCallService;

    @Autowired
    private Producer producer;

    @Autowired
    private EnrichmentService enrichmentService;

    @Value("${integration.kafka.create.topic}")
    private String callExternalServiceTopic;

    @Autowired
    private JWTTokenGeneratorUtil tokenGeneratorUtil;
    @Autowired
    ObjectMapper objectMapper;
    private final ReactiveRedisOperations<String, ResponseDTO> cacheOps;

    public IntegrationServiceImpl(ReactiveRedisOperations<String, ResponseDTO> cacheOps) {
        this.cacheOps = cacheOps;
    }

    @Override
    public Mono<ResponseDTO> createExternalAPICall(ExternalApiIntegrationDTO integrationDTO) {
        log.info("IntegrationServiceImpl::createExternalAPICall");


        integrationValidator.validate(integrationDTO);
        enrichmentService.enrich(integrationDTO);

        String token = tokenGeneratorUtil.generateRedisJwtTokenKey(integrationDTO.getRequestBody()
                , integrationDTO.getUrl()
                , integrationDTO.getOperationType().name());
        if (integrationDTO.getOperationType() == ExternalApiIntegrationDTO.OperationType.FIRE_AND_FORGET) {
            try {
                UUID uuid = Generators.timeBasedGenerator().generate();
                String id = uuid.toString();
                integrationDTO.setId(id);

                producer.send(callExternalServiceTopic, integrationDTO);

                ResponseDTO responseDTO = new ResponseDTO();
                responseDTO.setId(id);
                return Mono.just(responseDTO);
            } catch (Exception e) {
                throw new CustomException("ERROR_IN_KAFKA_PRODUCER", e.getMessage());
            }

        } else {
            if (!integrationDTO.isStrictCache()) {//false
                log.info("Strict Cache set to false");
                log.info("Due cache false Data reading from externa api url: {}, calling external API", integrationDTO.getUrl());
                log.info("Due cache false Data reading from externa api token: {}, calling external API", token);
                return apiCallService.makeExternalApiCall(integrationDTO);
            } else {
                ResponseDTO responseDto= new ResponseDTO();
                if(integrationDTO.isAlwaysDataReadFromCache()){
                    log.info("Always data reading from Cache");
                    return cacheOps.opsForValue().get(token)
                            .doOnNext(data ->{
                                log.info("Data reading from Redis for url: {}", integrationDTO.getUrl());
                                log.info("Data reading from Redis for token: {}",token);
                            })
                            .switchIfEmpty(Mono.just(responseDto)
                                    .doOnNext(data->{
                                        log.info("Data not present in redis for the url {}", integrationDTO.getUrl());
                                        log.info("Data not present in redis for the token {}",token);
                                    }));

                }else {
                    return cacheOps.opsForValue().get(token)
                            .doOnNext(data -> {
                                log.info("Data reading from Redis for url: {}", integrationDTO.getUrl());
                                log.info("Data reading from Redis for token: {}", token);
                            })
                            .switchIfEmpty(
                                    apiCallService.makeExternalApiCall(integrationDTO)
                                            .doOnNext(data -> {
                                                log.info("Data not found in Redis for url: {}, calling external API", integrationDTO.getUrl());
                                                log.info("Data not found in Redis for token: {}, calling external API", token);
                                            })
                            );
                }

            }
        }

    }


    @Override
    public Mono<ResponseDTO> createExternalUploadAPICall(Flux<FilePart> files, String requestInput, String fileKeys) {
        log.info("IntegrationServiceImpl::createExternal Upload API Call");
        if (files == null || requestInput == null || fileKeys == null) {
            return Mono.error(new CustomException("ERROR", "Files, requestInput, and fileKeys must not be null"));
        }
        try {
            ExternalApiIntegrationDTO integrationDTO = new ObjectMapper().readValue(requestInput, ExternalApiIntegrationDTO.class);
            log.info("createExternal Upload API Call::integrationDTO {}", integrationDTO);
            enrichmentService.enrich(integrationDTO);

            String token = tokenGeneratorUtil.generateRedisJwtTokenKeyForFile(files, integrationDTO.getRequestBody()
                    , integrationDTO.getUrl()
                    , integrationDTO.getOperationType().name());
            log.info("createExternal Upload API Call::token {}", token);
            if (integrationDTO.getOperationType() == ExternalApiIntegrationDTO.OperationType.FIRE_AND_FORGET) {
                try {
                    UUID uuid = Generators.timeBasedGenerator().generate();
                    String id = uuid.toString();
                    integrationDTO.setId(id);

                    producer.send(callExternalServiceTopic, integrationDTO);

                    ResponseDTO responseDTO = new ResponseDTO();
                    responseDTO.setId(id);
                    return Mono.just(responseDTO);
                } catch (Exception e) {
                    throw new CustomException("ERROR_IN_KAFKA_PRODUCER", e.getMessage());
                }

            } else {
                if (!integrationDTO.isStrictCache()) {
                    log.info("createExternal Upload API Call::PEER_TO_PEER ,Strictcache false");
                    return apiCallService.makeExternalUploadApiCall(files, integrationDTO, fileKeys);
                } else {
                    return cacheOps.opsForValue().get(token).switchIfEmpty(
                            apiCallService.makeExternalUploadApiCall(files, integrationDTO, fileKeys));
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }
}
