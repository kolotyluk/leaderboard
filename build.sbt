// NOTE: Check project/build.properties to make sure we are using the correct version of SBT

// import sbtunidoc.Plugin.unidocSettings

name := "leaderboard"

version := "0.0.0"

scalaVersion := "2.12.5"

lazy val akkaVersion = "2.5.18"
lazy val akkaHttpVersion = "10.1.5"

resolvers ++= Seq(
  "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
)

// https://stackoverflow.com/questions/41564915/why-could-not-find-implicit-error-in-scala-intellij-scalatest-scalactic

libraryDependencies ++= Seq(
  "ch.qos.logback"                %   "logback-classic"     % "1.2.3",
  "com.github.swagger-akka-http"  %%  "swagger-akka-http"   % "0.14.0" % "provided" exclude("org.slf4j", "slf4j-api"),
  "com.typesafe.akka"             %%  "akka-actor"          % akkaVersion,
  "com.typesafe.akka"             %%  "akka-actor-typed"    % akkaVersion,
  "com.typesafe.akka"             %%  "akka-http"           % akkaHttpVersion,
  "com.typesafe.akka"             %%  "akka-stream"         % akkaVersion,
  "com.typesafe.akka"             %%  "akka-testkit"        % akkaVersion,
  "com.typesafe"                  %   "config"              % "1.3.3"  % "provided" exclude("org.slf4j", "slf4j-api"),
  "io.swagger"                    %   "swagger-jaxrs"       % "1.5.18" % "provided" exclude("org.slf4j", "slf4j-api"),
  "org.clapper"                   %   "grizzled-slf4j_2.12" % "1.3.2"  % "provided" exclude("org.slf4j", "slf4j-api"),
  // "org.scalactic"                 %% "scalactic"            % "3.0.5",
  "org.scalatest"                 % "scalatest_2.12"        % "3.0.1"  % "test"
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
