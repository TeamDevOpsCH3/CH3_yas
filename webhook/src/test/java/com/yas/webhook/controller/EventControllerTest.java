package com.yas.webhook.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.webhook.model.enums.EventName;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.service.EventService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    // ------------------------------------------------------------------ //
    // GET /backoffice/events                                              //
    // ------------------------------------------------------------------ //

    @Test
    void listEvents_whenEventsExist_thenReturn200WithList() throws Exception {
        EventVm eventVm = EventVm.builder().id(1L).name(EventName.ON_ORDER_CREATED).build();
        when(eventService.findAllEvents()).thenReturn(List.of(eventVm));

        mockMvc.perform(get("/backoffice/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value(EventName.ON_ORDER_CREATED.name()));
    }

    @Test
    void listEvents_whenNoEvents_thenReturn200WithEmptyList() throws Exception {
        when(eventService.findAllEvents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/backoffice/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}
