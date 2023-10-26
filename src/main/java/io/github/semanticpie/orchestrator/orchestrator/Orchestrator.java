package io.github.semanticpie.orchestrator.orchestrator;

import io.github.semanticpie.orchestrator.orchestrator.exceptions.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Orchestrator implements Mediator {

    private final List<Agent> agents;
    private DefaultScContext context;

    public Orchestrator(DefaultScContext context) {
        this.context = context;
        this.agents = new ArrayList<>();
    }

    @Override
    public void addAgent(Agent agent) {
        agent.setContext(context);
        agents.add(agent);
    }

    @Override
    public void removeAgent(Agent agent) {
        agents.remove(agent);
    }

    public void listen() {
        bootstrap();
    }

    @SuppressWarnings("java:S2189")
    private void bootstrap() {
           while (true) {
               try {
                   Thread.sleep(Duration.ofSeconds(1).toMillis());
                   context.memory().open();
                   agents.forEach(Agent::subscribe);
                   do {
                       Thread.sleep(Duration.ofSeconds(1).toMillis());
                   } while (context.memory().isOpen());
               } catch (AgentException e) {
                   log.warn("failed subscribe agent: {}", e.getMessage());
               }  catch (Exception ignored) {}
           }
    }
}
