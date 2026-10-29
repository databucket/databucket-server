package pl.databucket.server.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The API published in Swagger: signing in and working on bucket data. Endpoints used only by the
 * web application are hidden (@Hidden), so they can change together with it.
 */
@Configuration
public class OpenApiConfiguration {

    /** Name of the security scheme referenced by @SecurityRequirement on secured controllers. */
    public static final String BEARER = "bearer";

    @Bean
    public OpenAPI openApi(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        return new OpenAPI()
                .info(new Info()
                        .title("Databucket API")
                        .version(build != null ? build.getVersion() : null)
                        .description("Sign in with POST /api/public/signin, then click Authorize and paste the token."))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("The token from POST /api/public/signin")));
    }
}
