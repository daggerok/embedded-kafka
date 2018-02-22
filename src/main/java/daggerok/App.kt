package daggerok

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.kafka.test.rule.KafkaEmbedded
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@SpringBootApplication
class App {

  @Bean
  fun kafka(@Value("\${topics:test}") topics: String): KafkaEmbedded {
    val kafkaEmbedded = KafkaEmbedded(1, false, *topics.split(",").toTypedArray())
    kafkaEmbedded.setKafkaPorts(9092)
    return kafkaEmbedded
  }

  val healthChecker: (ServerRequest) -> Mono<ServerResponse> = {
    val data = mapOf("status" to "UP")
    ok().body(Mono.just(data), data.javaClass)
  }

  @Bean
  fun router() = router {
    contentType(APPLICATION_JSON_UTF8)
    GET("/**", healthChecker)
  }
}

fun main(args: Array<String>) {
  runApplication<App>(*args)
}
