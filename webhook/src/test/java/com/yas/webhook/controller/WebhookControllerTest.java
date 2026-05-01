package com.yas.webhook.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.service.WebhookService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController webhookController;

    private WebhookDetailVm webhookDetailVm;
    private WebhookPostVm webhookPostVm;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        webhookDetailVm = new WebhookDetailVm();
        webhookDetailVm.setId(1L);
        webhookDetailVm.setPayloadUrl("https://example.com/hook");

        webhookPostVm = new WebhookPostVm(
            "https://example.com/hook", "secret123", "application/json", true, null
        );
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/webhooks/paging                                     //
    // ------------------------------------------------------------------ //

    @Test
    void getPageableWebhooks_whenCalled_thenReturn200() throws Exception {
        WebhookListGetVm listVm = WebhookListGetVm.builder()
            .webhooks(List.of()).pageNo(0).pageSize(10)
            .totalPages(0).totalElements(0L).isLast(true).build();
        when(webhookService.getPageableWebhooks(anyInt(), anyInt())).thenReturn(listVm);

        mockMvc.perform(get("/backoffice/webhooks/paging")
                .param("pageNo", "0")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNo").value(0));
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/webhooks                                            //
    // ------------------------------------------------------------------ //

    @Test
    void listWebhooks_whenCalled_thenReturn200WithList() throws Exception {
        WebhookVm webhookVm = new WebhookVm();
        when(webhookService.findAllWebhooks()).thenReturn(List.of(webhookVm));

        mockMvc.perform(get("/backoffice/webhooks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/webhooks/{id}                                       //
    // ------------------------------------------------------------------ //

    @Test
    void getWebhook_whenExists_thenReturn200() throws Exception {
        when(webhookService.findById(1L)).thenReturn(webhookDetailVm);

        mockMvc.perform(get("/backoffice/webhooks/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.payloadUrl").value("https://example.com/hook"));
    }

    @Test
    void getWebhook_whenNotFound_thenReturn404() throws Exception {
        when(webhookService.findById(99L))
            .thenThrow(new NotFoundException("WEBHOOK_NOT_FOUND", 99L));

        mockMvc.perform(get("/backoffice/webhooks/99"))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // POST /backoffice/webhooks                                           //
    // ------------------------------------------------------------------ //

    @Test
    void createWebhook_whenValid_thenReturn201() throws Exception {
        when(webhookService.create(any(WebhookPostVm.class))).thenReturn(webhookDetailVm);

        mockMvc.perform(post("/backoffice/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPostVm)))
            .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ //
    // PUT /backoffice/webhooks/{id}                                       //
    // ------------------------------------------------------------------ //

    @Test
    void updateWebhook_whenValid_thenReturn204() throws Exception {
        doNothing().when(webhookService).update(any(WebhookPostVm.class), eq(1L));

        mockMvc.perform(put("/backoffice/webhooks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPostVm)))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateWebhook_whenNotFound_thenReturn404() throws Exception {
        doThrow(new NotFoundException("WEBHOOK_NOT_FOUND", 99L))
            .when(webhookService).update(any(WebhookPostVm.class), eq(99L));

        mockMvc.perform(put("/backoffice/webhooks/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPostVm)))
            .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ //
    // DELETE /backoffice/webhooks/{id}                                    //
    // ------------------------------------------------------------------ //

    @Test
    void deleteWebhook_whenExists_thenReturn204() throws Exception {
        doNothing().when(webhookService).delete(1L);

        mockMvc.perform(delete("/backoffice/webhooks/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteWebhook_whenNotFound_thenReturn404() throws Exception {
        doThrow(new NotFoundException("WEBHOOK_NOT_FOUND", 99L))
            .when(webhookService).delete(99L);

        mockMvc.perform(delete("/backoffice/webhooks/99"))
            .andExpect(status().isNotFound());
    }
}
