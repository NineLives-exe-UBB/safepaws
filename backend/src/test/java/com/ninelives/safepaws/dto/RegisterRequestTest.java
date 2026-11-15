package com.ninelives.safepaws.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The validation rules of POST /auth/register, without Spring.
 */
class RegisterRequestTest {

    private static final String VALID_PASSWORD = "SafePaws1!";
    private static final String PASSWORD_RULE =
            "Password must have at least 8 characters, one uppercase letter, one digit and one special character";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    /** "field: message" for every broken rule, sorted. */
    private static List<String> errors(String email, String password, String displayName) {
        return validator.validate(new RegisterRequest(email, password, displayName)).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
    }

    @Test
    void aValidRequestHasNoErrors() {
        assertThat(errors("ana@safepaws.dev", VALID_PASSWORD, "Ana Pop")).isEmpty();
    }

    @Test
    void emailIsTrimmedAndLowercased() {
        RegisterRequest request = new RegisterRequest("  Ana.Pop@SafePaws.DEV ", VALID_PASSWORD, "Ana");

        assertThat(request.email()).isEqualTo("ana.pop@safepaws.dev");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void displayNameIsTrimmed() {
        assertThat(new RegisterRequest("a@b.ro", VALID_PASSWORD, "  Ana Pop ").displayName()).isEqualTo("Ana Pop");
    }

    @Test
    void everyFieldMissingGivesOneRequiredErrorEach() {
        assertThat(errors(null, null, null)).containsExactly(
                "displayName: Display name is required",
                "email: Email is required",
                "password: Password is required");
    }

    @Test
    void blankFieldsOnlyGetTheRequiredError() {
        assertThat(errors("   ", "   ", "   ")).containsExactly(
                "displayName: Display name is required",
                "email: Email is required",
                "password: Password is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "@safepaws.dev", "ana@safepaws", "ana pop@safepaws.dev", "ana@@safepaws.dev"})
    void anInvalidEmailIsRejected(String email) {
        assertThat(errors(email, VALID_PASSWORD, "Ana")).containsExactly("email: Email must be a valid email address");
    }

    @Test
    void emailLongerThan255CharactersIsRejected() {
        // a valid shape (64-character local part, labels under 64), 256 characters in total
        String label = "b".repeat(60);
        String email = "a".repeat(64) + "@" + label + "." + label + "." + label + "." + "c".repeat(8);
        assertThat(email).hasSize(256);

        assertThat(errors(email, VALID_PASSWORD, "Ana")).containsExactly("email: Email must be at most 255 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Safe1!",          // 6 characters
            "Safepaws1!a",     // fine, control below
    })
    void passwordLengthIsChecked(String password) {
        List<String> expected = password.length() < 8 ? List.of("password: " + PASSWORD_RULE) : List.of();

        assertThat(errors("a@b.ro", password, "Ana")).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "safepaws1!",      // no uppercase letter
            "SafePaws!!",      // no digit
            "SafePaws12",      // no special character
            "SafePa1!",        // 8 characters exactly is fine (control, see below)
    })
    void passwordNeedsAnUppercaseLetterADigitAndASpecialCharacter(String password) {
        List<String> expected = password.equals("SafePa1!") ? List.of() : List.of("password: " + PASSWORD_RULE);

        assertThat(errors("a@b.ro", password, "Ana")).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Safe Paws1", "SafePaws1_", "SafePaws1€", "Parolă12-"})
    void anythingThatIsNotALetterOrADigitCountsAsSpecial(String password) {
        assertThat(errors("a@b.ro", password, "Ana")).isEmpty();
    }

    @Test
    void romanianLettersCountAsLettersNotAsSpecialCharacters() {
        // "Ă" is the uppercase letter; nothing here is special
        assertThat(errors("a@b.ro", "Ăparola12", "Ana")).containsExactly("password: " + PASSWORD_RULE);
    }

    @Test
    void passwordOf72CharactersIsAcceptedAnd73IsRejected() {
        String base = "SafePaws1!";

        assertThat(errors("a@b.ro", base + "a".repeat(62), "Ana")).isEmpty();
        assertThat(errors("a@b.ro", base + "a".repeat(63), "Ana"))
                .containsExactly("password: Password must be at most 72 characters");
    }

    @Test
    void passwordLimitCountsBytesBecauseThatIsWhatBcryptReads() {
        // 10 + 31 * 2 bytes = 72 bytes is fine; one more "ă" is 74 bytes
        assertThat(errors("a@b.ro", "SafePaws1!" + "ă".repeat(31), "Ana")).isEmpty();
        assertThat(errors("a@b.ro", "SafePaws1!" + "ă".repeat(32), "Ana"))
                .containsExactly("password: Password must be at most 72 characters");
    }

    @Test
    void displayNameLongerThan50CharactersIsRejected() {
        assertThat(errors("a@b.ro", VALID_PASSWORD, "a".repeat(51)))
                .containsExactly("displayName: Display name must be at most 50 characters");
        assertThat(errors("a@b.ro", VALID_PASSWORD, "a".repeat(50))).isEmpty();
    }

    @Test
    void messagesHaveNoFullStop() {
        assertThat(validator.validate(new RegisterRequest("x", "y", "")))
                .extracting(ConstraintViolation::getMessage)
                .allSatisfy(message -> assertThat(message).doesNotEndWith("."));
    }

    @Test
    void toStringNeverShowsThePassword() {
        assertThat(new RegisterRequest("ana@safepaws.dev", VALID_PASSWORD, "Ana").toString())
                .doesNotContain(VALID_PASSWORD)
                .contains("ana@safepaws.dev");
    }
}
