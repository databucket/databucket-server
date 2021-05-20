package pl.databucket.controller.configuration


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

@AutoConfigureMockMvc
@WebMvcTest(controllers = ConfigurationController.class)
class ConfigurationControllerITSpec extends Specification {

  @Autowired
  MockMvc mockMvc

//  def "should return app title"() {
//    when:
//    def result = mockMvc.perform(get('/api/title'))
//
//    then:
//    result
//        .andExpect(status().isOk())
//        .andExpect(content().contentType(APPLICATION_JSON))
//        .andExpect(jsonPath('$.title').value('Databucket'))
//  }
}
