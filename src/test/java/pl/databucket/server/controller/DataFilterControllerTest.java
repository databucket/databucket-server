package pl.databucket.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.mapper.DataFilterPropertyMap;
import pl.databucket.server.security.AuthConfig;
import pl.databucket.server.security.AuthResponseBuilder;
import pl.databucket.server.security.OAuth2LogoutHandler;
import pl.databucket.server.security.OAuth2SecurityConfig;
import pl.databucket.server.service.DataFilterService;

@ExtendWith(SpringExtension.class)
//@ContextConfiguration(classes = {AuthConfig.class, OAuth2SecurityConfig.class, CustomJwtDecoder.class})
@WebMvcTest(controllers = DataFilterController.class)
class DataFilterControllerTest {

    @Autowired
    private WebApplicationContext context;
    @MockBean
    DataFilterService dataFilterService;
    @MockBean
    JwtDecoder jwtDecoder;
    @MockBean
    OAuth2LogoutHandler oAuth2LogoutHandler;
    @MockBean
    AuthResponseBuilder authResponseBuilder;
    @MockBean
    AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;
    private static MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @WithMockUser(value = "spring")
    @Test
    void createFilter() throws Exception {
        mvc.perform(post("/api/filters").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void getFilters() throws Exception {
        DataFilter dataFilter = new DataFilter();
        dataFilter.setId(1L);
        dataFilter.setName("FilterName");
        when(dataFilterService.getFilters()).thenReturn(List.of(dataFilter));
        mvc.perform(get("/api/filters").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['name']").value("FilterName"))
            .andExpect(jsonPath("$['id']").value(1L));
    }

    @Test
    void modifyFilter() {
    }

    @Test
    void deleteFilters() {
    }

    @TestConfiguration
    @Import({AuthConfig.class, OAuth2SecurityConfig.class})
    static class AdditionalConfig {

        @Bean
        public ModelMapper modelMapper() {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            modelMapper.addMappings(new DataFilterPropertyMap());
            return modelMapper;
        }

        @Bean
        public OAuth2AuthorizedClientRepository authorizedClientRepository() {
            return new HttpSessionOAuth2AuthorizedClientRepository();
        }
    }
}
