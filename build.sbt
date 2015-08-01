//  Licensed to the Apache Software Foundation (ASF) under one
//  or more contributor license agreements.  See the NOTICE file
//  distributed with this work for additional information
//  regarding copyright ownership.  The ASF licenses this file
//  to you under the Apache License, Version 2.0 (the
//  "License"); you may not use this file except in compliance
//  with the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing,
//  software distributed under the License is distributed on an
//  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//  KIND, either express or implied.  See the License for the
//  specific language governing permissions and limitations
//  under the License.

// TODO:
// todo - compile messages
// todo   - run the pst tool
// todo - task for Apache Rat
// todo - edit unzipDependencies to move to correct folders and more jars to lib

// Imports
// Task - Definitions
// Task - Dependencies
lazy val dependenciesUnzip = taskKey[Unit]("Uncompresses the Dependancies " +
  "that are stored as zips or wars")
// Task - Compile
lazy val compileProtoBuf = taskKey[Unit]("Compiles all proto source files")
lazy val compileGXP = taskKey[Unit]("Compile GXP Files")
lazy val compileMessages = taskKey[Unit]("Generates the DTO message source")
// Task - Deploy
lazy val deployAuditLicense = taskKey[Unit]("Run the Apache Rat - " +
  "release audit tool ")
// Task - Test
lazy val testGwt = taskKey[Unit]("Test all the Gwt Model")
lazy val testLarge = taskKey[Unit]("Large suit of tests")

// Common Settings between projects
lazy val commonSettings = Seq(
  organization := "org.apache",
  scalaVersion := "2.11.4"
)

