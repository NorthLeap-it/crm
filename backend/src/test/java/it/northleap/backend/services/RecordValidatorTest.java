package it.northleap.backend.services;

import it.northleap.backend.entities.FieldDef;
import it.northleap.backend.entities.FieldType;
import it.northleap.backend.exceptions.RecordValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordValidatorTest {

    private RecordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RecordValidator();
    }

    private FieldDef field(String key, FieldType type) {
        FieldDef f = new FieldDef();
        f.setKey(key);
        f.setLabel(key);
        f.setType(type);
        return f;
    }

    @Test
    void requiredFieldMissingThrows() {
        FieldDef f = field("name", FieldType.TEXT);
        f.setRequired(true);

        assertThrows(RecordValidationException.class, () -> validator.validate(List.of(f), Map.of()));
    }

    @Test
    void defaultValueAppliedWhenMissing() {
        FieldDef f = field("priority", FieldType.TEXT);
        f.setDefaultValue("low");

        Map<String, Object> out = validator.validate(List.of(f), Map.of());

        assertEquals("low", out.get("priority"));
    }

    @Test
    void numberWithinBoundsPasses() {
        FieldDef f = field("amount", FieldType.NUMBER);
        f.setMin(0f);
        f.setMax(1000f);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("amount", 500));

        assertEquals(500.0, out.get("amount"));
    }

    @Test
    void numberOutOfBoundsThrows() {
        FieldDef f = field("amount", FieldType.NUMBER);
        f.setMax(100f);

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("amount", 500)));
    }

    @Test
    void integerFieldRejectsFraction() {
        FieldDef f = field("qty", FieldType.INTEGER);

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("qty", 3.5)));
    }

    @Test
    void integerFieldCoercesWholeNumber() {
        FieldDef f = field("qty", FieldType.INTEGER);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("qty", 3.0));

        assertEquals(3L, out.get("qty"));
    }

    @Test
    void stringLengthAndPatternEnforced() {
        FieldDef f = field("code", FieldType.TEXT);
        f.setMin(2f);
        f.setMax(5f);
        f.setPattern("^[A-Z]+$");

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("code", "a")));
        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("code", "abc")));

        Map<String, Object> out = validator.validate(List.of(f), Map.of("code", "ABC"));
        assertEquals("ABC", out.get("code"));
    }

    @Test
    void emailValidatedAndLowercased() {
        FieldDef f = field("email", FieldType.EMAIL);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("email", "User@Example.com"));
        assertEquals("user@example.com", out.get("email"));

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("email", "not-an-email")));
    }

    @Test
    void urlMustBeAbsolute() {
        FieldDef f = field("website", FieldType.URL);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("website", "https://example.com"));
        assertEquals("https://example.com", out.get("website"));

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("website", "not a url")));
    }

    @Test
    void colorMustBeHex() {
        FieldDef f = field("brandColor", FieldType.COLOR);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("brandColor", "#0A84FF"));
        assertEquals("#0A84FF", out.get("brandColor"));

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("brandColor", "blue")));
    }

    @Test
    void booleanCoercedFromStringAndNative() {
        FieldDef f = field("done", FieldType.BOOLEAN);

        assertEquals(true, validator.validate(List.of(f), Map.of("done", "true")).get("done"));
        assertEquals(true, validator.validate(List.of(f), Map.of("done", "1")).get("done"));
        assertEquals(false, validator.validate(List.of(f), Map.of("done", "no")).get("done"));
        assertEquals(true, validator.validate(List.of(f), Map.of("done", Boolean.TRUE)).get("done"));
    }

    @Test
    void selectEnforcesOptionsWhitelist() {
        FieldDef f = field("status", FieldType.SELECT);
        f.setOptions(List.of(Map.of("value", "new", "label", "Nuovo"), Map.of("value", "lost", "label", "Perso")));

        Map<String, Object> out = validator.validate(List.of(f), Map.of("status", "new"));
        assertEquals("new", out.get("status"));

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("status", "unknown")));
    }

    @Test
    void multiselectNormalizedToList() {
        FieldDef f = field("tags", FieldType.MULTISELECT);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("tags", "vip"));
        assertEquals(List.of("vip"), out.get("tags"));

        Map<String, Object> out2 = validator.validate(List.of(f), Map.of("tags", List.of("vip", "urgent")));
        assertEquals(List.of("vip", "urgent"), out2.get("tags"));
    }

    @Test
    void relationSingleNormalizesToFirstElement() {
        FieldDef f = field("company", FieldType.RELATION);
        f.setConfig(Map.of("multiple", false));

        Map<String, Object> out = validator.validate(List.of(f), Map.of("company", List.of("id-1", "id-2")));
        assertEquals("id-1", out.get("company"));
    }

    @Test
    void relationMultipleNormalizesToList() {
        FieldDef f = field("products", FieldType.RELATION);
        f.setConfig(Map.of("multiple", true));

        Map<String, Object> out = validator.validate(List.of(f), Map.of("products", "id-1"));
        assertEquals(List.of("id-1"), out.get("products"));
    }

    @Test
    void dateNormalizedToIsoDate() {
        FieldDef f = field("closeDate", FieldType.DATE);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("closeDate", "2026-06-19T10:00:00Z"));
        assertEquals("2026-06-19", out.get("closeDate"));
    }

    @Test
    void invalidDateThrows() {
        FieldDef f = field("closeDate", FieldType.DATE);

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("closeDate", "not-a-date")));
    }

    @Test
    void datetimeNormalizedToInstant() {
        FieldDef f = field("dueAt", FieldType.DATETIME);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("dueAt", "2026-06-19T10:00:00Z"));
        assertEquals("2026-06-19T10:00:00Z", out.get("dueAt"));
    }

    @Test
    void geoRequiresLatLng() {
        FieldDef f = field("location", FieldType.GEO);

        Map<String, Object> out = validator.validate(List.of(f), Map.of("location", Map.of("lat", 45.0, "lng", 9.0)));
        assertEquals(Map.of("lat", 45.0, "lng", 9.0), out.get("location"));

        assertThrows(RecordValidationException.class,
                () -> validator.validate(List.of(f), Map.of("location", Map.of("lat", 45.0))));
    }

    @Test
    void deriveTitlePrefersKnownKeys() {
        FieldDef textField = field("subject", FieldType.TEXT);
        String title = validator.deriveTitle(List.of(textField), Map.of("subject", "Hello", "name", "World"));
        assertEquals("World", title); // "name" ha priorità su "subject" nell'ordine TITLE_KEYS
    }

    @Test
    void deriveTitleFallsBackToFirstNameLastName() {
        String title = validator.deriveTitle(List.of(), Map.of("firstName", "Luca", "lastName", "Ferrari"));
        assertEquals("Luca Ferrari", title);
    }

    @Test
    void deriveTitleDefaultsToSenzaTitolo() {
        String title = validator.deriveTitle(List.of(), Map.of());
        assertEquals("Senza titolo", title);
    }

    @Test
    void deriveStatusUsesStatusTypeField() {
        FieldDef statusField = field("stage", FieldType.STATUS);
        String status = validator.deriveStatus(List.of(statusField), Map.of("stage", "won"));
        assertEquals("won", status);
    }

    @Test
    void deriveStatusReturnsNullWhenAbsent() {
        String status = validator.deriveStatus(List.of(), Map.of());
        assertNull(status);
    }
}
