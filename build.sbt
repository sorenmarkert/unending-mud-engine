
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "3.4.0"

val akkaActorVersion = "2.8.5"
val ravenDbVersion = "6.0.1"

lazy val engine = project
    .in(file("engine"))
    .settings(
        name := "Unending Engine",

        libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-actor-typed" % akkaActorVersion,
            "ch.qos.logback" % "logback-classic" % "1.5.3",
            "org.mongodb.scala" %% "mongo-scala-driver" % "5.0.0" cross CrossVersion.for3Use2_13,
            "net.ravendb" % "ravendb" % ravenDbVersion,
            "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0",
            "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",
            "org.eclipse.jetty.websocket" % "jetty-websocket-jetty-api" % "12.0.7",
            "org.eclipse.jetty.websocket" % "jetty-websocket-jetty-server" % "12.0.7",


            "net.ravendb" % "ravendb-test-driver" % ravenDbVersion % Test,
            "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % Test,
            "org.scalatest" %% "scalatest" % "3.2.18" % Test,
            "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % Test,
            "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaActorVersion,
        ),

        coverageEnabled := true,
        coverageFailOnMinimum := false,
        coverageMinimumStmtTotal := 90,
        coverageMinimumBranchTotal := 90,
        coverageMinimumStmtPerPackage := 90,
        coverageMinimumBranchPerPackage := 85,
        coverageMinimumStmtPerFile := 85,
        coverageMinimumBranchPerFile := 80,

        Compile / resourceGenerators += Def.task {
            val source = (webapp / Compile / scalaJSLinkedFile).value.data
            val dest = (Compile / resourceManaged).value / "assets" / "main.js"
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

ThisBuild / scalacOptions ++= Seq(
    "-Ykind-projector:underscores",
    "-Wconf:msg=Alphanumeric method .* is not declared infix:s"
)
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports")
