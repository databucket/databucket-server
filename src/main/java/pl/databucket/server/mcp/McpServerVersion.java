package pl.databucket.server.mcp;

import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The MCP server introduces itself to the agents with the version of Databucket (instead of Spring AI's 1.0.0). */
@Configuration
public class McpServerVersion {

    @Bean
    static BeanPostProcessor mcpServerVersionSetter(ObjectProvider<BuildProperties> buildProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof McpServerProperties properties)
                    buildProperties.ifAvailable(build -> properties.setVersion(build.getVersion()));
                return bean;
            }
        };
    }
}
