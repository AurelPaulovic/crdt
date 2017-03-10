crossScalaVersions := Seq("2.12.1", "2.11.8")

lazy val mainProject = Project(id = "crdt", base = file(".")).
  settings(
    name := "crdt",
    organization := "com.aurelpaulovic",
    version := "0.2-SNAPSHOT",
    scalaVersion := "2.12.1",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.scalatest" %% "scalatest" % "3.0.1" % "test",
        "org.scalamock" %% "scalamock-scalatest-support" % "3.4.2" % "test"
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

