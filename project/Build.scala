/*
 *  ____    ____    _____    ____    ___     ____ 
 * |  _ \  |  _ \  | ____|  / ___|  / _/    / ___|        Precog (R)
 * | |_) | | |_) | |  _|   | |     | |  /| | |  _         Advanced Analytics Engine for NoSQL Data
 * |  __/  |  _ <  | |___  | |___  |/ _| | | |_| |        Copyright (C) 2010 - 2013 SlamData, Inc.
 * |_|     |_| \_\ |_____|  \____|   /__/   \____|        All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU Affero General Public License as published by the Free Software Foundation, either version 
 * 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See 
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this 
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import sbt._
import Keys._
import sbtassembly.Plugin.AssemblyKeys._
import sbt.NameFilter._

object PlatformBuild extends Build {
  val nexusSettings : Seq[Project.Setting[_]] = Seq(
    resolvers ++= Seq(//"Local Maven Repository"            at "file://"+Path.userHome.absolutePath+"/.m2/repository",
                      "ReportGrid repo"                   at "http://nexus.reportgrid.com/content/repositories/releases",
                      "ReportGrid repo (public)"          at "http://nexus.reportgrid.com/content/repositories/public-releases",
                      "ReportGrid snapshot repo"          at "http://nexus.reportgrid.com/content/repositories/snapshots",
                      "ReportGrid snapshot repo (public)" at "http://nexus.reportgrid.com/content/repositories/public-snapshots",
                      "Typesafe Repository"               at "http://repo.typesafe.com/typesafe/releases/",
                      "Maven Repo 1"                      at "http://repo1.maven.org/maven2/",
                      "Guiceyfruit"                       at "http://guiceyfruit.googlecode.com/svn/repo/releases/",
                      "Sonatype Snapshots"                at "https://oss.sonatype.org/content/repositories/snapshots/"),

    credentials += Credentials(Path.userHome / ".ivy2" / ".rgcredentials"),

    publishTo <<= (version) { version: String =>
      val nexus = "http://nexus.reportgrid.com/content/repositories/"
      if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus+"snapshots/") 
      else                                   Some("releases"  at nexus+"releases/")
    }
  )

  lazy val platform = Project(id = "platform", base = file(".")) aggregate(quirrel, yggdrasil, bytecode, daze, ingest, shard, pandora, util, common)
  
  lazy val common   = Project(id = "common", base = file("common")).settings(nexusSettings : _*)
  lazy val util     = Project(id = "util", base = file("util")).settings(nexusSettings: _*)

  lazy val bytecode = Project(id = "bytecode", base = file("bytecode")).settings(nexusSettings: _*)
  lazy val quirrel  = Project(id = "quirrel", base = file("quirrel")).settings(nexusSettings: _*) dependsOn (bytecode % "compile->compile;test->test", util)
  
  lazy val daze     = Project(id = "daze", base = file("daze")).settings(nexusSettings : _*) dependsOn (common, bytecode, yggdrasil % "compile->compile;test->test", util)
  
  val pandoraSettings = sbtassembly.Plugin.assemblySettings ++ nexusSettings
  lazy val pandora  = Project(id = "pandora", base = file("pandora")).settings(pandoraSettings : _*) dependsOn (quirrel, daze, yggdrasil, ingest)
  
  lazy val yggdrasil  = Project(id = "yggdrasil", base = file("yggdrasil")).settings(nexusSettings : _*).dependsOn(common % "compile->compile;test->test", util)
  
  val ingestSettings = sbtassembly.Plugin.assemblySettings ++ nexusSettings
  lazy val ingest   = Project(id = "ingest", base = file("ingest")).settings(ingestSettings: _*).dependsOn(common % "compile->compile;test->test", quirrel, daze, yggdrasil)

  val shardSettings = sbtassembly.Plugin.assemblySettings ++ nexusSettings
  lazy val shard    = Project(id = "shard", base = file("shard")).settings(shardSettings: _*).dependsOn(ingest, common % "compile->compile;test->test", quirrel, daze, yggdrasil)
  
  lazy val performance   = Project(id = "performance", base = file("performance")).settings(nexusSettings : _*).dependsOn(ingest, common, quirrel, daze, yggdrasil, shard)

  val dist = TaskKey[Unit]("dist", "builds dist")
  val dataDir = SettingKey[String]("data-dir", "The temporary directory into which to extract the test data")
  val extractData = TaskKey[String]("extract-data", "Extracts the LevelDB data files used by the tests and the REPL")
  val mainTest = SettingKey[String]("main-test", "The primary test class for the project (just used for pandora)")
}

