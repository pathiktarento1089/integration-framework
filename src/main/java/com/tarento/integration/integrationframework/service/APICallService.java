package com.tarento.integration.integrationframework.service;

import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface APICallService {

     Mono makeExternalUploadApiCall(Flux<FilePart> files, ExternalApiIntegrationDTO externalApiIntegrationDTO,String fileKeys);
     Mono makeExternalApiCall(ExternalApiIntegrationDTO externalApiIntegrationDTO);
}
