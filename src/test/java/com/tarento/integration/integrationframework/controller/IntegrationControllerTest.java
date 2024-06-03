package com.tarento.integration.integrationframework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;
import com.tarento.integration.integrationframework.model.ResponseDTO;
import com.tarento.integration.integrationframework.producer.Producer;
import com.tarento.integration.integrationframework.service.APICallService;
import com.tarento.integration.integrationframework.service.EnrichmentService;
import com.tarento.integration.integrationframework.service.impl.IntegrationServiceImpl;
import com.tarento.integration.integrationframework.util.JWTTokenGeneratorUtil;
import com.tarento.integration.integrationframework.validator.IntegrationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.LinkedHashMap;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = IntegrationController.class)
@Import(IntegrationServiceImpl.class)
class IntegrationControllerTest {

    @MockBean
    private IntegrationValidator integrationValidator;

    @MockBean
    private APICallService apiCallService;

    @MockBean
    private Producer producer;

    @MockBean
    private EnrichmentService enrichmentService;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private ReactiveRedisOperations<String, ResponseDTO> cacheOps;

    @MockBean
    private JWTTokenGeneratorUtil tokenGeneratorUtil;

    @MockBean
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

//    @Test
//    void createExternalAPICall_success() {
//
//        ExternalApiIntegrationDTO externalApiIntegrationDTO = new ExternalApiIntegrationDTO();
//        externalApiIntegrationDTO.setUrl("https://staging.sunbirded.org/getGeneralisedResourcesBundles/en/all_labels_en.json");
//
//        LinkedHashMap requestHeaderMap = new LinkedHashMap();
//        requestHeaderMap.put("content-type","application/json");
//        externalApiIntegrationDTO.setRequestHeader(requestHeaderMap);
//
//        externalApiIntegrationDTO.setRequestMethod(ExternalApiIntegrationDTO.RequestMethod.GET);
//        externalApiIntegrationDTO.setServiceCode("1234");
//        externalApiIntegrationDTO.setResponseClassType("Object");
//        externalApiIntegrationDTO.setOperationType(ExternalApiIntegrationDTO.OperationType.PEER_TO_PEER);
//
//        webClient.post()
//                .uri("/v1/create-external-call")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(BodyInserters.fromObject(externalApiIntegrationDTO))
//                .exchange()
//                .expectStatus().isOk();
//    }

    @Test
    void createExternalAPICall_error_withSimpleStringRequest() {

        ExternalApiIntegrationDTO externalApiIntegrationDTO = new ExternalApiIntegrationDTO();
        externalApiIntegrationDTO.setUrl("http://localhost:8081");
        externalApiIntegrationDTO.setRequestHeader(new LinkedHashMap<>());
        externalApiIntegrationDTO.setRequestMethod(ExternalApiIntegrationDTO.RequestMethod.GET);
        externalApiIntegrationDTO.setServiceCode("1234");
        externalApiIntegrationDTO.setResponseClassType("Object");

        webClient.post()
                .uri("/v1/create-external-call")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject("simple string"))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void createExternalAPICall_error() {

        ExternalApiIntegrationDTO externalApiIntegrationDTO = new ExternalApiIntegrationDTO();
        externalApiIntegrationDTO.setUrl("http://localhost:8081");
        externalApiIntegrationDTO.setRequestHeader(new LinkedHashMap<>());
        externalApiIntegrationDTO.setRequestMethod(ExternalApiIntegrationDTO.RequestMethod.GET);
        externalApiIntegrationDTO.setServiceCode("1234");
        externalApiIntegrationDTO.setResponseClassType("Object");

        webClient.post()
                .uri("/v1/create-external-call")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.empty())
                .exchange()
                .expectStatus().is5xxServerError();
    }
}