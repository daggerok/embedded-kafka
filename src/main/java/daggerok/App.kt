package daggerok

import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kafka.admin.AdminUtils
import kafka.server.KafkaServerStartable
import kafka.utils.ZkUtils
import kafka.utils.`ZKStringSerializer$`
import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.ZkConnection
import org.apache.log4j.BasicConfigurator
import org.apache.zookeeper.server.NIOServerCnxnFactory
import org.apache.zookeeper.server.ZooKeeperServer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.*

val zkPort = 2181
val httpPort = 8080
val log = LoggerFactory.getLogger(App::class.java)

fun startZk() {

  val dir = Files.createTempDirectory("zk").toFile().absoluteFile
  val zkServer = ZooKeeperServer(dir, dir, 2000)
  val standaloneServerFactory = NIOServerCnxnFactory.createFactory(2181, Short.MAX_VALUE.toInt())

  standaloneServerFactory.startup(zkServer)
  log.info("zookeeper started.\nzkPort: {}\nzkDir: {}", zkPort, dir)
}

fun startKafka() {

  val kafkaProperties = mapOf(
      "zookeeper.connect" to "127.0.0.1:$zkPort",
      "zk.connectiontimeout.ms" to "1000000",
      "brokerid" to "embedded-kafka-broker"
  ).toProperties()
  val config = KafkaServerStartable.fromProps(kafkaProperties)

  config.startup()
  log.info("kafka started.\nkafkaProperties: {}", kafkaProperties)
}

fun handleUserInput(vararg topics: String) {


  val userInput = topics.toList()
  log.info("handling user input: $userInput")
  if (userInput.isEmpty()) return

  val zkUrl = "127.0.0.1:$zkPort"
  val timeout = 60000
  val zkClient = ZkClient(zkUrl, timeout, timeout, `ZKStringSerializer$`.`MODULE$`)
  val zkConnection = ZkConnection(zkUrl, timeout)
  val zkUtils = ZkUtils(zkClient, zkConnection, false)

  userInput
      .flatMap { it.split("\\s+".toRegex()) }
      .filter { it.startsWith("--topics=") }
      .map { it.split("=") }
      .map { it[1] }
      .onEach { println("\n\n $it \n\n") }
      .flatMap { it.split(",", ", ", ";", "; ") }
      .forEach {
        AdminUtils.createTopic(zkUtils, it, 1, 1, Properties(), null)
        log.info("Topic $it created.")
      }
}

fun startHttpServer() {

  val server = HttpServer.create()
  val healthHandler = HttpHandler {
    val body = """{"status":"UP"}""".toByteArray(UTF_8)
    it.responseHeaders.add("content-type", "application/json")
    it.sendResponseHeaders(200, body.size.toLong())
    it.responseBody.use { it.write(body) }
  }

  server.bind(InetSocketAddress(httpPort), 0)
  server.createContext("/", healthHandler)
  server.start()
  log.info("http server started on port: {}", httpPort)
}

class App {
  companion object {
    @JvmStatic fun main(args: Array<String>) {
      BasicConfigurator.configure()
      startZk()
      startKafka()
      handleUserInput(*args)
      startHttpServer()
    }
  }
}
