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
import java.nio.file.Paths
import java.util.*

const val defaultZkPort = "2181"
const val defaultZkDir = "zk"

const val defaultKafkaPort = "9092"

const val defaultHttpPort = "8080"
const val defaultHttpContext = "/"

val log = LoggerFactory.getLogger(App::class.java)!!

/**
 * Starts zookeeper server
 *
 * default configurations:
 * --zookeeperPort=2181
 * --zookeeperDir=zk
 */
fun startZk(config: Map<String, List<String>>) {

  var zkDir = config.getOrDefault("zookeeperDir", listOf(defaultZkDir)).first()
  if (zkDir.startsWith(".")) zkDir = Paths.get(zkDir).toAbsolutePath().toString()
  if (zkDir.isEmpty()) zkDir = defaultZkDir

  val aPath = Paths.get(zkDir, "zk-${UUID.randomUUID()}", "embedded-kafka").toAbsolutePath()
  aPath.toFile().parentFile.deleteOnExit()

  val zookeeperDir = Files.createDirectories(aPath).toFile()
  val zkServer = ZooKeeperServer(zookeeperDir, zookeeperDir, 2000)
  val zookeeperPort = config.getOrDefault("zookeeperPort", listOf(defaultZkPort)).first()
  val standaloneServerFactory = NIOServerCnxnFactory.createFactory(zookeeperPort.toInt(), Short.MAX_VALUE.toInt())

  standaloneServerFactory.startup(zkServer)
  log.info("zookeeper server started.")
}

/**
 * Starts kafka server
 *
 * default configurations:
 * --kafkaPort=9092
 */
fun startKafka(config: Map<String, List<String>>) {

  val zookeeperPort = config.getOrDefault("zookeeperPort", listOf(defaultZkPort)).first()
  val kafkaPort = config.getOrDefault("kafkaPort", listOf(defaultKafkaPort)).first()
  val kafkaProperties = mapOf(
      "zookeeper.connect" to "127.0.0.1:$zookeeperPort",
      "zk.connectiontimeout.ms" to "1000000",
      "bootstrap.servers" to "127.0.0.1:$kafkaPort"
  ).toProperties()
  val kafkaServer = KafkaServerStartable.fromProps(kafkaProperties)

  kafkaServer.startup()
  log.info("kafka server started")
}

/**
 * Creates kafka topic if proper configuration provided
 *
 * for example:
 * --kafkaTopics=topic1,topic2
 */
fun createKafkaTopics(config: Map<String, List<String>>) {

  val kafkaTopics = config.getOrDefault("kafkaTopics", listOf())
  if (kafkaTopics.isEmpty()) return

  val zookeeperPort = config.getOrDefault("zookeeperPort", listOf(defaultZkPort)).first()
  val zkUrl = "127.0.0.1:$zookeeperPort"
  val timeout = 60000
  val zkClient = ZkClient(zkUrl, timeout, timeout, `ZKStringSerializer$`.`MODULE$`)
  val zkUtils = ZkUtils(zkClient, ZkConnection(zkUrl, timeout), false)

  kafkaTopics.forEach {
    AdminUtils.createTopic(zkUtils, it, 1, 1, Properties(), null)
    log.info("Topic $it created.")
  }
}

/**
 * Starts http server with health endpoint
 *
 * default configurations:
 * --httpPort=8080
 * --httpContext=/
 */
fun startHttpServer(config: Map<String, List<String>>) {

  val server = HttpServer.create()
  val healthHandler = HttpHandler {
    val body = """{"status":"UP"}""".toByteArray(UTF_8)
    it.responseHeaders.add("content-type", "application/json")
    it.sendResponseHeaders(200, body.size.toLong())
    it.responseBody.use { it.write(body) }
  }

  val httpPort = config.getOrDefault("httpPort", listOf(defaultHttpPort)).first()
  val httpContext = config.getOrDefault("httpContext", listOf(defaultHttpContext)).first()

  server.bind(InetSocketAddress(httpPort.toInt()), 0)
  server.createContext(httpContext, healthHandler)
  server.start()
  log.info("http server started.")
}

/**
 * Parses application configurations from user inputs
 *
 * supported config properties:
 * --zookeeperPort=2181
 * --zookeeperDir=zk
 * --kafkaPort=9092
 * --kafkaTopics=topic1,topic2
 * --httpPort=8080
 * --httpContext=/
 */
fun parseConfigProps(args: Array<String>): Map<String, List<String>> {

  val props = args
      .asList()
      .flatMap { it.split("\\s+".toRegex()) }
      .filter { it.startsWith("--") }
      .filter { it.contains("=") }
      .map { it.split("=") }
      .map { ((it[0] to it[1])) }
      .map {
        val (key, value) = it
        val propertyName = key.substring(2)
        val propertyValues = value.split(",", ", ", ";", "; ")
        (propertyName to propertyValues)
      }
      .toMap()

  return mapOf(
      "zookeeperDir" to props.getOrDefault("zookeeperDir", listOf("zk")),
      "zookeeperPort" to props.getOrDefault("zookeeperPort", listOf("2181")),
      "kafkaPort" to props.getOrDefault("kafkaPort", listOf("9092")),
      "kafkaTopics" to props.getOrDefault("kafkaTopics", listOf("test")),
      "httpPort" to props.getOrDefault("httpPort", listOf(defaultHttpPort)),
      "httpContext" to props.getOrDefault("httpContext", listOf(defaultHttpContext))
  )
}

/**
 * Prints application help if it was request by ony of these args:
 * --help
 * /?
 * -h
 */
fun printHelpOrContinue(args: Array<String>) {

  val userInput = args.toList()
  val noHelpRequested = listOf("-h", "--help", "/?")
      .none { userInput.contains(it) }

  if (noHelpRequested) return
  log.info("""

    support next arguments:

      --zookeeperPort=$defaultZkPort
      --zookeeperDir=./zk
      --kafkaPort=$defaultKafkaPort
      --kafkaTopics=topic1,topic2
      --httpPort=$defaultHttpPort
      --httpContext=$defaultHttpContext
      """.trimIndent())
  System.exit(0)
}

/**
 * java -jar embedded-kafka-0.0.3-all.jar \
 *              --zookeeperPort=8080
 *              --zookeeperDir=./zk
 *              --kafkaPort=9092
 *              --kafkaTopics=topic1,topic2
 *              --httpPort=8080
 *              --httpContext=/
 */
class App {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {

      BasicConfigurator.configure()
      printHelpOrContinue(args)

      val config = parseConfigProps(args)
      startZk(config)
      startKafka(config)
      createKafkaTopics(config)
      startHttpServer(config)
    }
  }
}
