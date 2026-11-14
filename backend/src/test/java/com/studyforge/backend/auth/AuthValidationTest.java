package com.studyforge.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.studyforge.backend.common.ApiException;

/** Validarile de la inregistrare. Nu au nevoie de baza de date. */
class AuthValidationTest {

    @Test
    void usernameMustBeFirstAndLastName() {
        assertThat(AuthService.normalizeUsername("  Popescu   Ion ")).isEqualTo("Popescu Ion");
        assertThat(AuthService.normalizeUsername("Ionescu Ana-Maria")).isEqualTo("Ionescu Ana-Maria");
        assertThat(AuthService.normalizeUsername("Țărău Ștefan Andrei")).isEqualTo("Țărău Ștefan Andrei");

        assertThatThrownBy(() -> AuthService.normalizeUsername("Popescu")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.normalizeUsername("ion123 pop")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.normalizeUsername("   ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.normalizeUsername(null)).isInstanceOf(ApiException.class);
    }

    @Test
    void emailIsLowercasedAndValidated() {
        assertThat(AuthService.normalizeEmail("  Ion.Popescu@Exemplu.RO ")).isEqualTo("ion.popescu@exemplu.ro");

        assertThatThrownBy(() -> AuthService.normalizeEmail("ion@")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.normalizeEmail("ion.exemplu.ro")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.normalizeEmail("ion pop@exemplu.ro")).isInstanceOf(ApiException.class);
    }

    @Test
    void genderAcceptsOnlyKnownValues() {
        assertThat(Gender.parse("MALE")).isEqualTo(Gender.MALE);
        assertThat(Gender.parse(" female ")).isEqualTo(Gender.FEMALE);
        assertThat(Gender.parse("Other")).isEqualTo(Gender.OTHER);

        assertThatThrownBy(() -> Gender.parse("woman")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Gender.parse("")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Gender.parse(null)).isInstanceOf(ApiException.class);
    }

    @Test
    void passwordLengthIsChecked() {
        AuthService.validatePassword("parola123");

        assertThatThrownBy(() -> AuthService.validatePassword("scurta")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.validatePassword("x".repeat(73))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.validatePassword("          ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AuthService.validatePassword(null)).isInstanceOf(ApiException.class);
    }
}
