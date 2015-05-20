name := "wave"

organization  := "org.apache"

scalaVersion  := "2.11.6"

version := "0.1.0"

javaSource in Compile := baseDirectory.value / "src"

unmanagedSourceDirectories in Compile += baseDirectory.value / "proto_src"

unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "gxp"

unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "messages"

unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "messages"

//managedSourceDirectories in Compile <<=(javaSource in Compile) (base => base / "proto_src" ::  Nil)

//javaSource in Test := baseDirectory.value / "test"

//unmanagedBase := baseDirectory.value / "third_party" / "runtime"

//unmanagedJars in Compile ++= {
//  val base = baseDirectory.value
//  val baseDirectories = (base / "thirparty" / "runtime")
//  val customJars = (baseDirectories ** "*.jar")
//  customJars.classpath
//}

unmanagedJars in Compile := (baseDirectory.value / "third_party" / "runtime" ** "*.jar").classpath

unmanagedJars in Test := (baseDirectory.value / "third_party" / "test" ** "*.jar").classpath

libraryDependencies += "com.google.gwt" % "gwt-user" % "2.7.0"

libraryDependencies += "com.google.gwt" % "gwt-dev" % "2.7.0"

libraryDependencies += "com.google.gwt" % "gwt-codeserver" % "2.7.0"




