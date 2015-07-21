package net.thunderklaus

import sbt._
import sbt.Keys._
import java.io.File
import com.earldouglas.xsbtwebplugin.WebPlugin._
import com.earldouglas.xsbtwebplugin.PluginKeys._

object GwtPlugin extends Plugin {

  lazy val Gwt = config("gwt") extend (Compile)

  val gwtModules = TaskKey[Seq[String]]("gwt-modules")
  val gwtCompile = TaskKey[Unit]("gwt-compile", "Runs the GWT compiler")
  val gwtForceCompile = TaskKey[Boolean]("gwt-force-compile", "Always recompile gwt modules")
  val gwtDevMode = TaskKey[Unit]("gwt-devmode", "Runs the GWT devmode shell")
  val gwtVersion = SettingKey[String]("gwt-version")
  val gwtTemporaryPath = SettingKey[File]("gwt-temporary-path")
  val gwtWebappPath = SettingKey[File]("gwt-webapp-path")
  val gaeSdkPath = SettingKey[Option[String]]("gae-sdk-path")

  var gwtModule: Option[String] = None
  val gwtSetModule = Command.single("gwt-set-module") { (state, arg) =>
    Project.evaluateTask(gwtModules, state) match {
      case Some(Value(mods)) => {
        gwtModule = mods.find(_.toLowerCase.contains(arg.toLowerCase))
        gwtModule match {
          case Some(m) => println("gwt-devmode will run: " + m)
          case None => println("No match for '" + arg + "' in " + mods.mkString(", "))
        }
      }
      case _ => None
    }
    state
  }

  lazy val gwtSettings: Seq[Setting[_]] = webSettings ++ gwtOnlySettings

