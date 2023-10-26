package io.github.semanticpie.orchestrator.orchestrator.agents;

import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.exceptions.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.ostis.scmemory.model.element.ScElement;
import org.ostis.scmemory.model.element.edge.ScEdge;
import org.ostis.scmemory.model.event.OnAddIngoingEdgeEvent;
import org.ostis.scmemory.model.exception.ScMemoryException;

@Slf4j
public class AgentImpl extends Agent {

    @Override
    public void subscribe() {
        try {
            context.findKeynode("nrel_format").ifPresent(subscriber -> {
                var event = new OnAddIngoingEdgeEvent(){

                    @Override
                    public void onEvent(ScElement source, ScEdge edge, ScElement target) {
                        log.info("ScElement [{}] ScEdge [{}] ScElement [{}]", source.getAddress(), edge.getAddress(), target.getAddress());
                        onEventDo(source, edge, target);
                    }
                };

                try {
                    context.subscribeOnEvent(subscriber, event);
                } catch (ScMemoryException e) {
                    throw new AgentException("Error while subscribing [" +  Agent.class + "] ", e);
                }
            });

        } catch (AgentException | ScMemoryException  e) {
            throw new AgentException(e);
        }
    }

    private void onEventDo(ScElement source, ScEdge edge, ScElement target) {
        // TODO
    }
}
