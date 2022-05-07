
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "3.1.2"

val akkaActorVersion = "2.6.19"
val akkaHttpVersion  = "10.2.9"

lazy val webpage = project
    .in(file("web-app"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
        name := "Unending Web App",
        scalaJSUseMainModuleInitializer := true,
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % "2.1.0",
            ),
        )
    .dependsOn(models.js)

lazy val engine = project
    .in(file("engine"))
    .dependsOn(models.jvm)
    .settings(
        name := "Unending Engine",

        libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-http" % akkaHttpVersion cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-stream" % akkaActorVersion cross CrossVersion.for3Use2_13,
            "ch.qos.logback" % "logback-classic" % "1.2.11",
            "org.scalatest" %% "scalatest" % "3.2.12" % Test,
            "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaActorVersion % Test cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion cross CrossVersion.for3Use2_13,
            ),
        Test / unmanagedResourceDirectories += baseDirectory(_ / "target/web/public/test").value,
        )

lazy val models = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("dto"))
    .settings(
        name := "Unending Data Models",
        )