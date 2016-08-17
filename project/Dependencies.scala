import sbt._

object Version {
  final val Scala     = "2.11.8"
  final val ScalaTest = "3.0.0-RC4"
  final val Akka = "2.4.8"
}

object Library {
  val scalaTest  = "org.scalatest"     %% "scalatest"   % Version.ScalaTest
  val akka_actor  = "com.typesafe.akka" %% "akka-actor" % Version.Akka
  val akka_streams  = "com.typesafe.akka" % "akka-stream_2.11" % Version.Akka	
  val akka_testkit = "com.typesafe.akka" %% "akka-testkit" % Version.Akka % "test"
  val akka_streams_testkit  = "com.typesafe.akka" % "akka-stream-testkit_2.11" % Version.Akka % "test"
}
