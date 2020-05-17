name := "akka-http-bookstore"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.4"
val akkaHttpVersion = "10.1.11"

Test / parallelExecution := false

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
  "org.postgresql" % "postgresql" % "42.2.12",
  "org.flywaydb" % "flyway-core" % "6.3.3",
  "com.lihaoyi" %% "scalatags" % "0.8.2",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.1",
  "com.pauldijou" %% "jwt-core" % "4.2.0",
)
