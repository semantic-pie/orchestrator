package io.github.semanticpie.orchestrator.orchestrator;

import lombok.Setter;
import org.ostis.api.context.DefaultScContext;

public abstract class Agent {

    @Setter
    protected DefaultScContext context;

    public void subscribe() {}
}
