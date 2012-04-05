import sbt._
import Keys._
import scala.xml._
import java.net.URL

object ScalatraContribBuild extends Build {
  import Dependencies._
  import Resolvers._

  val scalatraVersion = "2.1.0-SNAPSHOT"

  lazy val scalatraContribSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "1.0.0-SNAPSHOT",
    crossScalaVersions := Seq("2.9.1", "2.9.0-1", "2.9.0", "2.8.2", "2.8.1"),
    scalaVersion <<= (crossScalaVersions) { versions => versions.head },
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    manifestSetting,
    publishSetting,
    resolvers += ScalaToolsSnapshots
  ) ++ mavenCentralFrouFrou

  lazy val scalatraContribProject = Project(
    id = "scalatra-contrib",
    base = file("."),
    settings = scalatraContribSettings ++ Unidoc.settings ++ doNotPublish ++ Seq(
      description := "Scalatra contributed extensions" /*,
      (name in Posterous) := "scalatra-contrib" */
    ),
/*
    aggregate = Seq(scalatraCore, scalatraAuth, scalatraFileupload,
      scalatraScalate, scalatraSocketio, scalatraLiftJson, scalatraAntiXml,
      scalatraTest, scalatraScalatest, scalatraSpecs, scalatraSpecs2,
      scalatraExample, scalatraAkka, scalatraDocs)
*/
    aggregate = Seq(commonUtilites, validations)
  )

  lazy val commonUtilites = Project(
    id = "contrib-commons",
    base = file("commons"),
    settings = scalatraContribSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatraModule("scalatra"), servletApi, scalaz, slf4jSimple % "test", specs2(sv) % "test")),
      description := "Common utilities for contrib modules"
    )
  ) // dependsOn(Seq(scalatraSpecs2, scalatraSpecs, scalatraScalatest) map { _ % "test->compile" } :_*)

  lazy val validations = Project(
    id = "contrib-validation",
    base = file("validations"),
    settings = scalatraContribSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatraModule("scalatra"), servletApi, scalaz, slf4jSimple % "test", specs2(sv) % "test", scalatraModule("scalatra-specs2") % "test")),
      description := "Validation module"
    )
  ) dependsOn(commonUtilites)

  object Dependencies {

    def scalatraModule(moduleName: String) = {
	    "org.scalatra" % moduleName %	scalatraVersion
    }

    def grizzledSlf4j(scalaVersion: String) = {
      // Temporary hack pending 2.8.2 release of slf4s.
      val artifactId = "grizzled-slf4j_"+(scalaVersion match {
        case "2.8.2" => "2.8.1"
        case v => v
      })
      "org.clapper" % artifactId % "0.6.6"
    }

    val junit = "junit" % "junit" % "4.10"

    def scalatest(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "1.5.1"
        case _ => "1.6.1"
      }
      "org.scalatest" %% "scalatest" % libVersion
    }

    def specs(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case "2.9.1" => "1.6.9"
        case _ => "1.6.8"
      }
      "org.scala-tools.testing" %% "specs" % libVersion
    }

    def specs2(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "1.5"
        case "2.9.0" => "1.5" // https://github.com/etorreborre/specs2/issues/33
        case _ => "1.7.1"
      }
      "org.specs2" %% "specs2" % libVersion
    }

    val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"

    val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"

  }

  object Resolvers {
    val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  }

  lazy val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

  lazy val publishSetting = publishTo <<= (version) { version: String =>
    if (version.trim.endsWith("SNAPSHOT"))
      Some(sonatypeNexusSnapshots)
    else
      Some(sonatypeNexusStaging)
  }

  // Things we care about primarily because Maven Central demands them
  lazy val mavenCentralFrouFrou = Seq(
    homepage := Some(new URL("http://www.scalatra.org/")),
    startYear := Some(2009),
    licenses := Seq(("BSD", new URL("http://github.com/scalatra/scalatra/raw/HEAD/LICENSE"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
      <scm>
        <url>http://github.com/scalatra/scalatra</url>
        <connection>scm:git:git://github.com/scalatra/scalatra.git</connection>
      </scm>
      <developers>
        <developer>
          <id>riffraff</id>
          <name>Gabriele Renzi</name>
          <url>http://www.riffraff.info</url>
        </developer>
        <developer>
          <id>alandipert</id>
          <name>Alan Dipert</name>
          <url>http://alan.dipert.org</url>
        </developer>
        <developer>
          <id>rossabaker</id>
          <name>Ross A. Baker</name>
          <url>http://www.rossabaker.com/</url>
        </developer>
        <developer>
          <id>chirino</id>
          <name>Hiram Chirino</name>
          <url>http://hiramchirino.com/blog/</url>
        </developer>
        <developer>
          <id>casualjim</id>
          <name>Ivan Porto Carrero</name>
          <url>http://flanders.co.nz/</url>
        </developer>
        <developer>
          <id>jlarmstrong</id>
          <name>Jared Armstrong</name>
          <url>http://www.jaredarmstrong.name/</url>
        </developer>
      </developers>
    )}
  )

  lazy val doNotPublish = Seq(publish := {}, publishLocal := {})
}
