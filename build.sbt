

name := "unending"

version := "1.0" 

lazy val `unending` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.13.5"

scalacOptions += "-Ywarn-value-discard"

unmanagedSourceDirectories in Compile += baseDirectory.value / "src"
unmanagedResourceDirectories in Test += baseDirectory ( _ /"target/web/public/test" ).value

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.5" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.10" % Test