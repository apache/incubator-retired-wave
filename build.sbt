name := "wave"

organization  := "org.apache"

scalaVersion  := "2.11.6"

version := "0.1.0"

javaSource in Compile := baseDirectory.value / "src"

javaSource in Test := baseDirectory.value / "test"

//includeFilter in Test := "*Test"

//excludeFilter in Test :=  "**/*GwtTest" || "**/*LargeTest" || "**/server/persistence/**"

unmanagedSourceDirectories in Compile += baseDirectory.value / "proto_src"

unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "gxp"

unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "messages"

unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "messages"


unmanagedJars in Compile := (baseDirectory.value / "third_party" / "runtime" ** "*.jar").classpath

unmanagedJars in Compile ++= (baseDirectory.value / "third_party" / "test" ** "*.jar").classpath

unmanagedJars in Compile ++= (baseDirectory.value / "third_party" / "codegen" ** "*.jar").classpath


libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

mainClass in (Compile, run) := Some("org.waveprotocol.box.server.ServerMain")

mainClass in (Compile, packageBin) := Some("org.waveprotocol.box.server.ServerMain")

javaOptions in run += "-Djava.security.auth.login.config=jaas.config"

javaOptions in run += "-Dorg.eclipse.jetty.LEVEL=DEBUG"

fork in run := true
