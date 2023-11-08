ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "sample spark project"
  )

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.4.1",
  "org.apache.spark" %% "spark-graphx" % "3.4.1",
  "org.apache.spark" %% "spark-sql" % "3.4.1",
  "org.yaml" % "snakeyaml" % "1.29",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
  "software.amazon.awssdk" % "s3" % "2.16.83",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "log4j" % "log4j" % "1.2.17",
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.0",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.14.0",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.0"
)
compileOrder := CompileOrder.JavaThenScala
test / fork := true
run / fork := true
run / javaOptions ++= Seq(
  "-Xms8G",
  "-Xmx100G",
  "-XX:+UseG1GC"
)

Compile / mainClass := Some("Main")
run / mainClass := Some("Main")

val jarName = "graphs_mitm_attack.jar"
assembly / assemblyJarName := jarName

assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("com.fasterxml.jackson.**" -> "shaded.jackson.@1").inAll
)

// Merging strategies
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}