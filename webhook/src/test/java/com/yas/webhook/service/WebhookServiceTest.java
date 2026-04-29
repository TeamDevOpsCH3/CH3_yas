package com.yas.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Event;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.enums.NotificationStatus;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.repository.EventRepository;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookEventRepository;
import com.yas.webhook.repository.WebhookRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private WebhookRepository webhookRepository;
    @Mock private EventRepository eventRepository;
    @Mock private WebhookEventRepository webhookEventRepository;
    @Mock private WebhookEventNotificationRepository webhookEventNotificationRepository;
    @Mock private WebhookMapper webhookMapper;
    @Mock private WebhookApi webHookApi;

    @InjectMocks
    private WebhookService webhookService;

    private Webhook webhook;
    private WebhookDetailVm webhookDetailVm;
    private WebhookPostVm webhookPostVm;

    @BeforeEach
    void setUp() {
        webhook = new Webhook();
        webhook.setId(1L);
        webhook.setPayloadUrl("https://example.com/hook");
        webhook.setSecret("secret123");
        webhook.setIsActive(true);
        webhook.setWebhookEvents(Collections.emptyList());

        webhookDetailVm = new WebhookDetailVm();
        webhookDetailVm.setId(1L);
        webhookDetailVm.setPayloadUrl("https://example.com/hook");

        webhookPostVm = new WebhookPostVm(
            "https://example.com/hook", "secret123", "application/json", true, null
        );
    }

    // ------------------------------------------------------------------ //
    // notifyToWebhook (existing coverage kept)                            //
    // ------------------------------------------------------------------ //

    @Test
    void notifyToWebhook_whenCalled_thenUpdateStatusToNotified() {
        WebhookEventNotificationDto notificationDto = WebhookEventNotificationDto.builder()
            .notificationId(1L).url("").secret("").build();
        WebhookEventNotification notification = new WebhookEventNotification();

        when(webhookEventNotificationRepository.findById(1L))
            .thenReturn(Optional.of(notification));

        webhookService.notifyToWebhook(notificationDto);

        assertThat(notification.getNotificationStatus()).isEqualTo(NotificationStatus.NOTIFIED);
        verify(webhookEventNotificationRepository).save(notification);
        verify(webHookApi).notify(notificationDto.getUrl(), notificationDto.getSecret(),
            notificationDto.getPayload());
    }

    // ------------------------------------------------------------------ //
    // getPageableWebhooks                                                  //
    // ------------------------------------------------------------------ //

    @Test
    void getPageableWebhooks_whenCalled_thenReturnPagedResult() {
        Page<Webhook> page = new PageImpl<>(List.of(webhook), PageRequest.of(0, 10), 1);
        WebhookListGetVm expectedVm = WebhookListGetVm.builder()
            .webhooks(List.of()).pageNo(0).pageSize(10)
            .totalPages(1).totalElements(1L).isLast(true).build();

        when(webhookRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(webhookMapper.toWebhookListGetVm(page, 0, 10)).thenReturn(expectedVm);

        WebhookListGetVm result = webhookService.getPageableWebhooks(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getPageNo()).isZero();
    }

    // ------------------------------------------------------------------ //
    // findAllWebhooks                                                      //
    // ------------------------------------------------------------------ //

    @Test
    void findAllWebhooks_whenCalled_thenReturnList() {
        WebhookVm webhookVm = new WebhookVm();
        when(webhookRepository.findAll(Sort.by(Sort.Direction.DESC, "id")))
            .thenReturn(List.of(webhook));
        when(webhookMapper.toWebhookVm(webhook)).thenReturn(webhookVm);

        List<WebhookVm> result = webhookService.findAllWebhooks();

        assertThat(result).hasSize(1);
    }

    // ------------------------------------------------------------------ //
    // findById                                                             //
    // ------------------------------------------------------------------ //

    @Test
    void findById_whenExists_thenReturnWebhookDetailVm() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

        WebhookDetailVm result = webhookService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotFound_thenThrowNotFoundException() {
        when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.findById(99L));
    }

    // ------------------------------------------------------------------ //
    // create                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void create_whenNoEvents_thenSaveWebhookOnly() {
        when(webhookMapper.toCreatedWebhook(webhookPostVm)).thenReturn(webhook);
        when(webhookRepository.save(webhook)).thenReturn(webhook);
        when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

        WebhookDetailVm result = webhookService.create(webhookPostVm);

        assertThat(result).isNotNull();
        verify(webhookEventRepository, never()).saveAll(anyList());
    }

    @Test
    void create_whenWithEvents_thenSaveWebhookAndEvents() {
        Event event = new Event();
        event.setId(10L);

        EventVm eventVm = EventVm.builder().id(10L).build();
        WebhookPostVm postVmWithEvents = new WebhookPostVm(
            "https://example.com/hook", "secret", "application/json", true, List.of(eventVm)
        );

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setWebhookId(1L);
        webhookEvent.setEventId(10L);

        when(webhookMapper.toCreatedWebhook(postVmWithEvents)).thenReturn(webhook);
        when(webhookRepository.save(webhook)).thenReturn(webhook);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(webhookEventRepository.saveAll(anyList())).thenReturn(List.of(webhookEvent));
        when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

        WebhookDetailVm result = webhookService.create(postVmWithEvents);

        assertThat(result).isNotNull();
        verify(webhookEventRepository).saveAll(anyList());
    }

    @Test
    void create_whenEventNotFound_thenThrowNotFoundException() {
        EventVm eventVm = EventVm.builder().id(99L).build();
        WebhookPostVm postVmWithBadEvent = new WebhookPostVm(
            "https://example.com/hook", "secret", "application/json", true, List.of(eventVm)
        );

        when(webhookMapper.toCreatedWebhook(postVmWithBadEvent)).thenReturn(webhook);
        when(webhookRepository.save(webhook)).thenReturn(webhook);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.create(postVmWithBadEvent));
    }

    // ------------------------------------------------------------------ //
    // update                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void update_whenWebhookNotFound_thenThrowNotFoundException() {
        when(webhookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.update(webhookPostVm, 99L));
    }

    @Test
    void update_whenNoEvents_thenUpdateWithoutSavingEvents() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toUpdatedWebhook(webhook, webhookPostVm)).thenReturn(webhook);
        when(webhookRepository.save(webhook)).thenReturn(webhook);

        webhookService.update(webhookPostVm, 1L);

        verify(webhookEventRepository).deleteAll(anyList());
        verify(webhookEventRepository, never()).saveAll(anyList());
    }

    @Test
    void update_whenWithEvents_thenUpdateAndSaveEvents() {
        Event event = new Event();
        event.setId(10L);

        EventVm eventVm = EventVm.builder().id(10L).build();
        WebhookPostVm postVmWithEvents = new WebhookPostVm(
            "https://example.com/hook", "secret", "application/json", true, List.of(eventVm)
        );

        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toUpdatedWebhook(webhook, postVmWithEvents)).thenReturn(webhook);
        when(webhookRepository.save(webhook)).thenReturn(webhook);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        webhookService.update(postVmWithEvents, 1L);

        verify(webhookEventRepository).saveAll(anyList());
    }

    // ------------------------------------------------------------------ //
    // delete                                                               //
    // ------------------------------------------------------------------ //

    @Test
    void delete_whenNotFound_thenThrowNotFoundException() {
        when(webhookRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> webhookService.delete(99L));
        verify(webhookRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_whenExists_thenDeleteEventsAndWebhook() {
        when(webhookRepository.existsById(1L)).thenReturn(true);

        webhookService.delete(1L);

        verify(webhookEventRepository).deleteByWebhookId(1L);
        verify(webhookRepository).deleteById(1L);
    }
}
