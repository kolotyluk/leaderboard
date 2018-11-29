// NOTE: Check project/build.properties to make sure we are using the correct version of SBT

// import sbtunidoc.Plugin.unidocSettings

name := "leaderboard"

version := "0.0.0"

scalaVersion := "2.12.7"

lazy val akkaVersion = "2.5.18"
lazy val akkaHttpVersion = "10.1.5"
lazy val jacksonVersion = "2.9.7"
lazy val swaggerVersion = "2.0.5"

resolvers ++= Seq(
  "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
)

// https://stackoverflow.com/questions/41564915/why-could-not-find-implicit-error-in-scala-intellij-scalatest-scalactic

libraryDependencies ++= Seq(
  "ch.qos.logback"                    % "logback-classic"          % "1.2.3",
  "com.github.swagger-akka-http"      %% "swagger-akka-http"       % "0.14.0" % "provided" exclude("org.slf4j", "slf4j-api"),
  "com.github.swagger-akka-http"      %% "swagger-scala-module"    % "2.0.2",
  "com.fasterxml.jackson.module"      %% "jackson-module-scala"    % jacksonVersion,
  "com.fasterxml.jackson.dataformat"  %  "jackson-dataformat-yaml" % jacksonVersion,
  "com.typesafe.akka"                 %% "akka-actor"              % akkaVersion,
  "com.typesafe.akka"                 %% "akka-actor-typed"        % akkaVersion,
  "com.typesafe.akka"                 %% "akka-http"               % akkaHttpVersion,
  "com.typesafe.akka"                 %% "akka-http-spray-json"    % akkaHttpVersion,
  "com.typesafe.akka"                 %% "akka-stream"             % akkaVersion,
  "com.typesafe"                      %  "config"                  % "1.3.3"  % "provided" exclude("org.slf4j", "slf4j-api"),
  // "io.swagger"                    %   "swagger-jaxrs"       % "1.5.18" % "provided" exclude("org.slf4j", "slf4j-api"),
  "io.swagger.core.v3"                %  "swagger-core"            % swaggerVersion,
  "io.swagger.core.v3"                %  "swagger-annotations"     % swaggerVersion,
  "io.swagger.core.v3"                %  "swagger-models"          % swaggerVersion,
  "io.swagger.core.v3"                %  "swagger-jaxrs2"          % swaggerVersion,

  "org.clapper"                       %  "grizzled-slf4j_2.12"     % "1.3.2"  % "provided" exclude("org.slf4j", "slf4j-api"),
  "org.json4s"                        %% "json4s-native"           % "3.5.3",
  // "org.scalactic"                 %% "scalactic"            % "3.0.5",
  "com.typesafe.akka"                 %% "akka-testkit"            % akkaVersion % "test",
  "org.scalatest"                     %% "scalatest"               % "3.0.5"     % "test"
  // "org.pegdown"       %   "pegdown"             % "1.6.0"           % "test" // (For Html Scalatest reports)
  // "org.slf4j"   % "slf4j-simple"        % "1.7.25"
)

//fork in test := true
//
//javaOptions in test += "-ea"

//lazy val scalaTestRun = inputKey[Unit]("custom run task for test")
//scalaTestRun := {
//  val one = (runMain in Compile).fullInput(" org.scalatest.tools.Runner -s com.pg.macro.testcase.AutoTest -h ScalaTestReport").evaluated
//}

// parallelExecution in Test := false

// Get scaladoc to add rootdoc.txt content to index.html
scalacOptions in (Compile,doc) ++= Seq("-doc-root-content", "rootdoc.txt")