  lazy val gwtOnlySettings: Seq[Setting[_]] = inConfig(Gwt)(Defaults.configSettings) ++ Seq(
    managedClasspath in Gwt <<= (managedClasspath in Compile, update) map {
      (cp, up) => cp ++ Classpaths.managedJars(Provided, Set("src"), up)
    },
    unmanagedClasspath in Gwt <<= (unmanagedClasspath in Compile),
    gwtTemporaryPath <<= (target) { (target) => target / "gwt" },
    gwtWebappPath <<= (target) { (target) => target / "webapp" },
    gwtVersion := "2.3.0",
    gwtForceCompile := false,
    gaeSdkPath := None,
    libraryDependencies <++= gwtVersion(gwtVersion => Seq(
      "com.google.gwt" % "gwt-user" % gwtVersion % "provided",
      "com.google.gwt" % "gwt-dev" % gwtVersion % "provided",
      "javax.validation" % "validation-api" % "1.0.0.GA" % "provided" withSources (),
      "com.google.gwt" % "gwt-servlet" % gwtVersion)),
    gwtModules <<= (javaSource in Compile, resourceDirectory in Compile) map {
      (javaSource, resources) => findGwtModules(javaSource) ++ findGwtModules(resources)
    },
    gwtDevMode <<= (dependencyClasspath in Gwt, thisProject in Gwt,  state in Gwt, javaSource in Compile, javaOptions in Gwt,
      gwtModules, gaeSdkPath, gwtWebappPath, streams) map {
      (dependencyClasspath, thisProject, pstate, javaSource, javaOpts, gwtModules, gaeSdkPath, warPath, s) => {
        def gaeFile (path :String*) = gaeSdkPath.map(_ +: path mkString(File.separator))
        val module = gwtModule.getOrElse(gwtModules.headOption.getOrElse(error("Found no .gwt.xml files.")))
        val cp = dependencyClasspath.map(_.data.absolutePath) ++ getDepSources(thisProject.dependencies, pstate) ++
          gaeFile("lib", "appengine-tools-api.jar").toList :+ javaSource.absolutePath
        val javaArgs = javaOpts ++ (gaeFile("lib", "agent", "appengine-agent.jar") match {
          case None => Nil
          case Some(path) => List("-javaagent:" + path)
        })
        val gwtArgs = gaeSdkPath match {
          case None => Nil
          case Some(path) => List(
            "-server", "com.google.appengine.tools.development.gwt.AppEngineLauncher")
        }
        val command = mkGwtCommand(
          cp, javaArgs, "com.google.gwt.dev.DevMode", warPath, gwtArgs, module)
        s.log.info("Running GWT devmode on: " + module)
        s.log.debug("Running GWT devmode command: " + command)
        command !
      }
    },

    gwtCompile <<= (classDirectory in Compile, dependencyClasspath in Gwt, thisProject in Gwt, state in Gwt, javaSource in Compile, unmanagedSourceDirectories in Compile, javaOptions in Gwt,
      gwtModules, gwtTemporaryPath, streams, gwtForceCompile) map {
      (classDirectory, dependencyClasspath, thisProject, pstate, javaSource, unmanagedSource, javaOpts, gwtModules, warPath, s, force) => {

        val srcDirs = Seq(javaSource.absolutePath) ++  unmanagedSource.map(_.absolutePath) ++ getDepSources(thisProject.dependencies, pstate)
        val cp = Seq(classDirectory.absolutePath) ++
          dependencyClasspath.map(_.data.absolutePath) ++
          srcDirs

        val needToCompile : Boolean = {
          s.log.info("Checking GWT module updates: " + gwtModules.mkString(", "))
          val gwtFiles : Seq[File] = (warPath ** "*.nocache.js").get
          if(gwtFiles.isEmpty) {
            s.log.info("No GWT output is found in " + warPath)
            true
          }
          else {
            val lastCompiled = gwtFiles.map(_.lastModified).max
            val moduleDirs = for(d <- srcDirs; m <- (new File(d) ** "*.gwt.xml").get) yield { m.getParentFile }
            val gwtSrcs = for(m <- moduleDirs; f <- (m ** "*").get) yield f
            gwtSrcs.find(lastCompiled < _.lastModified).isDefined
          }
        }

        if(force || needToCompile) {
          val command = mkGwtCommand(
            cp, javaOpts, "com.google.gwt.dev.Compiler", warPath, Nil, gwtModules.mkString(" "))
          s.log.info("Compiling GWT modules: " + gwtModules.mkString(","))
          s.log.debug("Running GWT compiler command: " + command)
          command !
        }
        else
          s.log.info("GWT modules are up to date")
      }
    },
    webappResources in Compile <+= (gwtTemporaryPath) { (t: File) => t },

    packageWar in Compile <<= (packageWar in Compile).dependsOn(gwtCompile),

    commands ++= Seq(gwtSetModule)
  )




  def getDepSources(deps : Seq[ClasspathDep[ProjectRef]], state : State) : Set[String] = {
    var sources = Set.empty[String]
    val structure = Project.extract(state).structure
    def get[A] = setting[A](structure)_
    deps.foreach{
      dep=>
        sources +=  (get(dep.project, Keys.sourceDirectory, Compile).get.toString + "/java")
        sources ++= getDepSources(Project.getProject(dep.project, structure).get.dependencies, state)
    }
    sources
  }

  def setting[T](structure: Load.BuildStructure)(ref: ProjectRef, key: SettingKey[T], configuration: Configuration): Option[T] = key in (ref, configuration) get structure.data

  private def mkGwtCommand(cp: Seq[String], javaArgs: Seq[String], clazz: String, warPath: File,
                           gwtArgs: Seq[String], modules: String) = {
    println("classpath: " + cp.mkString("\n"))
    (List("java", "-cp", cp.mkString(File.pathSeparator)) ++ javaArgs ++
      List(clazz, "-war", warPath.absolutePath) ++ gwtArgs :+ modules).mkString(" ")
  }


  private def findGwtModules(srcRoot: File): Seq[String] = {
    import Path.relativeTo
    val files = (srcRoot ** "*.gwt.xml").get
    val relativeStrings = files.flatMap(_ x relativeTo(srcRoot)).map(_._2)
    relativeStrings.map(_.dropRight(".gwt.xml".length).replace(File.separator, "."))
  }



}