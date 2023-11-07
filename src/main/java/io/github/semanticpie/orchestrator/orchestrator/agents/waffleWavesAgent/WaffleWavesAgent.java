package io.github.semanticpie.orchestrator.orchestrator.agents.waffleWavesAgent;

import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.exceptions.AgentException;
import io.github.semanticpie.orchestrator.services.JmanticService;
import lombok.extern.slf4j.Slf4j;
import org.ostis.scmemory.model.element.ScElement;
import org.ostis.scmemory.model.element.edge.EdgeType;
import org.ostis.scmemory.model.element.edge.ScEdge;
import org.ostis.scmemory.model.element.node.NodeType;
import org.ostis.scmemory.model.element.node.ScNode;
import org.ostis.scmemory.model.event.OnAddOutgoingEdgeEvent;
import org.ostis.scmemory.model.exception.ScMemoryException;
import org.ostis.scmemory.model.pattern.ScPattern;
import org.ostis.scmemory.model.pattern.pattern3.ScPattern3Impl;
import org.ostis.scmemory.websocketmemory.memory.element.ScNodeImpl;
import org.ostis.scmemory.websocketmemory.memory.pattern.DefaultWebsocketScPattern;
import org.ostis.scmemory.websocketmemory.memory.pattern.SearchingPatternTriple;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.AliasPatternElement;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.FixedPatternElement;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.TypePatternElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WaffleWavesAgent extends Agent {

    private final WaffleWavesService waffleWavesService;

    private static final int PLAYLIST_SIZE  = 10;
    @Autowired
    public WaffleWavesAgent(JmanticService jmanticService, WaffleWavesService waffleWavesService) {
        this.waffleWavesService = waffleWavesService;
        this.jmanticService = jmanticService;
        this.context = jmanticService.getContext();
    }

    @Override
    public void subscribe() {
        try {
            context.resolveKeynode("waffle_waves_agent", NodeType.CONST);
        }catch (ScMemoryException e){
            log.error(e.getLocalizedMessage());
        }
        this.subscribe("waffle_waves_agent", (OnAddOutgoingEdgeEvent) this::onEventDo);
    }

    private void onEventDo(ScElement source, ScEdge edge, ScElement target) {
        try {
            log.info("WaffleWavesEvent");
            context.deleteElement(edge);
            waffleWavesService.loadGenreWeights(target);
//            List<ScElement> playlist =  waffleWavesService.createP2laylist(PLAYLIST_SIZE);
//
//           waffleWavesService.uploadPlaylist("flow_playlist", playlist, target);
            var oldPlaylist = waffleWavesService.getOldPlaylist("flow_playlist", target);
            log.info("Old playlist: {}", oldPlaylist);
            log.info("Playlist size: {}", oldPlaylist.size());
        } catch (ScMemoryException e) {
            throw new AgentException(e);
        }
    }

}
