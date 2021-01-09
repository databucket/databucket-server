package pl.databucket.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.databucket.mapper.BucketPropertyMap;
import pl.databucket.mapper.GroupPropertyMap;
import pl.databucket.mapper.AuthProjectPropertyMap;
import pl.databucket.mapper.UserPropertyMap;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.addMappings(new UserPropertyMap());
        modelMapper.addMappings(new BucketPropertyMap());
        modelMapper.addMappings(new GroupPropertyMap());
        modelMapper.addMappings(new AuthProjectPropertyMap());
        return modelMapper;
    }
}