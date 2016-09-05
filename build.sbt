lazy val commonFunctions = project
  .copy(id = "common-functions")
  .in(file("."))

name := "common-functions"
scalaVersion := Version.Scala

libraryDependencies ++= Vector(
  Library.scalaTest % "test",
  Library.akkaActor,
  Library.akkaStreams,
  Library.akkaTestkit,
  Library.akkaStreamsTestkit
)
