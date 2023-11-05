package io.github.semanticpie.orchestrator.orchestrator.agents.waffleWavesAgent;

import io.github.semanticpie.orchestrator.services.JmanticService;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;
import org.ostis.scmemory.model.element.ScElement;
import org.ostis.scmemory.model.element.edge.EdgeType;
import org.ostis.scmemory.model.element.link.LinkType;
import org.ostis.scmemory.model.element.link.ScLinkFloat;
import org.ostis.scmemory.model.element.link.ScLinkString;
import org.ostis.scmemory.model.element.node.NodeType;
import org.ostis.scmemory.model.element.node.ScNode;
import org.ostis.scmemory.model.exception.ScMemoryException;
import org.ostis.scmemory.model.pattern.ScPattern;
import org.ostis.scmemory.model.pattern.pattern3.ScPattern3;
import org.ostis.scmemory.model.pattern.pattern3.ScPattern3Impl;
import org.ostis.scmemory.websocketmemory.memory.element.ScEdgeImpl;
import org.ostis.scmemory.websocketmemory.memory.element.ScLinkFloatImpl;
import org.ostis.scmemory.websocketmemory.memory.element.ScLinkStringImpl;
import org.ostis.scmemory.websocketmemory.memory.element.ScNodeImpl;
import org.ostis.scmemory.websocketmemory.memory.pattern.DefaultWebsocketScPattern;
import org.ostis.scmemory.websocketmemory.memory.pattern.SearchingPatternTriple;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.AliasPatternElement;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.FixedPatternElement;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.TypePatternElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
public class WaffleWavesService {
    private final JmanticService service;

    private final DefaultScContext context;
    private final Map<ScElement, Integer> userGenresMap;

    @Autowired
    public WaffleWavesService(JmanticService service) {
        this.service = service;
        this.context = service.getContext();
        this.userGenresMap = new HashMap<>();
    }

    public void loadGenreWeights(ScElement userNode) throws ScMemoryException {

        ScNode nrelWeight = context.resolveKeynode("nrel_weight", NodeType.CONST_NO_ROLE);
        ScNode userGenresTuple = context.resolveKeynode("user_genres", NodeType.CONST_TUPLE);

        var genreList = context.find(userGenresPattern(userNode, userGenresTuple, nrelWeight)).toList();

        log.info("Genres count: " + genreList.size());

        for (var genre : genreList) {

            var scElements = genre.filter((element) -> element != null &&
                    (!element.equals(userGenresTuple) && !element.equals(userNode) && !element.equals(nrelWeight))
                    && (element.getClass() == ScLinkStringImpl.class || element.getClass() == ScNodeImpl.class)
            ).toList();

            int value = Integer.parseInt(context.getStringLinkContent((ScLinkString) scElements.get(1))
                    .replace("float:", "").replace("\"", ""));

            userGenresMap.put(scElements.get(0), value);
        }

        log.info(userGenresMap.toString());
    }


    private List<ScElement> getMusicByGenre(ScElement genre) throws ScMemoryException {
       var trackList =  context.find(new ScPattern3Impl<>(genre, EdgeType.ACCESS_VAR_POS_PERM, NodeType.VAR)).toList();
       log.warn(String.valueOf(trackList.size()));
       return null;
    }

    private ScPattern userGenresPattern(ScElement userNode, ScElement userGenresTuple, ScElement nrelWeight) {

        ScPattern pattern = new DefaultWebsocketScPattern();

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(userGenresTuple),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_1")),
                new TypePatternElement<>(NodeType.VAR_CLASS, new AliasPatternElement("genre"))));

        pattern.addElement(new SearchingPatternTriple(
                new AliasPatternElement("genre"),
                new TypePatternElement<>(EdgeType.D_COMMON_VAR, new AliasPatternElement("edge_2")),
                new TypePatternElement<>(LinkType.LINK_VAR, new AliasPatternElement("weight"))));

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(nrelWeight),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_3")),
                new AliasPatternElement("edge_2")));


        //Add to struct
        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_4")),
                new AliasPatternElement("genre")
        ));

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_6")),
                new AliasPatternElement("edge_1")
        ));

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_7")),
                new AliasPatternElement("edge_2")
        ));

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_8")),
                new AliasPatternElement("edge_3")
        ));

        return pattern;
    }

}

