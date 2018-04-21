name := "leaderboard"

version := "0.0.0"

scalaVersion := "2.12.5"

lazy val akkaVersion = "2.5.11"

libraryDependencies ++= Seq(
  "ch.qos.logback"    %   "logback-classic"     % "1.2.3",
  "com.typesafe.akka" %%  "akka-actor"          % akkaVersion,
  "com.typesafe.akka" %%  "akka-actor-typed"    % akkaVersion,
  "com.typesafe.akka" %%  "akka-http"           % "10.1.1",
  "com.typesafe.akka" %%  "akka-stream"         % akkaVersion,
  "com.typesafe.akka" %%  "akka-testkit"        % akkaVersion,
  "com.typesafe"      %   "config"              % "1.3.3",
  "org.clapper"       %   "grizzled-slf4j_2.12" % "1.3.2",
  //"org.scalatest"     %% "scalatest"          % "3.2.0-SNAP10",
  "org.scalatest"     %%  "scalatest"           % "3.0.5" % "test"
  //"org.slf4j"   % "slf4j-parent"        % "1.7.25"
)
