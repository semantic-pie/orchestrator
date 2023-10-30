package io.github.semanticpie.orchestrator.orchestrator.agents;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import io.github.semanticpie.orchestrator.models.TrackData;
import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.exceptions.AgentException;
import io.github.semanticpie.orchestrator.services.impl.TrackServiceException;
import io.github.semanticpie.orchestrator.services.TrackService;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;
import org.ostis.scmemory.model.element.ScElement;
import org.ostis.scmemory.model.element.edge.EdgeType;
import org.ostis.scmemory.model.element.edge.ScEdge;
import org.ostis.scmemory.model.element.link.LinkType;
import org.ostis.scmemory.model.element.link.ScLinkString;
import org.ostis.scmemory.model.element.node.NodeType;
import org.ostis.scmemory.model.event.OnAddIngoingEdgeEvent;
import org.ostis.scmemory.model.exception.ScMemoryException;
import org.ostis.scmemory.model.pattern.pattern5.ScPattern5Impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;

@Slf4j
@Component
public class TrackAgent extends Agent {

    private final RestTemplate restTemplate;
    private final TrackService trackService;

    @Autowired
    public TrackAgent(DefaultScContext context, RestTemplate restTemplate, TrackService trackService) {
        this.context = context;
        this.restTemplate = restTemplate;
        this.trackService = trackService;
    }

    @Override
    public void subscribe() {
        try {
            log.info("AGENT {} SUBSCRIBED", TrackAgent.class);
            context.findKeynode("format_audio_mpeg").ifPresent(subscriber -> {
                var event = new OnAddIngoingEdgeEvent(){

                    @Override
                    public void onEvent(ScElement source, ScEdge edge, ScElement target) {
                        log.info("Event accepted [{}]", TrackAgent.class);
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
        try {
            var mainIdtf = context.resolveKeynode("nrel_system_identifier", NodeType.CONST);

            var trackPattern = context.find(new ScPattern5Impl<>(
                    target, EdgeType.D_COMMON_VAR, LinkType.LINK_VAR, EdgeType.ACCESS_VAR_POS_PERM, mainIdtf
                    )).findFirst().orElseThrow(ScMemoryException::new);

            String hash = context.getStringLinkContent((ScLinkString) trackPattern.get3());

            TrackData trackData = getTrackMetadataByHash(hash);
            trackService.uploadTrackToScMachine(trackData);
            log.info("uploaded [{}]", hash);
        } catch (ScMemoryException e) {
            throw new AgentException(e);
        }
    }

    public TrackData getTrackMetadataByHash(String hash) {
        try {
            File resource = File.createTempFile(hash, "tmp");
            restTemplate.execute("http://localhost:8080/api/v1/loafloader/" + hash, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(resource));
                return resource;
            });

            return getTrackData(resource, hash);
        } catch (IOException | InvalidDataException | UnsupportedTagException e) {
            throw new TrackServiceException("Can't get track metadata by UUID [" + hash + "]");
        }
    }

    private TrackData getTrackData(File resource, String hash) throws IOException, InvalidDataException, UnsupportedTagException {

        Mp3File mp3file = new Mp3File(resource);
        ID3v2 id3v2 = mp3file.getId3v2Tag();
        return TrackData.builder()
                .hash(hash)
                .title(id3v2.getTitle())
                .album(id3v2.getAlbum())
                .artist(id3v2.getArtist())
                .genre(id3v2.getGenreDescription())
                .releaseYear(id3v2.getYear())
                .trackNumber(id3v2.getTrack())
                .bitrate(mp3file.getBitrate())
                .lengthInMilliseconds(mp3file.getLengthInMilliseconds())
                .build();
    }
}
