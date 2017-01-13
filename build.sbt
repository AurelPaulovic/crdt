
lazy val mainProject = Project(id = "crdt", base = file(".")).
  settings(
    name := "crdt",
    organization := "com.aurelpaulovic",
    version := "0.2-SNAPSHOT",
    scalaVersion := "2.11.5",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.5",
      "junit" % "junit" % "4.8.1" % "test",
      "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
    ),
    publishMavenStyle := false,
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-feature",
      "-deprecation",
      "-language:implicitConversions",
      "-language:existentials",
      "-language:higherKinds",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-explaintypes"
    )
  )
