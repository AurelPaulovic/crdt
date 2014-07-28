organization := "com.aurelpaulovic"

name := "crdt"

version := "0.1"

scalaVersion := "2.11.2"

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

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"
