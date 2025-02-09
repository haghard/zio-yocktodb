import sbt._

lazy val `yoctodb-schema` = (project in file(".")).settings(commonSettings)

lazy val scalac3Settings = Seq(
  scalacOptions ++= Seq(
    //"-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    //"-Xfatal-warnings",
    //"-Yexplicit-nulls",
    //"-Wunused",
    "-Ywarn-unused",  //sfix
    "-Ykind-projector",
    "-Ysafe-init", //guards against forward access reference
  ) ++ Seq("-rewrite", "-indent") ++ Seq("-source", "future")
)

lazy val commonSettings = scalac3Settings ++ Seq(
  name := "yoctodb-schema",
  organization := "haghard",
  version := "0.0.1-SNAPSHOT",
  startYear := Some(2021),

  Test / parallelExecution := false,
  run / fork := false,

  Compile / console / scalacOptions --= Seq("-Wunused:_", "-Xfatal-warnings"),

  //https://github.com/OOPMan/collectioneer/blob/master/build.sbt
  //https://github.com/com-lihaoyi/Ammonite/issues/1241
  /*Test / sourceGenerators += Def.task {
    val file = (Test / sourceManaged).value / "amm.scala"
    IO.write(file,
      """object amm {
        |   def main(args: Array[String]) = ammonite.Main.main(args)
        |}""".stripMargin
    )
    Seq(file)
  }.taskValue,*/

  //sbt headerCreate
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  scalaVersion := "3.1.1",
  headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
  headerLicense  := Some(HeaderLicense.Custom(
    """|Copyright (c) 2021-22 by Vadim Bondarev
       |This software is licensed under the Apache License, Version 2.0.
       |You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
       |""".stripMargin
  ))
)

resolvers ++= Seq(Resolver.jcenterRepo, "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/")

unmanagedBase := baseDirectory.value / "lib"

libraryDependencies ++= Seq(
  //"dev.zio" %% "izumi-reflect" % "1.1.3",
  "com.typesafe"       %  "config"          % "1.4.2",
  "ch.qos.logback"     %  "logback-classic" % "1.2.10",
  "com.yandex.yoctodb" %  "yoctodb-core"    % "0.0.20",
  "dev.zio"            %% "zio-prelude"     % "1.0.0-RC10",

  "com.softwaremill.magnolia1_3" %% "magnolia" % "1.1.0",

  //"dev.zio" %% "zio-schema" % "0.1.4",

  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

  //https://repo1.maven.org/maven2/com/lihaoyi/ammonite_3.0.1/2.4.0-11-5b9ff5e7/
  //("com.lihaoyi" % "ammonite"  % "2.5.2"  % "test").cross(CrossVersion.full)
)

//Compile / scalacOptions --= Seq("-Xfatal-warnings", "-Ywarn-unused:imports", "-Yno-imports", "-deprecation")

promptTheme := ScalapenosTheme

scalafmtOnCompile := true

/*Test / sourceGenerators += Def.task {
  val file = (Test / sourceManaged).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue*/

Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value)

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

Global / semanticdbEnabled := true
Global / semanticdbVersion := scalafixSemanticdb.revision
Global / watchAntiEntropy := scala.concurrent.duration.FiniteDuration(10000, java.util.concurrent.TimeUnit.MILLISECONDS)

addCommandAlias("sfix", "scalafix OrganizeImports; test:scalafix OrganizeImports")
addCommandAlias("sFixCheck", "scalafix --check OrganizeImports; test:scalafix --check OrganizeImports")

addCommandAlias("c", "compile")
addCommandAlias("r", "reload")