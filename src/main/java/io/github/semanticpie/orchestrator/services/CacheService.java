package io.github.semanticpie.orchestrator.services;

import org.ostis.scmemory.model.element.node.ScNode;

public interface CacheService {
    ScNode get(String idtf);
}
