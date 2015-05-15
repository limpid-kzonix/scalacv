lazy val commonSettings = Seq(
  organization := "it.callisto",
  version := "0.1.0",
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "scalacv"
  )