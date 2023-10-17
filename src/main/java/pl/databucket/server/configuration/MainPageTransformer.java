package pl.databucket.server.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

@Component
@RequiredArgsConstructor
public class MainPageTransformer implements ResourceTransformer {

    private final ServletContext servletContext;


    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
        throws IOException {
        if (Objects.equals(resource.getFilename(), "index.html")) {
            String info = "<span id=\"context-path\" hidden>" + servletContext.getContextPath() + "</span>";

            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            html = html.replace("<body>", "<body>" + info);
            return new TransformedResource(resource, html.getBytes());
        } else {
            return resource;
        }
    }
}
