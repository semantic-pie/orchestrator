package io.github.semanticpie.orchestrator.config;

import io.github.semanticpie.orchestrator.orchestrator.Orchestrator;
import io.github.semanticpie.orchestrator.orchestrator.agents.AgentImpl;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;
import org.ostis.scmemory.websocketmemory.memory.SyncOstisScMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.net.URI;

@Slf4j
@Configuration
@ComponentScan("io.github.semanticpie.orchestrator")
public class AppConfiguration {

    @Value("${application.sc-machine.url}")
    private String scMachineURL;
    @Bean
    public DefaultScContext contextBean() throws Exception {
        var memory = new SyncOstisScMemory(new URI(scMachineURL));
        memory.open();
        return new DefaultScContext(memory);
    }

    @Bean
    public Orchestrator orchestratorBean() throws Exception {
        log.info("orchestratorBean created");
        return new Orchestrator(contextBean());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() throws Exception {
        log.info("doSomethingAfterStartup");
        Orchestrator orchestrator = orchestratorBean();
        orchestrator.addAgent(new AgentImpl());
        log.info("try listen:");
        orchestrator.listen();
    }
}
