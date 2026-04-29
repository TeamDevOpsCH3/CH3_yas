package com.yas.location.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.location.model.Country;
import com.yas.location.model.StateOrProvince;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

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
}
