package io.github.semanticpie.orchestrator.services;

import io.github.semanticpie.orchestrator.models.TrackData;

public interface TrackService {
    void uploadTrackToScMachine(TrackData trackDataDTO);
}
