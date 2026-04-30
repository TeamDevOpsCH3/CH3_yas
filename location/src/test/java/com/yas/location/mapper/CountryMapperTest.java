package com.yas.location.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.location.model.Country;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CountryMapperTest {

    private final CountryMapper mapper = Mappers.getMapper(CountryMapper.class);

    @Test
    void toCountryFromPostVm_mapsFields() {
        CountryPostVm postVm = CountryPostVm.builder()
            .id("id")
            .code2("US")
            .name("United States")
            .code3("USA")
            .isBillingEnabled(true)
            .isShippingEnabled(false)
            .isCityEnabled(true)
            .isZipCodeEnabled(false)
            .isDistrictEnabled(true)
            .build();

        Country country = mapper.toCountryFromCountryPostViewModel(postVm);

        assertThat(country.getName()).isEqualTo("United States");
        assertThat(country.getCode2()).isEqualTo("US");
        assertThat(country.getCode3()).isEqualTo("USA");
        assertThat(country.getIsBillingEnabled()).isTrue();
        assertThat(country.getIsShippingEnabled()).isFalse();
    }

    @Test
    void toCountryFromPostVm_whenNull_returnsNull() {
        assertThat(mapper.toCountryFromCountryPostViewModel(null)).isNull();
    }

    @Test
    void updateCountry_ignoresNullValues() {
        Country existing = Country.builder()
            .id(1L)
            .name("Old Name")
            .code2("OLD")
            .isCityEnabled(false)
            .build();

        CountryPostVm postVm = CountryPostVm.builder()
            .id("id")
            .name(null)
            .code2(null)
            .isCityEnabled(true)
            .build();

        mapper.toCountryFromCountryPostViewModel(existing, postVm);

        assertThat(existing.getName()).isEqualTo("Old Name");
        assertThat(existing.getCode2()).isEqualTo("OLD");
        assertThat(existing.getIsCityEnabled()).isTrue();
    }

    @Test
    void updateCountry_overwritesWhenValuesPresent() {
        Country existing = Country.builder()
            .id(1L)
            .name("Old Name")
            .code2("OLD")
            .build();

        CountryPostVm postVm = CountryPostVm.builder()
            .id("id")
            .name("New Name")
            .code2("NEW")
            .build();

        mapper.toCountryFromCountryPostViewModel(existing, postVm);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getCode2()).isEqualTo("NEW");
    }

    @Test
    void updateCountry_whenDtoNull_keepsValues() {
        Country existing = Country.builder()
            .id(1L)
            .name("Old Name")
            .code2("OLD")
            .build();

        mapper.toCountryFromCountryPostViewModel(existing, null);

        assertThat(existing.getName()).isEqualTo("Old Name");
        assertThat(existing.getCode2()).isEqualTo("OLD");
    }

    @ParameterizedTest
    @MethodSource("nullableFlags")
    void toCountryFromPostVm_mapsNullableFlags(Boolean billing, Boolean shipping,
                                               Boolean city, Boolean zip, Boolean district) {
        CountryPostVm postVm = CountryPostVm.builder()
            .id("id")
            .code2("US")
            .name("United States")
            .isBillingEnabled(billing)
            .isShippingEnabled(shipping)
            .isCityEnabled(city)
            .isZipCodeEnabled(zip)
            .isDistrictEnabled(district)
            .build();

        Country country = mapper.toCountryFromCountryPostViewModel(postVm);

        assertThat(country.getIsBillingEnabled()).isEqualTo(billing);
        assertThat(country.getIsShippingEnabled()).isEqualTo(shipping);
        assertThat(country.getIsCityEnabled()).isEqualTo(city);
        assertThat(country.getIsZipCodeEnabled()).isEqualTo(zip);
        assertThat(country.getIsDistrictEnabled()).isEqualTo(district);
    }

    private static Stream<Arguments> nullableFlags() {
        return Stream.of(
            Arguments.of(null, null, null, null, null),
            Arguments.of(true, false, null, true, false)
        );
    }

    @Test
    void toCountryVm_mapsFields() {
        Country country = Country.builder()
            .id(2L)
            .name("Canada")
            .code2("CA")
            .code3("CAN")
            .isBillingEnabled(true)
            .isShippingEnabled(true)
            .isCityEnabled(false)
            .isZipCodeEnabled(true)
            .isDistrictEnabled(false)
            .build();

        CountryVm vm = mapper.toCountryViewModelFromCountry(country);

        assertThat(vm.id()).isEqualTo(2L);
        assertThat(vm.name()).isEqualTo("Canada");
        assertThat(vm.isZipCodeEnabled()).isTrue();
    }

    @Test
    void toCountryVm_whenNull_returnsNull() {
        assertThat(mapper.toCountryViewModelFromCountry(null)).isNull();
    }
}
