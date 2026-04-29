package com.n11.bootcamp.product_service.service;

import com.n11.bootcamp.product_service.dto.request.CreateTagRequest;
import com.n11.bootcamp.product_service.dto.response.TagResponse;
import com.n11.bootcamp.product_service.entity.Tag;
import com.n11.bootcamp.product_service.mapper.TagMapper;
import com.n11.bootcamp.product_service.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    private Tag tag;
    private TagResponse tagResponse;

    @BeforeEach
    void setUp() {
        tag = Tag.builder().name("Electronics").slug("electronics").build();
        tagResponse = new TagResponse(UUID.randomUUID(), "Electronics", "electronics");
    }

    @Test
    void testGetAllTags_when_tagsExist_returnsTagList() {
        when(tagRepository.findAllByIsActiveTrue()).thenReturn(List.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        List<TagResponse> result = tagService.getAllTags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
    }

    @Test
    void testCreateTag_when_nameIsUnique_returnsCreatedTag() {
        CreateTagRequest request = new CreateTagRequest("Electronics");

        when(tagRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag);
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        TagResponse result = tagService.createTag(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Electronics");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void testCreateTag_when_nameAlreadyExists_throwsConflict() {
        CreateTagRequest request = new CreateTagRequest("Electronics");

        when(tagRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
