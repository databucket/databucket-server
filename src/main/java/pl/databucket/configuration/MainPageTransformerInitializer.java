package pl.databucket.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MainPageTransformerInitializer {

    private final String contextPath;

    public MainPageTransformerInitializer(@Value("${server.servlet.context-path}") String contextPath) {
        this.contextPath = contextPath;
    }

    public MainPageTransformer initClass() {
        return new MainPageTransformer(this.contextPath);
    }
}