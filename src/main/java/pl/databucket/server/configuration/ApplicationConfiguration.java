package pl.databucket.server.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import pl.databucket.server.mapper.*;

@Configuration
@EnableJpaRepositories("pl.databucket.server.repository")
public class ApplicationConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(new ManageProjectPropertyMap());
        modelMapper.addMappings(new UserPropertyMap());
        modelMapper.addMappings(new ManageUserPropertyMap());
        modelMapper.addMappings(new BucketPropertyMap());
        modelMapper.addMappings(new GroupPropertyMap());
        modelMapper.addMappings(new AuthProjectPropertyMap());
        modelMapper.addMappings(new DataClassPropertyMap());
        modelMapper.addMappings(new DataColumnsPropertyMap());
        modelMapper.addMappings(new DataFilterPropertyMap());
        modelMapper.addMappings(new TagPropertyMap());
        modelMapper.addMappings(new TaskPropertyMap());
        modelMapper.addMappings(new ViewPropertyMap());
        modelMapper.addMappings(new RolePropertyMap());
        modelMapper.addMappings(new DataEnumPropertyMap());
        modelMapper.addMappings(new AccessTreeGroupPropertyMap());
        modelMapper.addMappings(new AccessTreeBucketPropertyMap());
        modelMapper.addMappings(new AccessTreeViewPropertyMap());
        modelMapper.addMappings(new UserColumnsPropertyMap());
        modelMapper.addMappings(new TemplatePropertyMap());
        modelMapper.addMappings(new TemplateDataPropertyMap());
        modelMapper.addMappings(new TemplateDataItemPropertyMap());
        modelMapper.addMappings(new SvgPropertyMap());
        return modelMapper;
    }
}
