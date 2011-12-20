organization := "com.osinka.subset"

name := "subset"

version := "0.3.0-SNAPSHOT"

scalaVersion := "2.8.2"

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "2.7.2",
  "joda-time" % "joda-time" % "1.6.2" % "optional",
  "org.scalatest" %% "scalatest" % "1.5.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
