import Dependencies._

ThisBuild / scalaVersion := "2.13.3"

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin) // generate binary using Ash
  .settings(
    name := "fp-cart",
    packageName in Docker := "shopping-cart",
    dockerExposedPorts ++= Seq(8080),
    dockerUpdateLatest := true,
    dockerBaseImage := "openjdk:8u201-jre-alpine3.9", // use only jre to reduce size of docker img,
    makeBatScripts := Seq(), // do not create scripts for windows
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      compilerPlugin(Libraries.kindProjector cross CrossVersion.full),
      compilerPlugin(Libraries.betterMonadicFor),
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.catsMeowMtl,
      Libraries.catsRetry,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeParser,
      Libraries.circeRefined,
      Libraries.cirisCore,
      Libraries.cirisEnum,
      Libraries.cirisRefined,
      Libraries.fs2,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sClient,
      Libraries.http4sCirce,
      Libraries.http4sJwtAuth,
      Libraries.javaxCrypto,
      Libraries.log4cats,
      Libraries.logback % Runtime,
      Libraries.newtype,
      Libraries.redis4catsEffects,
      Libraries.redis4catsLog4cats,
      Libraries.refinedCore,
      Libraries.refinedCats,
      Libraries.skunkCore,
      Libraries.skunkCirce,
      Libraries.squants,
      // Tests
      Libraries.scalaCheck,
      Libraries.scalaTest,
      Libraries.scalaTestPlus
    )
  )
