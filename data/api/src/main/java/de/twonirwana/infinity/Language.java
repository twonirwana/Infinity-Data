package de.twonirwana.infinity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public enum Language {
    English("en", Locale.ENGLISH),
    Spanish("es", Locale.forLanguageTag("es"));

    @Getter
    private final String code;
    @Getter final Locale locale;
}
