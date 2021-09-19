package pl.databucket.server.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
@EnableJpaRepositories("pl.databucket.server.repository")
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    MainPageTransformerInitializer mainPageTransformerInitializer;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath.contains("static/"))
                            resourcePath = resourcePath.substring(resourcePath.indexOf("static/"));
                        if (resourcePath.endsWith("favicon.ico"))
                            resourcePath = "/favicon.ico";

                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
//                            System.out.println(">>>> 1 >>> resourcePath: " + resourcePath + " >>> location: " + location);
                            return requestedResource;
                        } else {
                            ClassPathResource resource = new ClassPathResource("/static/index.html");
//                            System.out.println(">>>> 2 >>> resourcePath: " + resourcePath + " >>> location: " + location + " >>> exists: " + resource.exists());
                            return resource;
                        }
                    }
                })
                .addTransformer(mainPageTransformerInitializer.initClass());
    }
}