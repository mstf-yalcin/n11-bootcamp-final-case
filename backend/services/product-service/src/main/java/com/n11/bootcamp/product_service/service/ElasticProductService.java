package com.n11.bootcamp.product_service.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductMinimalResponse;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.dto.response.SearchSuggestionResponse;
import com.n11.bootcamp.product_service.entity.Product;
import com.n11.bootcamp.product_service.search.ProductSearchDoc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Elasticsearch-backed {@link ProductService}. Active when
 * {@code app.product.search.engine=elastic}; otherwise {@link JpaProductService} alone is wired.
 *
 * <p>Decorator pattern (composition):</p>
 * <ul>
 *   <li>{@link #getProducts} and {@link #getAdminProducts} → Elasticsearch bool query for filtering,
 *       then PostgreSQL bulk fetch for enrichment (CQRS read hybrid — no stale field risk).</li>
 *   <li>All other operations → delegated to {@link JpaProductService}. Writes always hit
 *       PostgreSQL; Elasticsearch is fed by Debezium CDC + Kafka Connect ES Sink, not by this app.</li>
 * </ul>
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "app.product.search.engine", havingValue = "elastic")
public class ElasticProductService implements ProductService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final JpaProductService delegate;
    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticProductService(JpaProductService delegate,
                                 ElasticsearchOperations elasticsearchOperations) {
        this.delegate = delegate;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public Page<ProductResponse> getProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                             BigDecimal minRating, String search, Pageable pageable) {
        log.info("Elastic listing products: categoryId={}, minPrice={}, maxPrice={}, minRating={}, search={}, page={}",
                categoryId, minPrice, maxPrice, minRating, search, pageable.getPageNumber());
        return searchAndEnrich(categoryId, minPrice, maxPrice, minRating, search, false, pageable);
    }

    @Override
    public Page<ProductResponse> getAdminProducts(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                                  BigDecimal minRating, String search, boolean includeInactive,
                                                  Pageable pageable) {
        log.info("Elastic admin listing products: categoryId={}, minPrice={}, maxPrice={}, minRating={}, search={}, includeInactive={}, page={}",
                categoryId, minPrice, maxPrice, minRating, search, includeInactive, pageable.getPageNumber());
        return searchAndEnrich(categoryId, minPrice, maxPrice, minRating, search, includeInactive, pageable);
    }

    private Page<ProductResponse> searchAndEnrich(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                                  BigDecimal minRating, String search, boolean includeInactive,
                                                  Pageable pageable) {
        Query esQuery = buildBoolQuery(categoryId, minPrice, maxPrice, minRating, search, includeInactive);

        Pageable paginationOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(esQuery)
                .withPageable(paginationOnly)
                .withTrackTotalHits(true);

        for (Sort.Order order : pageable.getSort()) {
            SortOrder so = order.isAscending() ? SortOrder.Asc : SortOrder.Desc;
            String esField = "name".equals(order.getProperty()) ? "name.keyword" : order.getProperty();
            builder.withSort(s -> s.field(f -> f.field(esField).order(so)));
        }

        boolean hasSearch = search != null && !search.isBlank();
        if (hasSearch) {
            builder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        SearchHits<ProductSearchDoc> hits = elasticsearchOperations.search(
                builder.build(), ProductSearchDoc.class);

        List<UUID> ids = hits.getSearchHits().stream()
                .map(h -> UUID.fromString(h.getId()))
                .toList();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, hits.getTotalHits());
        }

        Map<UUID, Product> byId = delegate.getProductRepository().findByIdInWithTags(ids, includeInactive).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ProductResponse> ordered = ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(delegate.getProductMapper()::toResponse)
                .toList();

        Page<ProductResponse> page = new PageImpl<>(ordered, pageable, hits.getTotalHits());
        return delegate.enrichPageWithStock(page);
    }

    private Query buildBoolQuery(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                 BigDecimal minRating, String search, boolean includeInactive) {
        return Query.of(q -> q.bool(b -> {
            if (search != null && !search.isBlank()) {
                String trimmed = search.trim();
                if (UUID_PATTERN.matcher(trimmed).matches()) {
                    b.must(m -> m.ids(i -> i.values(trimmed.toLowerCase())));
                } else {
                    b.must(m -> m.bool(inner -> inner
                            .should(s -> s.term(t -> t
                                    .field("name.keyword")
                                    .value(trimmed)
                                    .boost(100.0f)))
                            .should(s -> s.matchPhrase(mp -> mp
                                    .field("name")
                                    .query(search)
                                    .boost(50.0f)))
                            .should(s -> s.multiMatch(mm -> mm
                                    .fields("name")
                                    .query(search)
                                    .fuzziness("AUTO")
                                    .prefixLength(2)
                                    .maxExpansions(10)
                                    .operator(Operator.And)
                                    .boost(10.0f)))
                            .should(s -> s.multiMatch(mm -> mm
                                    .query(search)
                                    .type(TextQueryType.BoolPrefix)
                                    .fields("name.autocomplete",
                                            "name.autocomplete._2gram",
                                            "name.autocomplete._3gram")))
                            .should(s -> s.match(mt -> mt
                                    .field("description")
                                    .query(search)
                                    .boost(0.5f)))
                            .minimumShouldMatch("1")
                    ));
                }
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }
            if (!includeInactive) {
                b.filter(f -> f.term(t -> t.field("isActive").value(true)));
            }
            if (categoryId != null) {
                b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId.toString())));
            }
            if (minPrice != null || maxPrice != null) {
                b.filter(f -> f.range(r -> r.number(n -> {
                    n.field("price");
                    if (minPrice != null) {
                        n.gte(minPrice.doubleValue());
                    }
                    if (maxPrice != null) {
                        n.lte(maxPrice.doubleValue());
                    }
                    return n;
                })));
            }
            if (minRating != null) {
                b.filter(f -> f.range(r -> r.number(n -> n.field("ratingAverage").gte(minRating.doubleValue()))));
            }
            return b;
        }));
    }

    // Delegate-only — writes and tek-item reads always hit PostgreSQL.

    @Override
    public ProductResponse getProductById(UUID id) {
        return delegate.getProductById(id);
    }

    @Override
    public ProductResponse getProductBySlug(String slug) {
        return delegate.getProductBySlug(slug);
    }

    @Override
    public List<ProductMinimalResponse> getProductsByIds(List<UUID> ids) {
        return delegate.getProductsByIds(ids);
    }

    @Override
    public List<UUID> getExistingProductIds(List<UUID> ids) {
        return delegate.getExistingProductIds(ids);
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        return delegate.createProduct(request);
    }

    @Override
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        return delegate.updateProduct(id, request);
    }

    @Override
    public void deleteProduct(UUID id) {
        delegate.deleteProduct(id);
    }

    @Override
    public ProductResponse restoreProduct(UUID id) {
        return delegate.restoreProduct(id);
    }

    @Override
    public SearchSuggestionResponse suggest(String query) {
        if (query == null || query.isBlank()) {
            return new SearchSuggestionResponse(query, null);
        }

        Suggester suggester = Suggester.of(s -> s
                .suggesters("text-suggest", FieldSuggester.of(fs -> fs
                        .text(query)
                        .term(t -> t.field("name").size(1))))
        );

        NativeQuery nq = NativeQuery.builder()
                .withQuery(Query.of(qb -> qb.matchAll(ma -> ma)))
                .withMaxResults(0)
                .withSuggester(suggester)
                .build();

        SearchHits<ProductSearchDoc> hits = elasticsearchOperations.search(nq, ProductSearchDoc.class);
        Suggest suggest = hits.getSuggest();
        if (suggest == null) {
            return new SearchSuggestionResponse(query, null);
        }

        StringBuilder reconstructed = new StringBuilder();
        boolean changed = false;
        for (Suggest.Suggestion<?> suggestion : suggest.getSuggestions()) {
            for (Suggest.Suggestion.Entry<?> entry : suggestion.getEntries()) {
                String original = entry.getText();
                String corrected = original;
                if (!entry.getOptions().isEmpty()) {
                    corrected = entry.getOptions().get(0).getText();
                    if (!corrected.equalsIgnoreCase(original)) {
                        changed = true;
                    }
                }
                if (!reconstructed.isEmpty()) {
                    reconstructed.append(' ');
                }
                reconstructed.append(corrected);
            }
        }

        return changed
                ? new SearchSuggestionResponse(query, reconstructed.toString())
                : new SearchSuggestionResponse(query, null);
    }
}
