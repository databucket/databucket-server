package pl.databucket.server.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TreeMap;

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
                        .description("""
                                Sign in with **POST /api/public/signin**, then click **Authorize** and paste the token. \
                                The token is valid for one project and expires after 24 hours by default; then sign in again.

                                Errors are returned as `{"message": "..."}`.

                                Guide, rules and examples: \
                                [Get started with API](https://github.com/databucket/databucket-server/wiki/Get-started-with-API), \
                                [Rules](https://github.com/databucket/databucket-server/wiki/Rules)."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("The token from POST /api/public/signin")));
    }

    /** Adds the responses of a missing token or role to every secured operation, in the order of the codes. */
    @Bean
    public OpenApiCustomizer securedOperationResponses() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().stream()
                .filter(operation -> operation.getSecurity() != null)
                .forEach(operation -> {
                    ApiResponses responses = operation.getResponses();
                    responses.addApiResponse("401", new ApiResponse()
                            .description("Unauthorized - the token is missing, invalid or expired; sign in again"));
                    responses.addApiResponse("403", new ApiResponse()
                            .description("Forbidden - the user has neither the MEMBER nor the ROBOT role"));
                    ApiResponses sorted = new ApiResponses();
                    new TreeMap<>(responses).forEach(sorted::addApiResponse);
                    operation.setResponses(sorted);
                }));
    }
}
