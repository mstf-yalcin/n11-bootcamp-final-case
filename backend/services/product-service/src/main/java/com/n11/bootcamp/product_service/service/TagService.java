package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateTagRequest;
import com.n11.bootcamp.product_service.dto.response.TagResponse;
import com.n11.bootcamp.product_service.entity.Tag;
import com.n11.bootcamp.product_service.mapper.TagMapper;
import com.n11.bootcamp.product_service.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    public List<TagResponse> getAllTags() {
        log.info("Listing all active tags");
        return tagRepository.findAllByIsActiveTrue()
                .stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        log.info("Creating tag: name={}", request.name());
        if (tagRepository.existsByNameIgnoreCase(request.name())) {
            log.warn("Tag already exists: name={}", request.name());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag already exists: " + request.name());
        }
        Tag tag = Tag.builder()
                .name(request.name())
                .slug(generateSlug(request.name()))
                .build();
        Tag saved = tagRepository.save(tag);
        log.info("Tag created: id={}, slug={}", saved.getId(), saved.getSlug());
        return tagMapper.toResponse(saved);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
