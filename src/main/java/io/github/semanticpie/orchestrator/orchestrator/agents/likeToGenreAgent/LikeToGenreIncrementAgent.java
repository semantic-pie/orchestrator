package io.github.semanticpie.orchestrator.orchestrator.agents.likeToGenreAgent;

import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.agents.waffleWavesAgent.patterns.WaffleWavesPattern;
import io.github.semanticpie.orchestrator.services.CacheService;
import io.github.semanticpie.orchestrator.services.JmanticService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ostis.scmemory.model.element.ScElement;
import org.ostis.scmemory.model.element.edge.EdgeType;
import org.ostis.scmemory.model.element.edge.ScEdge;
import org.ostis.scmemory.model.element.link.LinkType;
import org.ostis.scmemory.model.element.link.ScLink;
import org.ostis.scmemory.model.element.link.ScLinkString;
import org.ostis.scmemory.model.element.node.NodeType;
import org.ostis.scmemory.model.element.node.ScNode;
import org.ostis.scmemory.model.event.OnAddOutgoingEdgeEvent;
import org.ostis.scmemory.model.exception.ScMemoryException;
import org.ostis.scmemory.websocketmemory.memory.element.ScLinkStringImpl;
import org.ostis.scmemory.websocketmemory.memory.element.ScNodeImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class LikeToGenreIncrementAgent extends Agent {
    private final LikeToGenreService service;

    @Autowired
    public LikeToGenreIncrementAgent(JmanticService jmanticService,  LikeToGenreService service) {
        this.service = service;
        this.jmanticService = jmanticService;
        this.context = jmanticService.getContext();
    }

    @Override
    public void subscribe() {
        this.subscribe("nrel_likes", (OnAddOutgoingEdgeEvent) this::onEventDo);
    }



    private void onEventDo(ScElement source, ScEdge edge, ScElement target) {
        try {
            service.incrementator(target, 1);
        } catch (ScMemoryException e) {
            throw new RuntimeException(e);
        }
    }
}
