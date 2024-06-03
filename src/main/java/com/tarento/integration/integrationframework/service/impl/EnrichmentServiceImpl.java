package com.tarento.integration.integrationframework.service.impl;

import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;
import com.tarento.integration.integrationframework.service.EnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnrichmentServiceImpl implements EnrichmentService {

    @Override
    public void enrich(ExternalApiIntegrationDTO integrationDTO) {
        enrichDefaultHeader(integrationDTO);
    }

    //default values
    private void enrichDefaultHeader(ExternalApiIntegrationDTO integrationDTO) {
    }
}
