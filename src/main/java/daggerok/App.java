package daggerok;

import info.batey.kafka.unit.KafkaUnit;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

  @SneakyThrows
  public static void main(String[] args) {

    final KafkaUnit kafkaUnitServer = new KafkaUnit("localhost:2181", "localhost:9092");

    Try.run(kafkaUnitServer::startup)
       .andThen(() -> kafkaUnitServer.createTopic("KafkaUnitTopic"))
       .onFailure(throwable -> {
         log.error("startup failed: {}", throwable.getLocalizedMessage(), throwable);
         kafkaUnitServer.shutdown();
       });
  }
}
