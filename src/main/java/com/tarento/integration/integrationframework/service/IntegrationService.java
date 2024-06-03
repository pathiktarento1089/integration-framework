package com.tarento.integration.integrationframework.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;
import com.tarento.integration.integrationframework.model.ResponseDTO;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IntegrationService {

    Mono<ResponseDTO> createExternalAPICall(ExternalApiIntegrationDTO integrationDTO);

    Mono<ResponseDTO> createExternalUploadAPICall(Flux<FilePart> files, String integrationModel,String fileKeys) throws JsonProcessingException;
}
