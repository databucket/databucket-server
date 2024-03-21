package pl.databucket.server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import pl.databucket.server.configuration.MainPageTransformer;
import pl.databucket.server.dto.DataFilterDto;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.mapper.DataFilterPropertyMap;
import pl.databucket.server.security.AuthConfig;
import pl.databucket.server.security.AuthResponseBuilder;
import pl.databucket.server.security.OAuth2LogoutHandler;
import pl.databucket.server.security.OAuth2SecurityConfig;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.DataFilterService;
import pl.databucket.server.service.UserService;

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
    @MockBean
    UserService userService;
    @MockBean
    MainPageTransformer mainPageTransformer;
    @MockBean
    TokenProvider tokenProvider;
    private static MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @WithMockUser(value = "spring", roles = {"ADMIN"})
    @Test
    void createFilter() throws Exception {
        when(dataFilterService.createFilter(any(DataFilterDto.class))).thenReturn(new DataFilter());
        mvc.perform(post("/api/filters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"NAME\"}").with(csrf()))
            .andExpect(status().isCreated());
        verify(dataFilterService).createFilter(any(DataFilterDto.class));
    }

    @WithMockUser(value = "spring")
    @Test
    void getFilters() throws Exception {
        DataFilter dataFilter = new DataFilter();
        dataFilter.setId(1L);
        dataFilter.setName("FilterName");
        when(dataFilterService.getFilters()).thenReturn(List.of(dataFilter));
        mvc.perform(get("/api/filters").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].name").value("FilterName"))
            .andExpect(jsonPath("$.[0].id").value(1L));
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

        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            RestTemplateBuilder rtb = mock(RestTemplateBuilder.class);
            RestTemplate restTemplate = mock(RestTemplate.class);
            when(rtb.build()).thenReturn(restTemplate);
            return rtb;
        }
    }
}
