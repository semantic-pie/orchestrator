package io.github.semanticpie.orchestrator.config;

import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.Orchestrator;
import io.github.semanticpie.orchestrator.orchestrator.agents.TrackAgent;
import io.github.semanticpie.orchestrator.services.TrackService;
import io.github.semanticpie.orchestrator.services.impl.TrackServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;
import org.ostis.scmemory.websocketmemory.memory.SyncOstisScMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@Configuration
@ComponentScan("io.github.semanticpie.orchestrator")
public class AppConfiguration {

    @Value("${application.sc-machine.url}")
    private String scMachineURL;

    @Bean
    public DefaultScContext contextBean() {
        try {
            var memory = new SyncOstisScMemory(new URI(scMachineURL));
            memory.open();
            return new DefaultScContext(memory);
        } catch (URISyntaxException e) {
            log.error("SC-Memory URI is invalid: {}", scMachineURL);
            throw new RuntimeException(e);
        } catch (Exception  e) {
            log.error("Connection error. Can't open sc-memory {}.", scMachineURL);
            throw new RuntimeException(e);
        }
    }

    @Bean
    public RestTemplate restTemplateBean() {
        return new RestTemplate();
    }
}
