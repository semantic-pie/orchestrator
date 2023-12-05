package io.github.semanticpie.orchestrator.orchestrator.agents.likeToGenreAgent;

import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.agents.waffleWavesAgent.patterns.WaffleWavesPattern;
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
public class LikeToGenreAgent extends Agent {
    private final LikeToGenrePatterns patterns;

    @Autowired
    public LikeToGenreAgent(JmanticService jmanticService, LikeToGenrePatterns patterns) {
        this.patterns = patterns;
        this.jmanticService = jmanticService;
        this.context = jmanticService.getContext();
    }

    @Override
    public void subscribe() {
        this.subscribe("nrel_likes", (OnAddOutgoingEdgeEvent) this::onEventDo);
    }

    private void onEventDo(ScElement source, ScEdge edge, ScElement target) {


        try {
            var elements = context.find(patterns.patternOfSearchingNodes(target, NodeType.VAR_STRUCT, NodeType.VAR)).toList().get(0).toList();

            log.info(elements.toString());
            ScElement userNode = null;
            ScElement trackNode = null;

            for (ScElement element : elements) {
                if (element instanceof ScNodeImpl) {
                    switch (((ScNodeImpl) element).getType()) {
                        case VAR -> trackNode = element;
                        case VAR_STRUCT -> userNode = element;
                    }
                }
            }

            if (userNode != null && trackNode != null) {
                ScElement finalTrackNode = trackNode;
                ScElement conceptGenre = context.findKeynode("nrel_genre").orElseThrow();
                var genre = context.find(patterns.patternOfSearchingTrackGenre(trackNode, conceptGenre))
                        .findFirst().orElseThrow()
                        .filter(scElement -> scElement instanceof ScNodeImpl)
                        .filter(scElement -> !scElement.equals(finalTrackNode))
                        .filter(scElement -> !scElement.equals(conceptGenre)).findFirst().orElseThrow();

                System.out.println("Genre: " + genre);

                ScNode nrelWeight = context.resolveKeynode("nrel_weight", NodeType.CONST_NO_ROLE);
                ScNode userGenresTuple = context.resolveKeynode("user_genres", NodeType.CONST_TUPLE);
                WaffleWavesPattern pattern = new WaffleWavesPattern();

                var genreList = context.find(pattern.userGenresPattern(userNode, userGenresTuple, nrelWeight)).toList();

                boolean finded = false;
                for (var genreNode : genreList) {

                    ScElement finalUserNode = userNode;
                    var scElements = genreNode.filter((element) -> element != null &&
                            (!element.equals(userGenresTuple) && !element.equals(finalUserNode) && !element.equals(nrelWeight))
                            && (element.getClass() == ScLinkStringImpl.class || element.getClass() == ScNodeImpl.class)
                    ).toList();

                    if (scElements.get(0).equals(genre)) {
                        int value = Integer.parseInt(context.getStringLinkContent((ScLinkString) scElements.get(1))
                                .replace("float:", "").replace("\"", ""));
                        context.setStringLinkContent((ScLinkString) scElements.get(1), String.valueOf(value + 1));
                        finded = true;
                        break;
                    }
                }

                if (!finded) {

                    ScLink weightNode = context.createStringLink(LinkType.LINK_CONST, Integer.toString(1));
                    ScEdge weightRelationEdge = context.resolveEdge(genre, EdgeType.D_COMMON_VAR, weightNode);

                    ScEdge genreNodeGenreConceptEdge = context.resolveEdge(userGenresTuple, EdgeType.ACCESS_VAR_POS_PERM, genre);

                    ScEdge weightRelationWeightRelationEdge = context.resolveEdge(nrelWeight, EdgeType.ACCESS_VAR_POS_PERM, weightRelationEdge);

                    context.memory().generate(pattern.linkToStructurePattern(userNode, List.of(weightNode, weightRelationEdge, genreNodeGenreConceptEdge, weightRelationWeightRelationEdge,
                            genre)));

                }

            } else {
                throw new NullPointerException();
            }

        } catch (ScMemoryException e) {
            throw new RuntimeException(e);
        }
    }
}
