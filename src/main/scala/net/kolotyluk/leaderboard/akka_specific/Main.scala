package net.kolotyluk.leaderboard.akka_specific

import akka.actor.typed.{ActorSystem, Terminated}
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.akka_specific.GuardianActor.{Bind, Shutdown}
import net.kolotyluk.leaderboard.protobuf.ProtocolBufferServer
import net.kolotyluk.scala.extras.{Environment, Logging}

import scala.util.{Failure, Success}


/** =Leaderboard Micro Service - Main Entry Point=
  *
  * ==Startup==
  *
  * Akka System Startup - set up our operating context with configuration, environment, and logging before
  * starting the actor system.
  *
  * ==Shutdown==
  *
  * While all systems should be designed to be robust when things spontaneously fail, with HTTP APIs, it's nice to
  * make provisions for an orderly shutdown so that any outstanding HTTP Requests can complete normally.
  * <p>
  * Note: normal shutdown will not happen if you press the IntelliJ Stop Button. Instead, you need to use the `Exit`
  * button in the Run panel. This will only work when Running, and not when Debugging.
  *
  * ===Shutdown Hook===
  *
  * By registering a Shutdown Hook, an application can perform any necessary housekeeping process before the JVM
  * shuts down. A Shutdown Hook is a Java Thread, and when all such threads complete, the JVM will finally complete
  * the shutdown process.
  * <p>
  * By using the Shutdown Hook, this service can make best effort to shutdown gracefully, minimizing loss of data
  * or operations. Note, because the hook runs at a rather low lever, we utilize fairly primitive mechanisms for
  * the Akka System to notify the Shutdown Hook when it's done.
  * <p>
  * Inside the JVM. the easiest way to shutdown the service is
  * {{{
  * System.exit(0)
  * }}}
  *
  * ===Unix Shutdown===
  *
  * On Unix like systems: Linux, OS X, Posix, you can stop force the JVM to invoke the Shutdown Hook using
  * {{{
  * kill -1 pid
  * }}}
  * where pid is the process identifier reported at the beginning of the system log. For example
  * {{{
  * /Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home/bin/java -Didea.launcher.port=56079 "-Didea.launcher.bin.path=/Applications/IntelliJ IDEA CE.app/Contents/bin" -Dfile.encoding=UTF-8 -classpath "/Volumes/Repos/leaderboard/target/classes:/Users/eric/.sbt/boot/scala-2.12.4/lib/scala-library.jar:/Users/eric/.m2/repository/ch/qos/logback/logback-classic/1.2.3/logback-classic-1.2.3.jar:/Users/eric/.m2/repository/ch/qos/logback/logback-core/1.2.3/logback-core-1.2.3.jar:/Users/eric/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/Users/eric/.m2/repository/com/github/swagger-akka-http/swagger-akka-http_2.12/2.0.0/swagger-akka-http_2.12-2.0.0.jar:/Users/eric/.m2/repository/org/scala-lang/modules/scala-java8-compat_2.12/0.8.0/scala-java8-compat_2.12-0.8.0.jar:/Users/eric/.m2/repository/com/github/swagger-akka-http/swagger-scala-module_2.12/2.0.2/swagger-scala-module_2.12-2.0.2.jar:/Users/eric/.m2/repository/org/scala-lang/scala-reflect/2.12.7/scala-reflect-2.12.7.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.9.7/jackson-dataformat-yaml-2.9.7.jar:/Users/eric/.m2/repository/org/yaml/snakeyaml/1.23/snakeyaml-1.23.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.7/jackson-core-2.9.7.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/module/jackson-module-scala_2.12/2.9.7/jackson-module-scala_2.12-2.9.7.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.9.7/jackson-annotations-2.9.7.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.9.7/jackson-databind-2.9.7.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/module/jackson-module-paranamer/2.9.7/jackson-module-paranamer-2.9.7.jar:/Users/eric/.m2/repository/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-actor_2.12/2.5.19/akka-actor_2.12-2.5.19.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-actor-typed_2.12/2.5.19/akka-actor-typed_2.12-2.5.19.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-http_2.12/10.1.7/akka-http_2.12-10.1.7.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-http-core_2.12/10.1.7/akka-http-core_2.12-10.1.7.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-parsing_2.12/10.1.7/akka-parsing_2.12-10.1.7.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-http-spray-json_2.12/10.1.7/akka-http-spray-json_2.12-10.1.7.jar:/Users/eric/.m2/repository/io/spray/spray-json_2.12/1.3.5/spray-json_2.12-1.3.5.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-http-testkit_2.12/10.1.7/akka-http-testkit_2.12-10.1.7.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-slf4j_2.12/2.5.18/akka-slf4j_2.12-2.5.18.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-stream_2.12/2.5.19/akka-stream_2.12-2.5.19.jar:/Users/eric/.m2/repository/com/typesafe/akka/akka-protobuf_2.12/2.5.19/akka-protobuf_2.12-2.5.19.jar:/Users/eric/.m2/repository/org/reactivestreams/reactive-streams/1.0.2/reactive-streams-1.0.2.jar:/Users/eric/.m2/repository/com/typesafe/ssl-config-core_2.12/0.3.6/ssl-config-core_2.12-0.3.6.jar:/Users/eric/.m2/repository/org/scala-lang/modules/scala-parser-combinators_2.12/1.1.1/scala-parser-combinators_2.12-1.1.1.jar:/Users/eric/.m2/repository/com/typesafe/config/1.3.3/config-1.3.3.jar:/Users/eric/.m2/repository/commons-codec/commons-codec/1.11/commons-codec-1.11.jar:/Users/eric/.m2/repository/io/swagger/swagger-jaxrs/1.5.18/swagger-jaxrs-1.5.18.jar:/Users/eric/.m2/repository/io/swagger/swagger-core/1.5.18/swagger-core-1.5.18.jar:/Users/eric/.m2/repository/io/swagger/swagger-models/1.5.18/swagger-models-1.5.18.jar:/Users/eric/.m2/repository/io/swagger/swagger-annotations/1.5.18/swagger-annotations-1.5.18.jar:/Users/eric/.m2/repository/javax/ws/rs/jsr311-api/1.1.1/jsr311-api-1.1.1.jar:/Users/eric/.m2/repository/org/reflections/reflections/0.9.11/reflections-0.9.11.jar:/Users/eric/.m2/repository/com/google/guava/guava/20.0/guava-20.0.jar:/Users/eric/.m2/repository/io/swagger/core/v3/swagger-annotations/2.0.5/swagger-annotations-2.0.5.jar:/Users/eric/.m2/repository/io/swagger/core/v3/swagger-core/2.0.5/swagger-core-2.0.5.jar:/Users/eric/.m2/repository/javax/xml/bind/jaxb-api/2.3.0/jaxb-api-2.3.0.jar:/Users/eric/.m2/repository/org/apache/commons/commons-lang3/3.7/commons-lang3-3.7.jar:/Users/eric/.m2/repository/javax/validation/validation-api/1.1.0.Final/validation-api-1.1.0.Final.jar:/Users/eric/.m2/repository/io/swagger/core/v3/swagger-jaxrs2/2.0.5/swagger-jaxrs2-2.0.5.jar:/Users/eric/.m2/repository/org/javassist/javassist/3.22.0-GA/javassist-3.22.0-GA.jar:/Users/eric/.m2/repository/io/swagger/core/v3/swagger-integration/2.0.5/swagger-integration-2.0.5.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/jaxrs/jackson-jaxrs-json-provider/2.9.5/jackson-jaxrs-json-provider-2.9.5.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/jaxrs/jackson-jaxrs-base/2.9.5/jackson-jaxrs-base-2.9.5.jar:/Users/eric/.m2/repository/com/fasterxml/jackson/module/jackson-module-jaxb-annotations/2.9.5/jackson-module-jaxb-annotations-2.9.5.jar:/Users/eric/.m2/repository/io/swagger/core/v3/swagger-models/2.0.5/swagger-models-2.0.5.jar:/Users/eric/.m2/repository/org/clapper/grizzled-slf4j_2.12/1.3.2/grizzled-slf4j_2.12-1.3.2.jar:/Users/eric/.m2/repository/org/json4s/json4s-native_2.12/3.6.2/json4s-native_2.12-3.6.2.jar:/Users/eric/.m2/repository/org/json4s/json4s-core_2.12/3.6.2/json4s-core_2.12-3.6.2.jar:/Users/eric/.m2/repository/org/json4s/json4s-ast_2.12/3.6.2/json4s-ast_2.12-3.6.2.jar:/Users/eric/.m2/repository/org/json4s/json4s-scalap_2.12/3.6.2/json4s-scalap_2.12-3.6.2.jar:/Users/eric/.m2/repository/org/scala-lang/scala-library/2.12.7/scala-library-2.12.7.jar:/Users/eric/.m2/repository/net/kolotyluk/java/java-file-utilities/0.0.3/java-file-utilities-0.0.3.jar:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar" com.intellij.rt.execution.application.AppMainV2 net.kolotyluk.leaderboard.Main
  * Process 41170, starting net.kolotyluk.leaderboard.Main$...
  * . . .
  * kill -15 41170
  * . . .
  * Process 41170 terminating
  * [WARN ] [2019-03-05 15:55:42,700] [leaderboard-akka.actor.default-dispatcher-2] [n.k.l.akka_specific.GuardianActor] Shutting down because: Shutdown Hook Called
  * [INFO ] [2019-03-05 15:55:42,705] [leaderboard-akka.actor.default-dispatcher-3] [n.k.l.akka_specific.RestActor] received Unbind(10 seconds,Actor[akka://leaderboard/temp/$a#0])
  * [INFO ] [2019-03-05 15:55:42,706] [leaderboard-akka.actor.default-dispatcher-3] [n.k.l.akka_specific.RestActor] Received request to unbind. Starting Graceful Termination of HTTP API.
  * [WARN ] [2019-03-05 15:55:42,707] [leaderboard-akka.actor.default-dispatcher-3] [n.k.l.akka_specific.RestActor] Unbinding from 0:0:0:0:0:0:0:0:8080 with deadline = 10 seconds
  * [INFO ] [2019-03-05 15:55:42,717] [leaderboard-akka.actor.default-dispatcher-2] [n.k.l.akka_specific.GuardianActor] REST API successfully unbound after completing all requests.
  * [WARN ] [2019-03-05 15:55:42,731] [leaderboard-akka.actor.default-dispatcher-5] [n.k.l.a.LeaderboardManagerActor] received signal with event = PostStop with actorContext = akka.actor.typed.internal.adapter.ActorContextAdapter@3a045e74
  * [WARN ] [2019-03-05 15:55:42,732] [leaderboard-akka.actor.default-dispatcher-5] [n.k.l.a.LeaderboardManagerActor] unknown event = PostStop, continuing...
  * [WARN ] [2019-03-05 15:55:42,733] [leaderboard-akka.actor.default-dispatcher-4] [n.k.l.akka_specific.GuardianActor] PostStop signal received
  * [INFO ] [2019-03-05 15:55:42,754] [leaderboard-akka.actor.default-dispatcher-7] [n.k.l.akka_specific.GuardianActor] terminated = Terminated(Actor[akka://leaderboard/])
  * [INFO ] [2019-03-05 15:55:42,755] [leaderboard-akka.actor.default-dispatcher-4] [net.kolotyluk.leaderboard.Main$] Actor System Terminated Normally
  * [INFO ] [2019-03-05 15:55:42,755] [leaderboard-akka.actor.default-dispatcher-7] [n.k.l.akka_specific.GuardianActor] Notifying Shutdown Hook
  * [INFO ] [2019-03-05 15:55:42,756] [Thread-2] [net.kolotyluk.leaderboard.Main$] Successful Shutdown
  *
  * Process finished with exit code 143 (interrupted by signal 15: SIGTERM)
  * }}}
  *
  * ===Windows Shutdown===
  *
  * It's not clear if there is any nice way to shutdown the JVM on windows.
  *
  * @author eric@kolotyluk.net
  * @see [[https://doc.akka.io/docs/akka/current Akka]]
  * @see [[https://doc.akka.io/docs/akka/current/typed/index.html#akka-typed Akka Typed]]
  * @see [[http://stackoverflow.com/questions/4727536/how-do-i-stop-a-processing-running-in-intellij-such-that-it-calls-the-shutdown-h ItelliJ Exit Button]]
  * @see [[https://docs.oracle.com/javase/8/docs/technotes/guides/lang/hook-design.html Design of the Shutdown Hooks API]]
  * @see [[https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runtime.html#addShutdownHook(java.lang.Thread) addShutdownHook]]
  */
