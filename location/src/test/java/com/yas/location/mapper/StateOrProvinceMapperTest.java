package com.yas.location.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.location.model.Country;
import com.yas.location.model.StateOrProvince;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateOrProvinceMapperTest {

    private final StateOrProvinceMapper mapper = Mappers.getMapper(StateOrProvinceMapper.class);

    @Test
    void toVm_mapsCountryId() {
        Country country = Country.builder().id(5L).name("Country").build();
        StateOrProvince state = StateOrProvince.builder()
            .id(10L)
            .name("State")
            .code("ST")
            .type("Type")
            .country(country)
            .build();

        StateOrProvinceVm vm = mapper.toStateOrProvinceViewModelFromStateOrProvince(state);

        assertThat(vm.id()).isEqualTo(10L);
        assertThat(vm.countryId()).isEqualTo(5L);
    }

    @Test
    void toVm_whenNull_returnsNull() {
        assertThat(mapper.toStateOrProvinceViewModelFromStateOrProvince(null)).isNull();
    }

    @Test
    void toVm_whenCountryNull_setsCountryIdNull() {
        StateOrProvince state = StateOrProvince.builder()
            .id(10L)
            .name("State")
            .code("ST")
            .type("Type")
            .country(null)
            .build();

        StateOrProvinceVm vm = mapper.toStateOrProvinceViewModelFromStateOrProvince(state);

        assertThat(vm.countryId()).isNull();
    }
}
