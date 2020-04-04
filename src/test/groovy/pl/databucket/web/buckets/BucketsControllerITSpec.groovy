package pl.databucket.web.buckets

import static java.util.UUID.randomUUID
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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

  def "should create bucket"() {
    given: 'request data'
    def userNameData = randomUUID().toString()
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

    when: 'request is performed'
    def result = mockMvc.perform(post("/api/buckets?userName=${userNameData}")
        .contentType(APPLICATION_JSON)
        .content(asJsonString(data)))

    then: 'bucket is created'
    1 * databucketService.createBucket(userNameData, bucketNameData, indexData, descriptionData, iconData, false, null) >> 1
    result
        .andExpect(status().isCreated())
        .andExpect(jsonPath('$.status').value('OK'))
    //Without .toString() on below expect we would compare String to GString (Which would always fail despite they look the same)
        .andExpect(jsonPath('$.message').value("The bucket '${bucketNameData}' has been successfully created.".toString()))
        .andExpect(jsonPath('$.bucket_id').value(1))
  }

  @Unroll
  def "create bucket should return [#status] with [#message] message for #requestData"() {
    given: 'user name'
    def userNameData = randomUUID().toString()

    when: 'request is performed'
    def result = mockMvc.perform(post("/api/buckets?userName=${userNameData}")
        .contentType(APPLICATION_JSON)
        .content(asJsonString(requestData)))

    then: 'validation fails'
    0 * databucketService.createBucket(*_)
    result
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath('$.status').value(status))
        .andExpect(jsonPath('$.message').value(message))

    where:
    requestData                         | status   | message
    [:]                                 | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [index: 1]                          | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [bucket_name: '']                   | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [bucket_name: null]                 | 'FAILED' | 'The \'bucket_name\' item can not be empty!'
    [bucket_name: '123$']               | 'FAILED' | 'Invalid name [bucket_name]. The name must match to the pattern: [a-zA-Z0-9-]+'
    [bucket_name: 'b1', index: 1.2]     | 'FAILED' | 'The \'index\' must be a natural number!'
    [bucket_name: 'b1', index: '1']     | 'FAILED' | 'The \'index\' must be a natural number!'
  }

  def "should modify bucket"() {
    given: 'request data'
    def userNameData = randomUUID().toString()
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

    when: 'request is performed'
    def result = mockMvc.perform(put("/api/buckets/${bucketNameData}?userName=${userNameData}")
            .contentType(APPLICATION_JSON)
            .content(asJsonString(data)))

    then: 'bucket is modified'
    1 * databucketService.modifyBucket(userNameData, bucketNameData, data)
    result
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.status').value('OK'))
            .andExpect(jsonPath('$.message').value("Bucket '${bucketNameData}' has been successfully modified.".toString()))
  }

  def "should delete bucket"() {
    given: 'request data'
    def userNameData = randomUUID().toString()
    def bucketNameData = randomUUID().toString()

    when: 'request is performed'
    def result = mockMvc.perform(delete("/api/buckets/${bucketNameData}?userName=${userNameData}")
            .contentType(APPLICATION_JSON))

    then: 'bucket is deleted'
    1 * databucketService.deleteBucket(bucketNameData, userNameData)
    result
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.status').value('OK'))
  }

  def asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj)
    } catch (Exception e) {
      throw new RuntimeException(e)
    }
  }

}


