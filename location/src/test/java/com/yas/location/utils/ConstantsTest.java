package com.yas.location.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConstantsTest {

    @Test
    void errorCode_constants_matchValues() {
        assertThat(Constants.ErrorCode.COUNTRY_NOT_FOUND).isEqualTo("COUNTRY_NOT_FOUND");
        assertThat(Constants.ErrorCode.CODE_ALREADY_EXISTED).isEqualTo("CODE_ALREADY_EXISTED");
    }

    @Test
    void pageable_constants_matchDefaults() {
        assertThat(Constants.PageableConstant.DEFAULT_PAGE_NUMBER).isEqualTo("0");
        assertThat(Constants.PageableConstant.DEFAULT_PAGE_SIZE).isEqualTo("10");
    }

    @Test
    void api_constants_matchUrls() {
        assertThat(Constants.ApiConstant.COUNTRIES_URL).isEqualTo("/backoffice/countries");
        assertThat(Constants.ApiConstant.COUNTRIES_STOREFRONT_URL).isEqualTo("/storefront/countries");
    }
}
