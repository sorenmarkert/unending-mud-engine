
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "3.2.2"

val akkaActorVersion = "2.8.0"
val akkaHttpVersion  = "10.5.0"
val ravenDbVersion   = "5.4.1"

lazy val engine = project
    .in(file("engine"))
    .settings(
        name := "Unending Engine",

        libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-http" % akkaHttpVersion cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-stream" % akkaActorVersion cross CrossVersion.for3Use2_13,
            "ch.qos.logback" % "logback-classic" % "1.4.6",
            "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0" cross CrossVersion.for3Use2_13,
            "net.ravendb" % "ravendb" % ravenDbVersion,
            "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2",
            "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",

            "net.ravendb" % "ravendb-test-driver" % ravenDbVersion % Test,
            "org.scalatest" %% "scalatest" % "3.2.15" % Test,
            "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaActorVersion % Test cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test cross CrossVersion.for3Use2_13,
            "com.typesafe.akka" %% "akka-stream-testkit" % akkaActorVersion % Test cross CrossVersion.for3Use2_13,
            ),

        coverageEnabled := true,
        coverageFailOnMinimum := true,
        coverageMinimumStmtTotal := 90,
        coverageMinimumBranchTotal := 90,
        coverageMinimumStmtPerPackage := 90,
        coverageMinimumBranchPerPackage := 85,
        coverageMinimumStmtPerFile := 85,
        coverageMinimumBranchPerFile := 80,

        Compile / resourceGenerators += Def.task {
            val source = (webapp / Compile / scalaJSLinkedFile).value.data
            val dest   = (Compile / resourceManaged).value / "assets" / "main.js"
            IO.copy(Seq(source -> dest))
            Seq(dest)
        },
        run / fork := true
        )
    .dependsOn(models.jvm)

lazy val webapp = project
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

lazy val models = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("dto"))
    .settings(
        name := "Unending Data Models",
        )
