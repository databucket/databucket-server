package pl.databucket.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.databucket.mapper.*;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(new ProjectPropertyMap());
        modelMapper.addMappings(new UserPropertyMap());
        modelMapper.addMappings(new BucketPropertyMap());
        modelMapper.addMappings(new GroupPropertyMap());
        modelMapper.addMappings(new AuthProjectPropertyMap());
        modelMapper.addMappings(new DataClassPropertyMap());
        modelMapper.addMappings(new DataColumnsPropertyMap());
        modelMapper.addMappings(new DataFilterPropertyMap());
        modelMapper.addMappings(new TagPropertyMap());
        modelMapper.addMappings(new TaskPropertyMap());
        modelMapper.addMappings(new ViewPropertyMap());
        return modelMapper;
    }
}