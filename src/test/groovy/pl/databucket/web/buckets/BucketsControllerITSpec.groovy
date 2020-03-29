package pl.databucket.web.buckets

import static java.util.UUID.randomUUID
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.web.servlet.MockMvc
import pl.databucket.service.DatabucketService
import spock.lang.Specification
import spock.lang.Unroll
import spock.mock.DetachedMockFactory

@AutoConfigureMockMvc
@WebMvcTest(controllers = BucketsController.class)
class BucketsControllerITSpec extends Specification {

  @TestConfiguration
  static class TestContext {

    def factory = new DetachedMockFactory()

    @Bean
    @Primary
    DatabucketService getDatabucketService() {
      factory.Mock(DatabucketService)
    }
  }

  @Autowired
  MockMvc mockMvc

  @Autowired
  DatabucketService databucketService

  def "def should create bucket"() {
    given: 'request data'
    def usernameData = randomUUID().toString()
    def bucketNameData = randomUUID().toString()
    def descriptionData = randomUUID().toString()
    def indexData = 1
    def iconData = 'PanoramaFishEye'
    def data = [
        icon_name  : iconData,
        index      : indexData,
        bucket_name: bucketNameData,
        description: descriptionData
    ]

    when: 'request is performer'
    def result = mockMvc.perform(post("/api/buckets?userName=${usernameData}")
        .contentType(APPLICATION_JSON)
        .content(asJsonString(data)))

    then: 'bucket is created'
    1 * databucketService.createBucket(usernameData, bucketNameData, indexData, descriptionData, iconData, false, null) >> 1
    result
        .andExpect(status().isCreated())
        .andExpect(jsonPath('$.status').value('OK'))
    //Without .toString() on below expect we would compare String to GString (Which would always fail despite they look the same)
        .andExpect(jsonPath('$.message').value("The bucket '${bucketNameData}' has been successfully created.".toString()))
        .andExpect(jsonPath('$.bucket_id').value(1))

  }

  @Unroll
  def "should return [#status] with [#message] message for #requestData"() {
    given: 'username'
    def usernameData = randomUUID().toString()

    when: 'request is performed'
    def result = mockMvc.perform(post("/api/buckets?userName=${usernameData}")
        .contentType(APPLICATION_JSON)
        .content(asJsonString(requestData)))

    then: 'validation fails'
    0 * databucketService.createBucket(*_)
    result
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath('$.status').value(status))
        .andExpect(jsonPath('$.message').value(message))

    where:
    requestData           | status   | message
    [:]                   | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [index: 1]            | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [bucket_name: '']     | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [bucket_name: null]   | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    // Below case returns HTTP.500, but it should return 406 like above cases.
    [bucket_name: '123$'] | 'FAILED' | 'Invalid name [bucket_name]. The name must match to the pattern: [a-zA-Z0-9-]+'
  }

  def asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj)
    } catch (Exception e) {
      throw new RuntimeException(e)
    }
  }

}


