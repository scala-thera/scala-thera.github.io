val ScalaVer = "2.12.6"

// val Cats          = "1.2.0"
val CatsEffect    = "1.0.0-RC2-3433449"
val KindProjector = "0.9.7"
val Circe         = "0.9.3"
val BetterFiles   = "3.6.0"

lazy val commonSettings = Seq(
  name    := "effectextensions"
, version := "0.1.0"
, scalaVersion := ScalaVer
, libraryDependencies ++= Seq(
    // "org.typelevel"  %% "cats"        % Cats
    "org.typelevel"  %% "cats-effect" % CatsEffect

  , "io.circe" %% "circe-core"    % Circe
  , "io.circe" %% "circe-generic" % Circe
  , "io.circe" %% "circe-parser"  % Circe
  
  , "com.github.pathikrit" %% "better-files" % BetterFiles)

, addCompilerPlugin("org.spire-math" %% "kind-projector" % KindProjector)
, scalacOptions ++= Seq(
      "-deprecation"
    , "-encoding", "UTF-8"
    , "-feature"
    , "-language:existentials"
    , "-language:higherKinds"
    , "-language:implicitConversions"
    , "-language:experimental.macros"
    , "-unchecked"
    // , "-Xfatal-warnings"
    // , "-Xlint"
    // , "-Yinline-warnings"
    , "-Ywarn-dead-code"
    , "-Xfuture"
    , "-Ypartial-unification")
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    initialCommands := "import playground._, Main._"
  )
