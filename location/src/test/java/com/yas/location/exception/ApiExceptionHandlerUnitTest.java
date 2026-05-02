package com.yas.location.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.commonlibrary.exception.ApiExceptionHandler;
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.viewmodel.error.ErrorVm;
import com.yas.location.viewmodel.country.CountryPostVm;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class ApiExceptionHandlerUnitTest {

    private final TestableApiExceptionHandler handler = new TestableApiExceptionHandler();

    @Test
    void handleNotFoundException_returnsNotFound() {
        NotFoundException ex = new NotFoundException("NOT_FOUND");

        ResponseEntity<ErrorVm> response = handler.handleNotFoundException(ex,
            new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    @Test
    void handleBadRequestException_returnsBadRequest() {
        BadRequestException ex = new BadRequestException("BAD_REQUEST");

        ResponseEntity<ErrorVm> response = handler.handleBadRequestException(ex,
            new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    @Test
    void handleOtherException_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("boom");

        ResponseEntity<ErrorVm> response = handler.handleOtherExceptionPublic(ex,
            new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().detail()).isEqualTo("boom");
    }

    @Test
    void handleMethodArgumentNotValid_returnsFieldErrors() throws Exception {
        Method method = Dummy.class.getDeclaredMethod("handle", CountryPostVm.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "countryPostVm");
        bindingResult.addError(new FieldError("countryPostVm", "code2", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorVm> response = handler.handleMethodArgumentNotValidPublic(ex,
            new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).containsExactly("code2 must not be blank");
    }

    static class Dummy {
        void handle(@Valid CountryPostVm vm) {
        }
    }

    static class TestableApiExceptionHandler extends ApiExceptionHandler {
        ResponseEntity<ErrorVm> handleOtherExceptionPublic(Exception ex, ServletWebRequest request) {
            return super.handleOtherException(ex, request);
        }

        ResponseEntity<ErrorVm> handleMethodArgumentNotValidPublic(MethodArgumentNotValidException ex,
                                                                   ServletWebRequest request) {
            return super.handleMethodArgumentNotValid(ex, request);
        }
    }
}