object Main
  extends App
    with Configuration
    with Environment
    with Logging {

  val pid = ProcessHandle.current.pid

  // Safest way to indicate something is happening, don't rely on logging yet
  println(s"Process $pid, starting ${getClass.getName}...")
  println("Reporting environment and configuration for troubleshooting purposes. Don't disable this.")

  println(environment.getEnvironmentReport())
  println(config.getConfigurationReport())

  logger.info("Logging started")

  // Start the Akka actor system, with the top level guardian actor, using its default behavior
  // Note: we stash it as an option so we can delete it when Akka terminates, so that the ShutdownHook
  // does not try to shut it down.
  var systemOption: Option[ActorSystem[GuardianActor.Message]] =
    Some(ActorSystem(guardianActor.behavior, config.getAkkaSystemName()))

  systemOption.foreach{system =>
    logger.info(s"Akka Actor System Started")

    system ! Bind() // to our HTTP REST endpoint

    // TODO name this better and add to shutdown hook
    val server = new ProtocolBufferServer(system.executionContext)
    server.start()

    system.whenTerminated.onComplete {
      case Success(Terminated(actorRef)) =>
        systemOption = None
        // println(s"Actor System Terminated Normally")
        logger.info(s"Actor System Terminated Normally")
      case Failure(cause) =>
        systemOption = None
        // println(s"Actor System Termination Failure", cause)
        logger.error(s"Actor System Termination Failure", cause)
    } (system.executionContext)
  }

  Runtime.getRuntime.addShutdownHook(new Thread("Shutdown Hook") {
    override def run() = {
      println(s"Java process $pid shutting down")
      systemOption.foreach{system =>
        val notifier = new Object

        system ! Shutdown("Shutdown Hook Called", notifier)

        // Block this thread until Akka notifies us it is done,
        // and then the JVM will finish shutting down when we exit
        // the Shutdown Hook thread
        notifier.synchronized {
          try {
            notifier.wait(10000)
            logger.info("Shutdown complete")
          } catch {
            case cause: Throwable =>
              logger.error("Shutdown complete", cause)
          }
        }
      }
    }
  })

  // We're done on this thread. the Akka system is running the show now
  logger.info("Exiting this thread now...")
}
