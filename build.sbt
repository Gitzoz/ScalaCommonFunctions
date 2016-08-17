lazy val commonFunctions = project
  .copy(id = "common-functions")
  .in(file("."))

name := "common-functions"
scalaVersion := "2.11.8"

libraryDependencies ++= Vector(
  Library.scalaTest % "test",
  Library.akka_actor,
  Library.akka_streams,
  Library.akka_testkit,
  Library.akka_streams_testkit
)
