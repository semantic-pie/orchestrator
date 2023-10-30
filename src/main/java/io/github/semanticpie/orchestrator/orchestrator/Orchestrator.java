package io.github.semanticpie.orchestrator.orchestrator;

import io.github.semanticpie.orchestrator.config.ReopenTask;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

@Slf4j
public class Orchestrator implements Mediator {

    private final List<Agent> agents;
    private DefaultScContext context;
    private final Timer timer;

    public Orchestrator(DefaultScContext context) {
        this.context = context;
        this.agents = new ArrayList<>();
        this.timer = new Timer();
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
        this.timer.scheduleAtFixedRate(new ReopenTask(context, agents), 1000, 1000);
    }
}
