lazy val commonFunctions = project
  .copy(id = "common-functions")
  .in(file("."))

name := "common-functions"

libraryDependencies ++= Vector(
  Library.scalaTest % "test"
)
