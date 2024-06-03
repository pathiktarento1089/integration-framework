package com.tarento.integration.integrationframework.service;

import com.tarento.integration.integrationframework.model.ExternalApiIntegrationDTO;

public interface EnrichmentService {

    void enrich(ExternalApiIntegrationDTO integrationDTO);
}
