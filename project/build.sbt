resolvers ++= Seq(
  "Oncue Bintray Repo" at "http://dl.bintray.com/oncue/releases",
  "Docker Java" at "https://oss.sonatype.org/content/groups/public"
)

libraryDependencies ++= Seq(
  "org.kohsuke" % "github-api" % "1.59",
  "io.verizon.knobs" %% "core" % "3.12.27a",
  "org.scalaz" %% "scalaz-core" % "7.2.10",
  "com.github.docker-java" % "docker-java" % "3.0.8",
  "com.google.code.findbugs" % "jsr305" % "2.0.0"
)

disablePlugins(TravisCiPlugin)

scalacOptions ++= commonScalacOptions_2_10

// sbt/sbt#2572
scalacOptions in (Compile, console) --= Seq(
  "-Yno-imports",
  "-Ywarn-unused-import")
