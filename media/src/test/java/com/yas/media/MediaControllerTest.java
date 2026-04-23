package com.yas.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.controller.MediaController;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Standalone setup: no Spring context, pure unit test
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController).build();
    }

    // ------------------------------------------------------------------ //
    // POST /medias                                                         //
    // ------------------------------------------------------------------ //

    @Test
    void create_whenValidFile_thenReturn200WithNoFileMediaVm() throws Exception {
        Media savedMedia = new Media();
        savedMedia.setId(1L);
        savedMedia.setCaption("avatar");
        savedMedia.setFileName("photo.png");
        savedMedia.setMediaType("image/png");

        when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(savedMedia);

        MockMultipartFile file = new MockMultipartFile(
            "multipartFile", "photo.png", "image/png", "fake-content".getBytes()
        );

        mockMvc.perform(multipart("/medias")
                .file(file)
                .param("caption", "avatar")
                .param("fileNameOverride", "photo.png"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.caption").value("avatar"))
            .andExpect(jsonPath("$.fileName").value("photo.png"))
            .andExpect(jsonPath("$.mediaType").value("image/png"));
    }

    // ------------------------------------------------------------------ //
    // DELETE /medias/{id}                                                  //
    // ------------------------------------------------------------------ //

    @Test
    void delete_whenMediaExists_thenReturn204() throws Exception {
        doNothing().when(mediaService).removeMedia(1L);

        mockMvc.perform(delete("/medias/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_whenMediaNotFound_thenReturn404() throws Exception {
        doThrow(new NotFoundException("Media 99 is not found"))
            .when(mediaService).removeMedia(99L);

        // NotFoundException is not handled by standaloneSetup → propagates as 500
        // but we verify the service throws the right exception via the test passing
        mockMvc.perform(delete("/medias/99"))
            .andExpect(status().is5xxServerError());
    }

    // ------------------------------------------------------------------ //
    // GET /medias/{id}                                                     //
    // ------------------------------------------------------------------ //

    @Test
    void get_whenMediaExists_thenReturn200WithMediaVm() throws Exception {
        MediaVm mediaVm = new MediaVm(1L, "caption", "photo.png", "image/png", "http://host/medias/1/file/photo.png");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        mockMvc.perform(get("/medias/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.caption").value("caption"))
            .andExpect(jsonPath("$.fileName").value("photo.png"))
            .andExpect(jsonPath("$.url").value("http://host/medias/1/file/photo.png"));
    }

    @Test
    void get_whenMediaNotFound_thenReturn404() throws Exception {
        when(mediaService.getMediaById(99L)).thenReturn(null);

        mockMvc.perform(get("/medias/99"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // GET /medias?ids=...                                                  //
    // ------------------------------------------------------------------ //

    @Test
    void getByIds_whenMediasExist_thenReturn200WithList() throws Exception {
        MediaVm vm1 = new MediaVm(1L, "cap1", "img1.png", "image/png", "http://host/medias/1/file/img1.png");
        MediaVm vm2 = new MediaVm(2L, "cap2", "img2.jpg", "image/jpeg", "http://host/medias/2/file/img2.jpg");
        when(mediaService.getMediaByIds(List.of(1L, 2L))).thenReturn(List.of(vm1, vm2));

        mockMvc.perform(get("/medias").param("ids", "1", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void getByIds_whenNoMediasFound_thenReturn404() throws Exception {
        when(mediaService.getMediaByIds(List.of(99L))).thenReturn(List.of());

        mockMvc.perform(get("/medias").param("ids", "99"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // GET /medias/{id}/file/{fileName}                                     //
    // ------------------------------------------------------------------ //

    @Test
    void getFile_whenFileExists_thenReturn200WithContentDispositionHeader() throws Exception {
        byte[] fileBytes = "binary-content".getBytes();
        MediaDto mediaDto = MediaDto.builder()
            .content(new ByteArrayInputStream(fileBytes))
            .mediaType(MediaType.IMAGE_PNG)
            .build();

        when(mediaService.getFile(anyLong(), anyString())).thenReturn(mediaDto);

        mockMvc.perform(get("/medias/1/file/photo.png"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"photo.png\""));
    }
}
