package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.media.utils.FileTypeValidator;
import com.yas.media.utils.ValidFileType;
import jakarta.validation.ConstraintValidatorContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileTypeValidatorTest {

    private FileTypeValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private static byte[] VALID_PNG_BYTES;

    @BeforeAll
    static void generatePng() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        VALID_PNG_BYTES = baos.toByteArray();
    }

    @BeforeEach
    void setUp() {
        validator = new FileTypeValidator();

        // Mock the annotation to supply allowed types
        ValidFileType annotation = mock(ValidFileType.class);
        when(annotation.allowedTypes()).thenReturn(new String[]{"image/jpeg", "image/png", "image/gif"});
        when(annotation.message()).thenReturn("File type not allowed.");
        validator.initialize(annotation);

        // Mock context for constraint violation building
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(violationBuilder);
    }

    @Test
    void isValid_whenFileIsNull_thenReturnFalse() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenContentTypeIsNull_thenReturnFalse() {
        MultipartFile file = new MockMultipartFile("file", "test.png", null, VALID_PNG_BYTES);
        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenContentTypeNotAllowed_thenReturnFalse() {
        MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{1, 2, 3});
        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenValidPngFile_thenReturnTrue() {
        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", VALID_PNG_BYTES);
        boolean result = validator.isValid(file, context);
        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenPngContentTypeButInvalidImageBytes_thenReturnFalse() {
        // Content type says PNG but bytes are not a valid image → ImageIO.read() returns null
        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});
        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenValidJpegFile_thenReturnTrue() throws Exception {
        // Generate a valid JPEG
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", baos.toByteArray());
        boolean result = validator.isValid(file, context);
        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenIOExceptionThrown_thenReturnFalse() throws Exception {
        // Mock MultipartFile that throws IOException on getInputStream()
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }
}
