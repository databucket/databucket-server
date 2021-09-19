package pl.databucket.server.configuration;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class MainPageTransformer implements ResourceTransformer {

    private final String contextPath;

    public MainPageTransformer(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
        if (resource.getFilename().equals("index.html")) {
            String info = "<span id=\"context-path\" hidden>" + contextPath + "</span>";

            String html = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
            html = html.replace("<body>", "<body>" + info);
            return new TransformedResource(resource, html.getBytes());
        } else
            return resource;
    }
}