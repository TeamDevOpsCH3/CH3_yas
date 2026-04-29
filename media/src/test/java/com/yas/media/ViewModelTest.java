package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.media.viewmodel.ErrorVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for viewmodel classes.
 * Covers getters/setters, constructors, and record accessors to improve
 * the viewmodel package line coverage (currently 65%).
 */
class ViewModelTest {

    // ------------------------------------------------------------------ //
    // MediaVm                                                              //
    // ------------------------------------------------------------------ //

    @Test
    void mediaVm_constructorAndGetters_workCorrectly() {
        MediaVm vm = new MediaVm(1L, "caption", "photo.png", "image/png", "http://example.com/photo.png");

        assertThat(vm.getId()).isEqualTo(1L);
        assertThat(vm.getCaption()).isEqualTo("caption");
        assertThat(vm.getFileName()).isEqualTo("photo.png");
        assertThat(vm.getMediaType()).isEqualTo("image/png");
        assertThat(vm.getUrl()).isEqualTo("http://example.com/photo.png");
    }

    @Test
    void mediaVm_setters_workCorrectly() {
        MediaVm vm = new MediaVm(null, null, null, null, null);
        vm.setId(2L);
        vm.setCaption("new caption");
        vm.setFileName("new.jpg");
        vm.setMediaType("image/jpeg");
        vm.setUrl("http://example.com/new.jpg");

        assertThat(vm.getId()).isEqualTo(2L);
        assertThat(vm.getCaption()).isEqualTo("new caption");
        assertThat(vm.getFileName()).isEqualTo("new.jpg");
        assertThat(vm.getMediaType()).isEqualTo("image/jpeg");
        assertThat(vm.getUrl()).isEqualTo("http://example.com/new.jpg");
    }

    // ------------------------------------------------------------------ //
    // NoFileMediaVm (record)                                              //
    // ------------------------------------------------------------------ //

    @Test
    void noFileMediaVm_recordAccessors_workCorrectly() {
        NoFileMediaVm vm = new NoFileMediaVm(10L, "cat", "file.png", "image/png");

        assertThat(vm.id()).isEqualTo(10L);
        assertThat(vm.caption()).isEqualTo("cat");
        assertThat(vm.fileName()).isEqualTo("file.png");
        assertThat(vm.mediaType()).isEqualTo("image/png");
    }

    @Test
    void noFileMediaVm_equalityAndToString() {
        NoFileMediaVm vm1 = new NoFileMediaVm(1L, "cap", "f.png", "image/png");
        NoFileMediaVm vm2 = new NoFileMediaVm(1L, "cap", "f.png", "image/png");

        assertThat(vm1).isEqualTo(vm2);
        assertThat(vm1.toString()).contains("NoFileMediaVm");
    }

    // ------------------------------------------------------------------ //
    // ErrorVm (record)                                                     //
    // ------------------------------------------------------------------ //

    @Test
    void errorVm_recordAccessors_workCorrectly() {
        ErrorVm vm = new ErrorVm("404", "Not Found", "Resource not found");

        assertThat(vm.statusCode()).isEqualTo("404");
        assertThat(vm.title()).isEqualTo("Not Found");
        assertThat(vm.detail()).isEqualTo("Resource not found");
    }
}
