package io.github.semanticpie.orchestrator.orchestrator.agents;

import com.mpatric.mp3agic.*;
import io.github.semanticpie.orchestrator.models.TrackData;
import io.github.semanticpie.orchestrator.orchestrator.Agent;
import io.github.semanticpie.orchestrator.orchestrator.exceptions.AgentException;
import io.github.semanticpie.orchestrator.services.impl.TrackServiceException;
import io.github.semanticpie.orchestrator.services.TrackService;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class TrackAgent extends Agent {

    private final RestTemplate restTemplate;
    private final TrackService trackService;

    @Value("${application.loaf-loader-url}")
    private String loafLoaderUrl;

    @Autowired
    public TrackAgent(DefaultScContext context, RestTemplate restTemplate, TrackService trackService) {
        this.context = context;
        this.restTemplate = restTemplate;
        this.trackService = trackService;
    }

    @Override
    public void subscribe() {
        this.subscribe("format_audio_mpeg", new OnAddIngoingEdgeEvent(){
            @Override
            public void onEvent(ScElement source, ScEdge edge, ScElement target) {
                onEventDo(source, edge, target);
            }
        });
        this.subscribe("format_audio_flac", new OnAddIngoingEdgeEvent(){
            @Override
            public void onEvent(ScElement source, ScEdge edge, ScElement target) {
                onEventDo(source, edge, target);
            }
        });
    }

    private void onEventDo(ScElement source, ScEdge edge, ScElement target) {
        try {
            log.info("EVENT EVENT EVENT");
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
            File resource = loadFileFromLoafLoader(hash);
            try {
                resource = changeExtension(resource, ".mp3");
                return getTrackData(resource, hash);
            } catch (IOException | InvalidDataException | RuntimeException e) {
                resource = changeExtension(resource, ".flac");
                var data = getVerboseCommentsTrackData(resource, hash);
                return data;
            } catch (UnsupportedTagException e) {
                throw new TrackServiceException("Can't get track metadata by UUID [" + hash + "]");
            }
        } catch (IOException | RuntimeException e) {
            log.info("err: {}", e);
            throw new RuntimeException(e);
        }

    }

    private File loadFileFromLoafLoader(String hash) throws IOException {
        File resource = File.createTempFile(hash, ".tmp");
        restTemplate.execute(loafLoaderUrl + hash, HttpMethod.GET, null, clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(resource));
            return resource;
        });
        return resource;
    }

    private TrackData getVerboseCommentsTrackData(File resource, String hash) {

        AudioFile audioFile = null;
        try {
            audioFile = AudioFileIO.read(resource);
            Tag tag = audioFile.getTag();

            AudioHeader header = audioFile.getAudioHeader();

            String artist = tag.getFirst(FieldKey.ARTIST);
            String album = tag.getFirst(FieldKey.ALBUM);
            String title = tag.getFirst(FieldKey.TITLE);
            String genre = tag.getFirst(FieldKey.GENRE);
            String year = tag.getFirst(FieldKey.YEAR);
            String tarckNr = tag.getFirst(FieldKey.TRACK);
            Integer bitRate = getBitRate(header.getBitRate());
            Long length = (long) header.getTrackLength();
            return TrackData.builder()
                    .hash(hash)
                    .title(title)
                    .album(album)
                    .artist(artist)
                    .genre(genre)
                    .releaseYear(year)
                    .trackNumber(tarckNr)
                    .bitrate(bitRate)
                    .lengthInMilliseconds(length)
                    .build();
        } catch (CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getBitRate(String bitRate) {
        try {
            return Integer.parseInt(bitRate);
        } catch (RuntimeException ignored) {
            return null;
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
    public static File changeExtension(File f, String newExtension) {
        var path = Path.of(f.getPath());
        try {
            return  Files.move(path, path.resolveSibling(path.getFileName() + newExtension)).toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
