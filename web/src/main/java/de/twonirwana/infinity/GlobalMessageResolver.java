package de.twonirwana.infinity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.AbstractMessageResolver;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Slf4j
@RequiredArgsConstructor
public class GlobalMessageResolver extends AbstractMessageResolver {

    private final String baseName;

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, context.getLocale());
        String message;
        try {
            message = bundle.getString(key);
            if (messageParameters != null && messageParameters.length > 0) {
                return MessageFormat.format(message, messageParameters);
            }
        } catch (MissingResourceException e) {
            log.error("Missing resource key '{}' in '{}'", key, baseName);
            message = key;
        }

        return message;
    }

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        return key;
    }
}