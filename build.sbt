lazy val ScalaCommonFunctions = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)

libraryDependencies ++= Vector(
  Library.scalaTest % "test",
  Library.akkaActor,
  Library.akkaStreams,
  Library.akkaTestkit % "test",
  Library.akkaStreamsTestkit % "test"
)

initialCommands := """|import de.gitzoz.scalacommonfunctions._
                      |""".stripMargin
