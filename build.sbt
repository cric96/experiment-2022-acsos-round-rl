ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "experiment-discoli-ac-round-rl"
  )

libraryDependencies += "io.github.pityka" %% "nspl-awt" % "0.3.0"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.0"
libraryDependencies += "com.outr" %% "scribe" % "3.8.0"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"
libraryDependencies += "com.lihaoyi" %% "upickle" % "1.4.3"
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
libraryDependencies += "com.sksamuel.scrimage" % "scrimage-core" % "4.0.31"
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-scala" % "4.0.31"

addCommandAlias("startGradient", "runMain it.unibo.casestudy.launch.Main configurations/config.json")
addCommandAlias("startGradientMulti", "runMain it.unibo.casestudy.launch.Main configurations/config-multi.json")
addCommandAlias("startBlockC", "runMain it.unibo.casestudy.launch.Main configurations/config-c.json")
addCommandAlias("startBlockLocal", "runMain it.unibo.casestudy.launch.Main configurations/base-config.json")
addCommandAlias("startCheckScale", "runMain it.unibo.casestudy.launch.CheckScale")
// Use with sbt "renderResult file/runtime.json <program> <scenario> <snapshot dt>
addCommandAlias("renderResult", "runMain it.unibo.casestudy.launch.VideoOutput")
// Use with sbt "generatePlots <sample> <training count> <max accepted error> <max accepted ticks>
addCommandAlias("generatePlots", "runMain it.unibo.casestudy.launch.Analysis")
