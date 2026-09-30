package de.twonirwana.infinity;

import lombok.RequiredArgsConstructor;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.AbstractMessageResolver;

import java.text.MessageFormat;
import java.util.ResourceBundle;

@RequiredArgsConstructor
public class GlobalMessageResolver extends AbstractMessageResolver {

    private final String baseName;

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, context.getLocale());
        String message = bundle.getString(key);

        if (messageParameters != null && messageParameters.length > 0) {
            return MessageFormat.format(message, messageParameters);
        }

        return message;
    }

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        return key;
    }
}