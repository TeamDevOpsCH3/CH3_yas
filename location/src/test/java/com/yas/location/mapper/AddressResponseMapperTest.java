package com.yas.location.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressResponseMapperTest {

    @Test
    void projection_interface_allowsValueAccess() {
        AddressResponseMapper mapper = new AddressResponseMapper() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getContactName() {
                return "contact";
            }

            @Override
            public String getPhone() {
                return "123";
            }

            @Override
            public String getAddressLine1() {
                return "line1";
            }
        };

        assertEquals(1L, mapper.getId());
        assertEquals("contact", mapper.getContactName());
        assertEquals("123", mapper.getPhone());
        assertEquals("line1", mapper.getAddressLine1());
    }
}
