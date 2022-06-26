// *****************************************************************************
// Build settings
// *****************************************************************************

inThisBuild(
  Seq(
    organization     := "bholten",
    organizationName := "Brennan Holten",
    startYear        := Some(2022),
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    scalaVersion := "2.13.8",
    testFrameworks += new TestFramework("munit.Framework"),
    scalafmtOnCompile := true,
    dynverSeparator   := "_", // the default `+` is not compatible with docker tags
  )
)

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akka-streams-commons` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .aggregate(`akka-streams-components`)

lazy val `akka-streams-components` =
  project
    .in(file("modules/akka-streams-components"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaStream,
        library.akkaStreamTestkit % Test,
        library.munit             % Test,
        library.munitScalaCheck   % Test,
      ),
    )

// *****************************************************************************
// Project settings
// *****************************************************************************

lazy val commonSettings =
  Seq(
    // Also (automatically) format build definition together with sources
    Compile / scalafmt := {
      val _ = (Compile / scalafmtSbt).value
      (Compile / scalafmt).value
    },
  )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka  = "2.6.19"
      val munit = "0.7.29"
    }
    val akkaStream        = "com.typesafe.akka" %% "akka-stream"         % Version.akka
    val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
    val munit             = "org.scalameta"     %% "munit"               % Version.munit
    val munitScalaCheck   = "org.scalameta"     %% "munit-scalacheck"    % Version.munit
  }
