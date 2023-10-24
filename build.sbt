ThisBuild / scalaVersion := "2.13.8"

name := "HarmonyState"
version := "1.0"

lazy val data_models = (project in file("data_models"))
  .settings(
    name := "data",
    libraryDependencies ++= commonDependencies ++ Seq(
    )
  )

lazy val harmonywatcher_simulation = (project in file("harmonywatcher_simulation"))
  .settings(
    name := "harmonywatcher_simulation",
    libraryDependencies ++= commonDependencies ++ Seq(
      dependencies.kafkaClients,
      dependencies.log4jAPI
    )
  ).dependsOn(
  data_models
)

lazy val trigger_alert = (project in file("trigger_alert"))
  .settings(
    name := "trigger_alert",
    libraryDependencies ++= commonDependencies ++ Seq(
      dependencies.kafkaClients,
      dependencies.sparkCore,
      dependencies.sparkSQL,
      dependencies.sparkStreamKafka,
      dependencies.sparkStream
    )
  )
  .dependsOn(
    data_models
  )

lazy val handle_alerts = (project in file("handle_alerts"))
  .settings(
    name := "handle_alerts",

    libraryDependencies ++= commonDependencies ++ Seq(
      dependencies.kafkaClients,
      dependencies.sparkCore,
      dependencies.sparkSQL,
      dependencies.sparkStreamKafka,
      dependencies.sparkStream,
      "com.typesafe.akka" %% "akka-http" % "10.5.0",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.0",
      "ch.megard" %% "akka-http-cors" % "1.2.0",
      "org.apache.kafka" % "kafka-clients" % "3.4.0",
      "org.apache.kafka" %% "kafka" % "3.4.0",
      "com.typesafe.play" %% "play-json" % "2.9.4"
    ),

  )
  .dependsOn(
    data_models
  )

lazy val storage = (project in file("storage"))
  .settings(
    name := "storage",
    libraryDependencies ++= commonDependencies ++ Seq(
      dependencies.kafkaClients,
      dependencies.sparkCore,
      dependencies.sparkSQL,
      dependencies.sparkStreamKafka,
      dependencies.sparkStream,
      dependencies.hadoopClient,
      dependencies.hadoopCommon,
      dependencies.hadoopMapReduce,
      dependencies.hadoopAWS
    )
  )
  .dependsOn(
    data_models
  )

lazy val analytics = (project in file("analytics"))
  .settings(
    name := "analytics",
    libraryDependencies ++= commonDependencies ++ Seq(
      dependencies.sparkCore,
      dependencies.sparkSQL,
      dependencies.hadoopCommon,
      dependencies.hadoopMapReduce,
      dependencies.hadoopAWS,
      dependencies.hadoopClient,
      "com.typesafe.akka" %% "akka-actor" % "2.8.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.0",
      "com.typesafe.akka" %% "akka-http" % "10.5.0",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      "ch.megard" %% "akka-http-cors" % "1.2.0",
      "org.apache.kafka" % "kafka-clients" % "3.4.0",
      "org.apache.kafka" %% "kafka" % "3.4.0",
      "com.typesafe.play" %% "play-json" % "2.9.4"

    )
  )
  .dependsOn(
    data_models
  )

lazy val dependencies =
  new {
    val pJson = "com.typesafe.play" %% "play-json" % "2.9.2"
    val log4jAPI = "org.apache.logging.log4j" % "log4j-1.2-api" % "2.17.2"
    val log4jCore = "org.apache.logging.log4j" % "log4j-core" % "2.17.2"
    val log4jslf = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.0"
    val kafkaClients = "org.apache.kafka" % "kafka-clients" % "3.4.0"
    val sparkCore = "org.apache.spark" %% "spark-core" % "3.3.2"
    val sparkSQL = "org.apache.spark" %% "spark-sql" % "3.3.2"
    val hadoopCommon = "org.apache.hadoop" % "hadoop-common" % "3.3.2"
    val hadoopMapReduce = "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.3.2"
    val hadoopAWS = "org.apache.hadoop" % "hadoop-aws" % "3.3.2"
    val hadoopClient = "org.apache.hadoop" % "hadoop-client" % "3.3.2"
    val sparkStreamKafka = "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.3.2"
    val sparkStream = "org.apache.spark" %% "spark-streaming" % "3.3.2"
  }

lazy val commonDependencies = Seq(
  dependencies.pJson
)