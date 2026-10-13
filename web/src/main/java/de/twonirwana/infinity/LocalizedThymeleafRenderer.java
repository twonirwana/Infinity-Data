package de.twonirwana.infinity;

import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import org.jspecify.annotations.NonNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Locale;
import java.util.Map;

public class LocalizedThymeleafRenderer implements FileRenderer {

    private final TemplateEngine templateEngine;

    public LocalizedThymeleafRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public @NonNull String render(
            @NonNull String filePath,
            @NonNull Map<String, ?> model,
            Context ctx) {
        Locale locale = ctx.attribute("locale");

        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(ctx.req().getServletContext());
        IServletWebExchange webExchange = application.buildExchange(ctx.req(), ctx.res());

        WebContext thymeleafContext = new WebContext(webExchange, locale, (Map<String, Object>) model);

        thymeleafContext.setVariables((Map<String, Object>) model);

        return templateEngine.process(filePath, thymeleafContext);
    }
}

