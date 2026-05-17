package com.n11.bootcamp.product_service.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Pre-creates the {@code products} index with {@link ProductSearchDoc} mapping at startup.
 * Without this, the Kafka Connect ES Sink creates the index with dynamic mapping (text+keyword
 * pairs for every string field), which breaks UUID term filters and inflates the index.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.product.search.engine", havingValue = "elastic")
public class ProductIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    public ProductIndexInitializer(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductSearchDoc.class);
        if (indexOps.exists()) {
            log.info("Elasticsearch index 'products' already exists; skipping create.");
            return;
        }
        indexOps.create();
        indexOps.putMapping();
        log.info("Created Elasticsearch index 'products' with explicit mapping from ProductSearchDoc.");
    }
}
