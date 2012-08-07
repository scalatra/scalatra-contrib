import sbt._
import Keys._
import scala.xml._
import java.net.URL

object ScalatraContribBuild extends Build {

  import Dependencies._
  import Resolvers._

  val scalatraVersion = "2.2.0-SNAPSHOT"

  lazy val scalatraContribSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.scalatra",
    version := "1.1.0-SNAPSHOT",
    scalaVersion := "2.9.2",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    javacOptions ++= Seq("-target", "1.6", "-source", "1.6"),
    manifestSetting,
    publishSetting,
    resolvers ++= Seq(sonatypeNexusSnapshots, sonatypeNexusReleases),
    crossPaths := false
  ) ++ mavenCentralFrouFrou

  lazy val scalatraContribProject = Project(
    id = "scalatra-contrib",
    base = file("."),
    settings = scalatraContribSettings ++ Unidoc.unidocSettings,
    aggregate = Seq(commonUtilites, validations, scalateSupport)
  )

  lazy val commonUtilites = Project(
    id = "contrib-commons",
    base = file("commons"),
    settings = scalatraContribSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatraModule("scalatra"), servletApi, scalatraModule("scalatra-lift-json"), scalaz, slf4jSimple % "test", scalatraModule("scalatra-specs2") % "test")),
      description := "Common utilities for contrib modules"
    )
  )

  lazy val scalateSupport = Project(
    id = "contrib-scalate",
    base = file("scalate"),
    settings = scalatraContribSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatraModule("scalatra"), servletApi, scalatraModule("scalatra-scalate"), scalatraModule("scalatra-specs2") % "test")),
      description := "Scalate utilities for contrib modules"
    )
  )

  lazy val validations = Project(
    id = "contrib-validation",
    base = file("validations"),
    settings = scalatraContribSettings ++ Seq(
      libraryDependencies <++= scalaVersion(sv => Seq(scalatraModule("scalatra"), servletApi, scalaz, slf4jSimple % "test", specs2(sv) % "test", scalatraModule("scalatra-specs2") % "test")),
      libraryDependencies ++= Seq(
        "commons-validator"       % "commons-validator"  % "1.4.0",
        "io.backchat.inflector"  %% "scala-inflector"    % "1.3.3"
      ),
      description := "Validation module",
      ivyXML := <dependencies>
        <dependency org="org.eclipse.jetty" name="test-jetty-servlet" rev="8.1.3.v20120416">
            <exclude org="org.eclipse.jetty.orbit"/>
        </dependency>
      </dependencies>
    )
  ) dependsOn (commonUtilites)

  object Dependencies {

    def scalatraModule(moduleName: String) = {
      "org.scalatra" % moduleName % scalatraVersion
    }

    def grizzledSlf4j(scalaVersion: String) = {
      // Temporary hack pending 2.8.2 release of slf4s.
      val artifactId = "grizzled-slf4j_" + (scalaVersion match {
        case "2.8.2" => "2.8.1"
        case v => v
      })
      "org.clapper" % artifactId % "0.6.6"
    }

    val junit = "junit" % "junit" % "4.10"

    def scalatest(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "1.5.1"
        case _ => "1.8"
      }
      "org.scalatest" %% "scalatest" % libVersion
    }

    def specs(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case "2.9.1" | "2.9.2" => "1.6.9"
        case _ => "1.6.8"
      }
      "org.scala-tools.testing" %% "specs" % libVersion
    }

    def specs2(scalaVersion: String) = {
      val libVersion = scalaVersion match {
        case x if x startsWith "2.8." => "1.5"
        case "2.9.0" => "1.5" // https://github.com/etorreborre/specs2/issues/33
        case _ => "1.12"
      }
      "org.specs2" %% "specs2" % libVersion
    }

    val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.6.4"

    val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"

  }

  object Resolvers {
    val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeNexusReleases = "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"
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

  lazy val publishSetting = publishTo <<= (version) {
    version: String =>
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
    pomExtra <<= (pomExtra, name, description) {
      (pom, name, desc) => pom ++ Group(
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
            <developer>
              <id>robb1e</id>
              <name>Robbie Clutton</name>
              <url>http://blog.iclutton.com/</url>
            </developer>
            <developer>
              <id>m20o</id>
              <name>Massimiliano Mazzarolo</name>
              <url>http://m20o.blogspot.it/</url>
            </developer>
          </developers>
      )
    }
  )

  lazy val doNotPublish = Seq(publish := {}, publishLocal := {})
}
