package io.github.semanticpie.orchestrator.orchestrator.agents.waffleWavesAgent;

import io.github.semanticpie.orchestrator.services.JmanticService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;
import org.ostis.scmemory.model.element.ScElement;
import org.ostis.scmemory.model.element.edge.EdgeType;
import org.ostis.scmemory.model.element.link.LinkType;
import org.ostis.scmemory.model.element.link.ScLinkString;
import org.ostis.scmemory.model.element.node.NodeType;
import org.ostis.scmemory.model.element.node.ScNode;
import org.ostis.scmemory.model.exception.ScMemoryException;
import org.ostis.scmemory.model.pattern.ScPattern;
import org.ostis.scmemory.websocketmemory.memory.element.ScEdgeImpl;
import org.ostis.scmemory.websocketmemory.memory.element.ScLinkStringImpl;
import org.ostis.scmemory.websocketmemory.memory.element.ScNodeImpl;
import org.ostis.scmemory.websocketmemory.memory.pattern.DefaultWebsocketScPattern;
import org.ostis.scmemory.websocketmemory.memory.pattern.GeneratingPatternTriple;
import org.ostis.scmemory.websocketmemory.memory.pattern.SearchingPatternTriple;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.AliasPatternElement;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.FixedPatternElement;
import org.ostis.scmemory.websocketmemory.memory.pattern.element.TypePatternElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class WaffleWavesService {
    private final JmanticService service;

    private int listEdgeIndex = 0;
    private int structEdgeIndex = 0;
    public ScElement next;
    public ScElement start;
    public ScElement end;
    private final DefaultScContext context;
    @Getter
    private final Map<ScElement, Integer> userGenresMap;

    @Autowired
    public WaffleWavesService(JmanticService service) {
        this.service = service;
        this.context = service.getContext();
        this.userGenresMap = new HashMap<>();
        try {
            this.next = context.resolveKeynode("nrel_next", NodeType.CONST_NO_ROLE);
            this.start = context.resolveKeynode("concept_start", NodeType.CONST_CLASS);
            this.end = context.resolveKeynode("concept_end", NodeType.CONST_CLASS);
        } catch (ScMemoryException e) {
            throw new RuntimeException(e);
        }
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
    }

    public List<ScElement> getAndDeleteOldPlaylist(ScElement playlistNode, ScElement userNode) throws ScMemoryException {
        List<ScElement> tracks = new ArrayList<>();
        List<ScElement> edges = new ArrayList<>();

        context.find(findPlaylistTrackNodestPattern(userNode, playlistNode)).toList().forEach(stream -> {
            List<ScElement> elements = stream.filter(Objects::nonNull).collect(Collectors.toList());
            tracks.addAll(elements.stream().filter(scElement -> scElement instanceof ScNodeImpl).filter(ScElement -> !ScElement.equals(userNode) && !ScElement.equals(playlistNode)).toList());
            edges.addAll(elements.stream().filter(scElement -> scElement instanceof ScEdgeImpl).toList());
        });

        ScPattern pattern = new DefaultWebsocketScPattern();

        if(!tracks.isEmpty()) {
            for (int i = 0; i < tracks.size() - 1; i++) {
                pattern.addElement(new SearchingPatternTriple(new FixedPatternElement(tracks.get(i)),
                        new TypePatternElement<>(EdgeType.D_COMMON_VAR, new AliasPatternElement("edge_" + i)),
                        new FixedPatternElement(tracks.get(i + 1))));

            }
            edges.addAll(context.find(pattern).toList().get(0).filter(scElement -> scElement.getClass() == ScEdgeImpl.class).toList());

            edges.addAll(getEdgesOfMetaRelation(userNode, tracks, end));

            edges.addAll(getEdgesOfMetaRelation(userNode, tracks, start));

            context.deleteElements(edges.stream());
        }
        return tracks;
    }

    private List<ScElement> getEdgesOfMetaRelation(ScElement userNode, List<ScElement> tracks, ScElement metaRelation) throws ScMemoryException {

        List<ScElement> edges = new ArrayList<>();
        for (var relations : context.find(scPattern5ForPlaylist(userNode, metaRelation)).toList()) {
            List<ScElement> temp = relations.collect(Collectors.toList());
            if (temp.stream().anyMatch(tracks::contains)) {
                edges.addAll(temp.stream().filter(scElement -> scElement instanceof ScEdgeImpl).toList());
            }
        }
        return edges;
    }


    private ScPattern findPlaylistTrackNodestPattern(ScElement userNode, ScElement playlistNode) {
        ScPattern pattern = new DefaultWebsocketScPattern();

        pattern.addElement(new SearchingPatternTriple(new FixedPatternElement(playlistNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("_edge2")),
                new TypePatternElement<>(NodeType.VAR, new AliasPatternElement("_track"))));

        pattern.addElement(new SearchingPatternTriple(new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("_edge3")),
                new AliasPatternElement("_track")));

        pattern.addElement(new SearchingPatternTriple(new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("_edge4")),
                new AliasPatternElement("_edge2")));

        return pattern;
    }

    private ScPattern scPattern5ForPlaylist(ScElement userNode, ScElement relation) {
        ScPattern pattern = new DefaultWebsocketScPattern();
        pattern.addElement(new SearchingPatternTriple(new FixedPatternElement(relation),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_1")),
                new TypePatternElement<>(NodeType.VAR, new AliasPatternElement("track"))));

        pattern.addElement(new SearchingPatternTriple(new FixedPatternElement(userNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_2")),
                new AliasPatternElement("edge_1")));

        return pattern;
    }

    public List<ScElement> createPlaylist(int size, List<ScElement> oldPlaylist) {
        Random random = new Random();
        int sum = userGenresMap.values().stream().mapToInt(Integer::intValue).sum();

        log.info("sum: {}", sum);

        userGenresMap.replaceAll((key, value) -> Math.round(((float) value / sum) * size));
        userGenresMap.forEach((key, value) -> log.info("limit: {}", value));
        List<ScElement> playlist = new ArrayList<>(Collections.nCopies(size, null));
        userGenresMap.forEach((key, value) -> {
            try {
                getTracksByGenre(key, value, oldPlaylist).forEach((track) -> {
                    while (true) {
                        int index = random.nextInt(size);
                        if (playlist.get(index) == null) {
                            playlist.set(index, track);
                            break;
                        }
                    }
                });

            } catch (ScMemoryException e) {
                throw new RuntimeException(e);
            }
        });
        playlist.removeAll(Collections.singleton(null));
        log.info("Playlist size: {}", playlist.size());
        log.info("Playlist: {}", playlist);

        return playlist;
    }

    public void uploadPlaylist(ScElement playlistNode, List<ScElement> playlist, ScElement userNode) throws ScMemoryException {
        var tuple = context.memory().generate(linkToStructurePattern(playlistNode, playlist)).filter(scElement -> !scElement.equals(playlistNode))
                .filter(scElement -> playlist.stream().noneMatch(track -> track.equals(scElement))).toList();

        List<ScElement> list = new ArrayList<>(uploadList(playlist).filter(scElement -> playlist.stream().noneMatch(track -> track.equals(scElement))).toList());
        list.addAll(playlist);
        list.addAll(tuple);
        log.info("Playlist size: {}", playlist.size());
        log.info("Uploaded list size: {}", list.size());
        context.memory().generate(linkToStructurePattern(userNode, list));
    }

    private Stream<? extends ScElement> uploadList(List<ScElement> list) throws ScMemoryException {
        return context.memory().generate(listPattern(list)).filter(Objects::nonNull).filter(scElement -> !scElement.equals(end) && !scElement.equals(start) && !scElement.equals(next));
    }

    private ScPattern linkToStructurePattern(ScElement structure, List<? extends ScElement> elements) {
        ScPattern pattern = new DefaultWebsocketScPattern();

        for (var element : elements) {
            pattern.addElement(new GeneratingPatternTriple(new FixedPatternElement(structure),
                    new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_" + structEdgeIndex)),
                    new FixedPatternElement(element)));
            structEdgeIndex++;
        }
        return pattern;
    }

    private ScPattern listPattern(List<ScElement> list) throws ScMemoryException {
        ScPattern pattern = new DefaultWebsocketScPattern();

        for (int i = 0; i < list.size() - 1; i++) {
            addListSection(pattern, list.get(i), list.get(i + 1), next);
        }

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(start),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_" + listEdgeIndex)),
                new FixedPatternElement(list.get(0))));
        listEdgeIndex++;
        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(end),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM,
                        new AliasPatternElement("edge_" + listEdgeIndex)),
                new FixedPatternElement(list.get(list.size() - 1))));
        listEdgeIndex++;
        return pattern;
    }

    private void addListSection(ScPattern pattern, ScElement source, ScElement target, ScElement relation) {
        int edge1 = listEdgeIndex;
        pattern.addElement(
                new GeneratingPatternTriple(
                        new FixedPatternElement(source),
                        new TypePatternElement<>(EdgeType.D_COMMON_VAR, new AliasPatternElement("edge_" + listEdgeIndex)),
                        new FixedPatternElement(target)));
        listEdgeIndex++;
        pattern.addElement(
                new GeneratingPatternTriple(
                        new FixedPatternElement(relation),
                        new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_" + listEdgeIndex)),
                        new AliasPatternElement("edge_" + edge1)));
        listEdgeIndex++;
    }

    private List<ScElement> getTracksByGenre(ScElement genreNode, int limit, List<ScElement> oldPlaylist) throws ScMemoryException {
        ScNode conceptTrack = context.resolveKeynode("concept_track", NodeType.CONST_CLASS);
        List<ScElement> output = new ArrayList<>();
        var trackList = context.find(trackPattern(genreNode, conceptTrack)).toList();
        int index = 0;
        for (var track : trackList) {
            if(index > limit) break;

            ScElement element = track.filter(Objects::nonNull).filter(scElement -> !scElement.equals(genreNode) && !scElement.equals(conceptTrack))
                    .filter(scElement -> scElement.getClass() != ScEdgeImpl.class).findFirst().orElseThrow();
            if(!oldPlaylist.contains(element)){
                index++;
                output.add(element);
            }
        }
        return output;
    }


    private ScPattern trackPattern(ScElement genreNode, ScElement conceptTrack) {
        ScPattern pattern = new DefaultWebsocketScPattern();

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(genreNode),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_1")),
                new TypePatternElement<>(NodeType.VAR, new AliasPatternElement("track"))
        ));

        pattern.addElement(new SearchingPatternTriple(
                new FixedPatternElement(conceptTrack),
                new TypePatternElement<>(EdgeType.ACCESS_VAR_POS_PERM, new AliasPatternElement("edge_2")),
                new AliasPatternElement("track")
        ));
        return pattern;
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

