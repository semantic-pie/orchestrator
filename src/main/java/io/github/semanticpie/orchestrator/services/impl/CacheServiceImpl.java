package io.github.semanticpie.orchestrator.services.impl;

import io.github.semanticpie.orchestrator.services.CacheService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ostis.api.context.DefaultScContext;
import org.ostis.scmemory.model.element.node.NodeType;
import org.ostis.scmemory.model.element.node.ScNode;
import org.ostis.scmemory.model.exception.ScMemoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    private final CacheManager cacheManager;
    private final DefaultScContext context;

    @Value("${application.cache.config}")
    private String configPath;

    @Autowired
    public CacheServiceImpl(CacheManager cacheManager, DefaultScContext context) {
        this.cacheManager = cacheManager;
        this.context = context;
    }

    @PostConstruct
    private void initCache() throws FileNotFoundException, ScMemoryException {
        Yaml yaml = new Yaml();
        LinkedHashMap<String, List<String>> config = yaml.load(new FileReader(ResourceUtils.getFile("classpath:" + configPath)));
        log.info("kek: {}", config);
        List<Optional<? extends ScNode>> nodes = context.memory().findKeynodes(config.get("include").stream()).toList();

        IntStream.range(0, config.get("include").size())
                .forEach(i ->
                    nodes.get(i).ifPresent((node) ->
                        Objects.requireNonNull(cacheManager.getCache("sc-elements"))
                                .put(config.get("include").get(i), node))
                );
    }

    @Override
    @Cacheable("sc-elements")
    public ScNode get(String idtf) {
        try {
            ScNode node = context.findKeynode(idtf).orElse(null);
            System.out.println(node);
            if (node != null) {
                return node;
            } else {
                log.warn("!!! [{}] - resolved node !!! (created by default)", idtf);
                return context.resolveKeynode(idtf, NodeType.NODE);
            }

        } catch (ScMemoryException e) {
            return null;
        }
    }
}
