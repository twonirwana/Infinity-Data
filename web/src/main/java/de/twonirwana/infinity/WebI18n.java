package de.twonirwana.infinity;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Slf4j
public final class WebI18n {
    private final static String MESSAGES_KEY = "messages";

    private WebI18n() {
    }

    public static String getMessage(String key, Language language) {
        try {
            return StringEscapeUtils.unescapeJava(ResourceBundle.getBundle(MESSAGES_KEY, language.getLocale()).getString(key));
        } catch (MissingResourceException e) {
            log.error("Missing I18n for key: {}", key);
            String[] split = key.split("\\.");
            return split[split.length - 1];
        }
    }

    public static String getMessage(String key, Language language, Object... arguments) {
        return MessageFormat.format(getMessage(key, language), arguments);
    }


}
