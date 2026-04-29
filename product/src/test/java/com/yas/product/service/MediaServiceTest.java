package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    @Test
    void getMedia_withNullId_returnsDefault() {
        MediaService mediaService = new MediaService(restClient, serviceUrlConfig);

        NoFileMediaVm result = mediaService.getMedia(null);

        assertNull(result.id());
        assertEquals("", result.url());
    }

    @Test
    void getMedia_withId_returnsFromClient() {
        @SuppressWarnings("unchecked")
        RestClient.RequestHeadersUriSpec<?> uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("unchecked")
        RestClient.RequestHeadersSpec<?> headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(serviceUrlConfig.media()).thenReturn("http://media");
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        NoFileMediaVm expected = new NoFileMediaVm(10L, "cap", "file", "type", "http://media/10");
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expected);

        MediaService mediaService = new MediaService(restClient, serviceUrlConfig);

        NoFileMediaVm result = mediaService.getMedia(10L);

        assertEquals(expected, result);
    }
}
