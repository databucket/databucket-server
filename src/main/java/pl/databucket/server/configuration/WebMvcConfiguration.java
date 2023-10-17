package pl.databucket.server.configuration;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final MainPageTransformer mainPageTransformer;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    if (resourcePath.contains("static/")) {
                        resourcePath = resourcePath.substring(resourcePath.indexOf("static/"));
                    }
                    if (resourcePath.endsWith("favicon.ico")) {
                        resourcePath = "/favicon.ico";
                    }

                    Resource requestedResource = location.createRelative(resourcePath);
                    if (requestedResource.exists() && requestedResource.isReadable()) {
//                            System.out.println(">>>> 1 >>> resourcePath: " + resourcePath + " >>> location: " + location);
                        return requestedResource;
                    } else {
                        return new ClassPathResource("/static/index.html");
                    }
                }
            })
            .addTransformer(mainPageTransformer);
    }
}