// Projects
// todo: future use (when client is separated from server)
//lazy val client = (project in file("client")).
//  settings(commonSettings: _*).
//  settings(
//    // other settings
//  )
// Dependencies
// todo: Split dependencies for server and client
// todo: change wave to server and file(".") to file("server") was separated
lazy val wave = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "wave",
    version := "0.6.0",
    // Dependencies - Test
    libraryDependencies ++= Seq(
      //"org.apache.ant" % "ant-junit" % "1.7.0" % Test, replaced by junit-interface
      "asm" % "asm-commons" % "3.2" % Test,
      "cglib" % "cglib" % "2.2" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "emma" % "emma" % "2.0.5312" % Test,
      "emma" % "emma_ant" % "2.1.5320" % Test,
      "org.hamcrest" % "hamcrest-all" % "1.3" % Test,
      "org.jmock" % "jmock-junit3" % "2.6.0" % Test,
      "org.jmock" % "jmock" % "2.6.0" % Test,
      "org.mockito" % "mockito-all" % "1.9.5" % Test,
      "junit" % "junit" % "4.10" % Test
    ),
    // Dependencies - CodeGen ( Compile )
    libraryDependencies ++= Seq(
      "org.antlr" % "antlr" % "3.2" % Compile,
      "com.google.gwt" % "gwt-dev" % "2.6.1" % Compile,
      "com.google.gwt" % "gwt-user" % "2.6.1" % Compile,
      "com.google.gwt" % "gwt-codeserver" % "2.6.1" % Compile,
      "javax.validation" % "validation-api" % "1.1.0.Final" % Compile,
      "javax.validation" % "validation-api" % "1.1.0.Final-sources.jar" % Compile,
      "org.apache.velocity" % "velocity" % "1.6.3" % Compile
    ),
    // Dependencies - Runtime ( Compile )
    libraryDependencies ++= Seq(
      "aopalliance" % "aopalliance" % "1.0" % Compile,
      "org.bouncycastle" % "bcprov-jdk16" % "1.45" % Compile,
      "commons-fileupload" % "commons-fileupload" % "1.2.2" % Compile withSources(),
      "commons-cli" % "commons-cli" % "1.2" % Compile,
      "commons-codec" % "commons-codec" % "1.4" % Compile withSources(),
      "commons-io" % "commons-io" % "2.4" % Compile,
      "commons-collections" % "commons-collections" % "3.2.1" % Compile,
      "commons-configuration" % "commons-configuration" % "1.6" % Compile,
      "commons-httpclient" % "commons-httpclient" % "3.1" % Compile withSources(),
      "commons-lang" % "commons-lang"  % "2.5" % Compile,
      "commons-logging" % "commons-logging-api" % "1.1" % Compile,
      "commons-logging" % "commons-logging" % "1.1.1" % Compile,
      "dom4j" % "dom4j" % "1.6.1" % Compile,
      "com.google.code.gson" % "gson" % "2.2.4" % Compile,
      "com.google.guava" % "guava" % "15.0" % Compile,
      "com.google.guava" % "guava-gwt" % "15.0" % Compile,
      "com.google.inject.extensions"% "guice-assistedinject" % "3.0" % Compile,
      "com.google.inject.extensions" % "guice-servlet" % "3.0" % Compile,
      "com.google.inject" % "guice" % "3.0" % Compile,
      "javax.inject" % "javax.inject" % "1" % Compile,
      "com.google.gxp" % "google-gxp" % "0.2.4-beta" % Compile,
      "javax.jdo" % "jdo2-api" % "2.2" % Compile from "http://people.apache.org/repo/m1-ibiblio-rsync-repository/javax.jdo/jars/jdo2-api-2.2.jar",
      "org.jdom" % "jdom" % "1.1.3" % Compile,
      "com.google.code.findbugs" % "jsr305" % "2.0.1" % Compile,
      "jline" % "jline" % "0.9.94" % Compile,
      "joda-time" % "joda-time" % "1.6" % Compile,
      "org.apache.lucene" % "lucene-core" % "3.5.0" % Compile,
      "org.mongodb" % "mongo-java-driver" % "2.11.2" % Compile,
      "com.google.code.oauth" % "oauth-provider-source"  % "20100527" % Compile from "https://oauth.googlecode.com/svn/code/maven/net/oauth/core/oauth-provider/20100527/oauth-provider-20100527-sources.jar",
      "com.google.code.oauth" % "oauth" % "20100527" % Compile from "https://oauth.googlecode.com/svn/code/maven/net/oauth/core/oauth/20100527/oauth-20100527.jar",
      "com.google.code.oauth" % "oauth-provider" % "20100527" % Compile from "https://oauth.googlecode.com/svn/code/maven/net/oauth/core/oauth-provider/20100527/oauth-provider-20100527.jar",
      "com.google.code.oauth" % "oauth-consumer" % "20100527" % Compile from "https://oauth.googlecode.com/svn/code/maven/net/oauth/core/oauth-consumer/20100527/oauth-consumer-20100527.jar",
      "com.google.protobuf" % "protobuf-java" % "2.5.0" % Compile,
      "com.google.code" % "protobuf-format-java" %  "1.1" from "https://protobuf-java-format.googlecode.com/files/protobuf-format-java-1.1.jar",
      "org.igniterealtime" % "tinder" % "1.2.1" % Compile,
      "xpp3" % "xpp3" % "1.1.4c" % Compile,
      "xpp3" % "xpp3_xpath" % "1.1.4c" % Compile,
      "org.gnu.inet" % "libidn" % "1.15" % Compile,
      "org.comunes" % "gwt-initials-avatars-shared" % "1.0" % Compile from "http://archiva.comunes.org/repository/comunes-snapshots/cc/kune/gwt-initials-avatars-shared/1.0-SNAPSHOT/gwt-initials-avatars-shared-1.0-20140324.102812-16.jar",
      "org.comunes" % "gwt-initials-avatars-shared-source" % "1.0" % Compile from "http://archiva.comunes.org/repository/comunes-snapshots/cc/kune/gwt-initials-avatars-shared/1.0-SNAPSHOT/gwt-initials-avatars-shared-1.0-20140324.102812-16-sources.jar",
      "org.comunes" % "gwt-initials-avatars-server" % "1.0" % Compile from "http://archiva.comunes.org/repository/comunes-snapshots/cc/kune/gwt-initials-avatars-server/1.0-SNAPSHOT/gwt-initials-avatars-server-1.0-20140324.102825-10.jar",
      "com.typesafe" % "config" % "1.2.1" % Compile,
      "xerces" % "xerces" % "2.4.0" % Compile
    ),
    // Dependencies - Runtime - Atmosphere ( Compile )
    libraryDependencies ++= Seq(
      "org.sonatype" % "atmosphere-runtime" % "2.1.0" % Compile from "https://oss.sonatype.org/service/local/repositories/releases/content/org/atmosphere/atmosphere-runtime/2.1.0/atmosphere-runtime-2.1.0.jar",
      "org.sonatype" % "atmosphere-guice" % "0.8.3" % Compile from "https://oss.sonatype.org/service/local/repositories/releases/content/org/atmosphere/atmosphere-guice/0.8.3/atmosphere-guice-0.8.3.jar",
      "org.sonatype" % "annotation-detector" % "3.0.0" % Compile from "https://oss.sonatype.org/service/local/repositories/releases/content/eu/infomas/annotation-detector/3.0.0/annotation-detector-3.0.0.jar",
      "org.sonatype" % "slf4j-sinple" % "1.6.1" % Compile from "https://oss.sonatype.org/service/local/repositories/releases/content/org/slf4j/slf4j-simple/1.6.1/slf4j-simple-1.6.1.jar",
      "org.sonatype" % "slf4j-api" % "1.6.1" % Compile from "https://oss.sonatype.org/service/local/repositories/releases/content/org/slf4j/slf4j-api/1.6.1/slf4j-api-1.6.1.jar"
    ),
    // Dependencies - Publish
    libraryDependencies ++= Seq(
      "org.apache.rat" % "apache-rat" % "0.11" % Compile
    ),
    // Java Compiler Options
    javacOptions += "-g:none",
    javacOptions ++= Seq("-source", "1.7"),
    autoScalaLibrary := false,
    crossPaths := false,
    includeFilter in Test := "*Test",
    excludeFilter in Test :=  "*GwtTest*" || "*LargeTest*" || "**/server/persistence/**",
    excludeFilter in (Compile, unmanagedSources)  :=  "*GwtTest*" || "*LargeTest*" || "**/server/persistence/**",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "pst" / "proto_src",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "gxp",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "gen" / "messages",
    mainClass in (Compile, run) := Some("org.waveprotocol.box.server.ServerMain"),
    mainClass in (Compile, packageBin) := Some("org.waveprotocol.box.server.ServerMain"),
    javaOptions in run += "-Djava.security.auth.login.config=jaas.config",
    javaOptions in run += "-Dorg.eclipse.jetty.LEVEL=DEBUG",
    fork in run := true,
    parallelExecution in Test := false,
    // Tasks
    // Tasks - Dependencies
    // todo: Unziped dependencies should be updated with mavern repositories where applicable
    dependenciesUnzip := {
      println(s"Download & Extracting Whack")
      IO.unzipURL(new URL("http://www.igniterealtime.org/downloadServlet?filename=" +
        "whack/whack_1_0_0.zip"), new File("lib"))
      println(s"Download & Extracting Apache Rat")
      IO.unzipURL(new URL("http://apache.spd.co.il/creadur/apache-rat-0.10/" +
        "apache-rat-0.10-bin.zip"), new File("lib"))
      println(s"Download & Extracting Apache Lucene Solr - (This will take time)")
      IO.unzipURL(new URL("https://archive.apache.org/dist/lucene/solr/4.9.1/" +
        "solr-4.9.1.zip"), new File("lib"))
      println(s"Download & Extracting Sona war")
      IO.unzipURL(new URL("https://oss.sonatype.org/service/local/repositories/" +
        "releases/content/org/atmosphere/client/javascript/2.1.5/" +
        "javascript-2.1.5.war"), new File("lib"))
      println(s"Download & Extracting jetty")
      IO.unzipURL(new URL("https://olex-secure.openlogic.com/content/openlogic/" +
        "jetty/9.1.1/jetty-distribution-9.1.1.v20140108.zip"), new File("lib"))
    },
    // Tasks - Compile
    compileProtoBuf := {
      "protoc --proto_path=src/main/java/ --java_out=proto_src src/main/java/org/waveprotocol/box/common/comms/waveclient-rpc.proto" +
        " src/main/java/org/waveprotocol/box/profile/profiles.proto" +
        " src/main/java/org/waveprotocol/box/search/search.proto" +
        " src/main/java/org/waveprotocol/box/server/persistence/protos/account-store.proto" +
        " src/main/java/org/waveprotocol/box/server/persistence/protos/delta-store.proto" +
        " src/main/java/org/waveprotocol/box/server/rpc/rpc.proto" +
        " src/main/java/org/waveprotocol/box/attachment/attachment.proto" +
        " src/main/java/org/waveprotocol/wave/concurrencycontrol/clientserver.proto" +
        " src/main/java/org/waveprotocol/wave/diff/diff.proto" +
        " src/main/java/org/waveprotocol/wave/federation/federation.protodevel" +
        " src/main/java/org/waveprotocol/wave/federation/federation_error.protodevel" +
        " src/main/java/org/waveprotocol/protobuf/extensions.proto" !
    },
    compileGXP := {
      "java -cp lib/gxp-0.2.4-beta.jar com.google.gxp.compiler.cli.Gxpc " +
        "--dir gen/gxp/ " +
        "--source src/main/java/ " +
        "--output_language java " +
        "src/main/java/org/waveprotocol/box/server/gxp/AnalyticsFragment.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/AuthenticationPage.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/OAuthAuthorizationCodePage.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/OAuthAuthorizeTokenPage.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/RobotRegistrationPage.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/RobotRegistrationSuccessPage.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/TopBar.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/UserRegistrationPage.gxp " +
        "src/main/java/org/waveprotocol/box/server/gxp/WaveClientPage.gxp" !
    },
    compileMessages := {
      // write function to automate the passing of the class file
      def runpst(protoClass:String): Unit = {
        "java -classpath " +
          "third_party/runtime/annotation-detector-3.0.0.jar:" +
          "third_party/runtime/aopalliance-1.0.jar:" +
          "third_party/runtime/atmosphere-guice-0.8.3.jar:" +
          "third_party/runtime/atmosphere-runtime-2.1.0.jar:" +
          "third_party/runtime/bcprov-jdk16-1.45.jar:" +
          "third_party/runtime/commons-cli-1.2.jar:" +
          "third_party/runtime/commons-codec-1.4.jar:" +
          "third_party/runtime/commons-collections-3.2.1.jar:" +
          "third_party/runtime/commons-configuration-1.6.jar:" +
          "third_party/runtime/commons-fileupload-1.2.2.jar:" +
          "third_party/runtime/commons-httpclient-3.1.jar:" +
          "third_party/runtime/commons-io-2.4.jar:" +
          "third_party/runtime/commons-lang-2.5.jar:" +
          "third_party/runtime/commons-logging-1.1.1-api.jar:" +
          "third_party/runtime/commons-logging-1.1.1.jar:" +
          "third_party/runtime/config-1.2.1.jar:" +
          "third_party/runtime/dom4j-1.6.1.jar:" +
          "third_party/runtime/google-gxp-0.2.4-beta.jar:" +
          "third_party/runtime/gson-2.2.4.jar:" +
          "third_party/runtime/guava-15.0.jar:" +
          "third_party/runtime/guava-gwt-15.0.jar:" +
          "third_party/runtime/guice-3.0.jar:" +
          "third_party/runtime/guice-assistedinject-3.0.jar:" +
          "third_party/runtime/guice-servlet-3.0.jar:" +
          "third_party/runtime/gwt-initials-avatars-server-1.0-20140324.102825-10.jar:" +
          "third_party/runtime/gwt-initials-avatars-shared-1.0-20140324.102812-16-sources.jar:" +
          "third_party/runtime/gwt-initials-avatars-shared-1.0-20140324.102812-16.jar:" +
          "third_party/runtime/javax.inject-1.jar:" +
          "third_party/runtime/jdo2-api-2.2.jar:" +
          "third_party/runtime/jdom-1.1.3.jar:" +
          "third_party/runtime/jetty-annotations-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-client-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-continuation-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-http-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-io-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-proxy-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-security-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-server-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-servlet-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-servlets-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-util-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-webapp-9.1.1.v20140108.jar:" +
          "third_party/runtime/jetty-xml-9.1.1.v20140108.jar:" +
          "third_party/runtime/jline-0.9.94.jar:" +
          "third_party/runtime/joda-time-1.6.jar:" +
          "third_party/runtime/jsr305-2.0.1.jar:" +
          "third_party/runtime/libidn-1.15.jar:" +
          "third_party/runtime/lucene-core-3.5.0.jar:" +
          "third_party/runtime/mongo-java-driver-2.11.2.jar:" +
          "third_party/runtime/oauth-20100527.jar:" +
          "third_party/runtime/oauth-consumer-20100527.jar:" +
          "third_party/runtime/oauth-provider-20100527-sources.jar:" +
          "third_party/runtime/oauth-provider-20100527.jar:" +
          "third_party/runtime/protobuf-format-java-1.1.jar:" +
          "third_party/runtime/protobuf-java-2.5.0.jar:" +
          "third_party/runtime/servlet-api-3.1.jar:" +
          "third_party/runtime/slf4j-api-1.6.1.jar:" +
          "third_party/runtime/slf4j-simple-1.6.1.jar:" +
          "third_party/runtime/tinder-1.2.1.jar:" +
          "third_party/runtime/websocket-api-9.1.1.v20140108.jar:" +
          "third_party/runtime/websocket-client-9.1.1.v20140108.jar:" +
          "third_party/runtime/websocket-common-9.1.1.v20140108.jar:" +
          "third_party/runtime/websocket-server-9.1.1.v20140108.jar:" +
          "third_party/runtime/websocket-servlet-9.1.1.v20140108.jar:" +
          "third_party/runtime/whack.jar:" +
          "third_party/runtime/xerces-2.4.0.jar:" +
          "third_party/runtime/xpp3-1.1.4c.jar:" +
          "third_party/runtime/xpp3_xpath-1.1.4c.jar:" +
          "third_party/codegen/antlr-3.2.jar:" +
          "third_party/codegen/gwt-codeserver-2.6.1.jar:" +
          "third_party/codegen/gwt-dev-2.6.1.jar:" +
          "third_party/codegen/gwt-user-2.6.1.jar:" +
          "third_party/codegen/validation-api-1.1.0.Final-sources.jar:" +
          "third_party/codegen/validation-api-1.1.0.Final.jar:" +
          "third_party/codegen/velocity-1.6.3-dep.jar:" +
          "third_party/codegen/velocity-1.6.3.jar:" +
          "third_party/test/ant-junit-1.7.0.jar:" +
          "third_party/test/asm-3.2.jar:" +
          "third_party/test/cglib-2.2.jar:" +
          "third_party/test/emma-2.0.5312.jar:" +
          "third_party/test/emma_ant-2.1.5320.jar:" +
          "third_party/test/hamcrest-all-1.2.jar:" +
          "third_party/test/jmock-2.6.0.jar:" +
          "third_party/test/jmock-junit3-2.6.0.jar:" +
          "third_party/test/junit-4.10.jar:" +
          "third_party/test/mockito-all-1.9.5.jar:" +
          "lib/pst.jar:" +
          "build/proto " +
          "org.waveprotocol.pst.PstMain " +
          "-s pst -d gen/messages -f " + protoClass +
          " src/pst/java/org/waveprotocol/pst/templates/api/api.st " +
          "src/pst/java/org/waveprotocol/pst/templates/builder/builder.st " +
          "src/pst/java/org/waveprotocol/pst/templates/pojo/pojo.st " +
          "src/pst/java/org/waveprotocol/pst/templates/jso/jso.st " +
          "src/pst/java/org/waveprotocol/pst/templates/util/util.st " +
          "src/pst/java/org/waveprotocol/pst/templates/gson/gson.st " +
          "src/pst/java/org/waveprotocol/pst/templates/proto/proto.st" !
      }
      // create list of class file's
      val protoClasses = Array(
        "/org/waveprotocol/box/common/comms/WaveClientRpc.class",
        "/org/waveprotocol/box/search/SearchProto.class",
        "/org/waveprotocol/box/profile/ProfilesProto.class",
        "/org/waveprotocol/box/server/rpc/Rpc.class",
        "/org/waveprotocol/box/attachment/AttachmentProto.class",
        "/org/waveprotocol/wave/federation/Proto.class",
        "/org/waveprotocol/wave/concurrencycontrol/ClientServer.class",
        "/org/waveprotocol/wave/diff/Diff.class"
      )
      // loop through and process them
      protoClasses.foreach(runpst)
    }
  )

lazy val pst = (project in file("src/pst")).
  settings(
    // Source Files
    javaSource in Compile := baseDirectory.value / "java",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "proto_src",
    // Dependencies
    libraryDependencies ++= Seq(
      "org.antlr" % "antlr" % "3.2" % Compile,
      "com.google.protobuf" % "protobuf-java" % "2.5.0" % Compile,
      "com.google.guava" % "guava" % "15.0" % Compile,
      "commons-cli" % "commons-cli" % "1.2" % Compile
    ),
    javacOptions += "-g:none",
    javacOptions ++= Seq("-source", "1.7"),
    autoScalaLibrary := false,
    crossPaths := false
  )