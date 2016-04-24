[![Build Status](https://builds.apache.org/buildStatus/icon?job=wave-small_tests)](https://builds.apache.org/job/wave-small_tests)   

# Apache Wave

The Apache Wave project is a stand alone wave server and rich web client
that serves as a Wave reference implementation.
Apache Wave site: http://incubator.apache.org/wave/.
This project lets developers and enterprise users run wave servers and
host waves on their own hardware. And then share those waves with other
wave servers.

## Cryptographic Software Notice

This distribution includes cryptographic software.  The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of
encryption software.  BEFORE using any encryption software, please
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to
see if this is permitted.  See <http://www.wassenaar.org/> for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS
Export Administration Regulations, Section 740.13) for both object
code and source code.

The following provides more details on the included cryptographic
software:

  Wave requires the BouncyCastle Java cryptography APIs:
    http://www.bouncycastle.org/java.html

## Run Binary

The nightly binaries can be downloaded from https://builds.apache.org/view/S-Z/view/Wave/job/wave-artifacts/lastSuccessfulBuild/artifact/.
The latest "dev" releases can be downloaded from: https://dist.apache.org/repos/dist/dev/incubator/wave/.
The latest officially released binaries can be downloaded from: https://dist.apache.org/repos/dist/release/incubator/wave/.
Extract the archive and execute run-server.sh for Linux/Mac or run-server.bat for Windows.
The web client will be accessible by default at http://localhost:9898/.

## Setup with Vagrant

A vagrant setup has been provided for automatic compile on a Ubuntu or Fedora
linux box. A windows box is also provided for testing but only installs requirements,
compilation and setup of the server require manual setup.

Note:

- requires vagrant and virtual box to be installed and an internet
connection.
- these images use jdk v8 which isn't officially supported but is used to test
for future compatibility.

### Ubuntu & Fedora ( recommended )

running `vagrant up ubuntu` or `vagrant up fedora` will create a linux box
where the project will be compiled and dist installed to `/opt/apache/wave`. In
this folder you can find the current dist source and run the server. A server
config file has been provided to allow the server to be accessible outside the
vm listening on `0.0.0.0:9898`.

To update the dist just run `vagrant up linux` where linux is either ubuntu or
fedora.

note:
`vagrant ssh linux` where linux is either ubuntu or fedora can be used for a
ssh session.

### Windows 10 (requires vagrant 1.8+)

running `vagrant up win10` will setup a windows 10 environment for Apache Wave,
this vm does not setup a dist but that can be done by following the steps below
in the Gradle Tasks section within the vm.

The virtual machine will make a copy of the current source into the users
documents folder under Apache-Wave. Running the vagrant up command again will
update this folder. The standard config for vagrant testing can be located at
`scripts/vagrant/application.conf`, just copy this file to the location
of the distributions config folder.

### Note

These vagrant setups are not production use and should not be used as such.

## Setup Dev

Apache Wave can be setup for eclipse and intellij IDE's.

Running `./gradlew eclipse` or `./gradlew idea` will generate all project files needed.
In a situation where dependencies have changed or project structure has changed
run `./gradlew cleanEclipse` or `./gradlew cleanIdea` depending on your IDE.


## Gradle Tasks

Apache Wave requires Java 7 & Gradle 2.8+ to build.

Gradle tasks can be run by `./gradlew [task name]`

Test Tasks:

- **test**: runs the standard unit tests.
- **testMongo**: runs the mongodb tests.
- **testLarge**: runs the more lengthy test cases.
- **testGwt**: runs gwt specific tests (currently broken till gwt jetty conflict issue).
- **testAll**: runs all the above tests.

Compile Tasks:

- **generateMessages**: Generates the message source files from the .st sources.
- **generateGXP**: Compiles sources from the gxp files.
- **compileJava**: Compiles all java sources.
- **compileGwt**: Compiles all the Gwt sources.
- **compileGwtDemo**: Compiles all the Gwt sources in Demo style.
- **compileGwtDev**: Compiles all the Gwt sources in Dev style.

Check Tasks:

- **rat**: will run the apache rat tool to check all distribution files.

Run Tasks:

- **run**: runs the server with the default parameters and with gwt compiled normally.
- **gwtDev**: runs the gwt development mode.

Distribution Tasks:
- **jar**: builds jar file for the project.
- **sourcesJar**: builds a source jar file for each project.
- **createDist**: builds the zip and tar file for bin and source.
- **createDistBin**: builds the zip for distribution.
- **createDistBinZip**: builds the zip for distribution.
- **createDistBinTar**: builds the tar for distribution.
- **createDistSource**: builds the zip and tar file for distributing the source.
- **createDistSourceZip**: builds the zip for distributing the source.
- **createDistSourceTar**: builds the tar for distributing the source.


## Build

To build the client and server:
    `./gradlew jar`
It will be created in wave/build/libs/wave-*version*.jar

The sources can also be packaged into a jar by doing
    `./gradlew sourcesJar`
This will create a `project name`-sources.jar in each projects build/libs directory.

Note:

- if pst-`version`.jar is unable to be found run `./gradlew pst:jar` then retry.
- if a jar is unable to be unzipped with wave:extractApi then delete the jar from your cache and try again.
    You may need to restart. If problem persists let the newsgroup know or create an issue on Jira.

To config your server a default configuration is provided by reference.conf,
this can be overwritten by application.conf with custom values.

To enable federation the following must be run.

To create a simple configuration run:
    `./gradlew prosody-config`

To override default values pass them to the ant script.
For example, to override wave\_server\_domain run:
`./gradlew prosody-config -Dwave_server_domain=example.com`
Take a look at the reference.conf to learn about configuration and possible/default values.

The server can be started (on Linux/MacOS) by running
    ./run-server.sh
Or on Windows by running
    run-server.bat
    Note: must be cd'ed into the root directory
Or, you can run the server from the compiled classes with Gradle:
    gradle run
The web client will be accessible by default at http://localhost:9898/.


## To learn more about Wave in a Box and Wave Federation Protocol:

1. Subscribe to the wave-dev mailing list, find instructions at http://incubator.apache.org/wave/mailing-lists.html.
2. Visit the Apache Wave wiki at https://cwiki.apache.org/confluence/display/WAVE/Home.
3. Look at the white papers folder - the information is a bit old but still usable.
4. Watch the Wave Summit videos on YouTube, find the links at: https://cwiki.apache.org/confluence/display/WAVE/Wave+Summit+Talks


## To enable SSL:

Create a Java keystore for your server (e.g. using http://portecle.sourceforge.net/).
You will need a key (e.g. called "server") whose subject Common Name (CN) is
the hostname of your server.

Set enable_ssl = true and set the ssl_keystore_path and ssl_keystore_password options.


To enable X.509 client authentication:

If your users have X.509 certificates which include their email address, you can have
them logged in automatically (with their wave ID being the same as their email address):
You can get X.509 certificates issued from any normal CA (e.g. StartSSL offer them for free).
You can get your CA's certficate from their website, though note they might provide more than 1 certificate which you need to chain before your client certificates are considered trusted.

1. Add the signing CA to your keystore file.
2. Set enable_clientauth = true
3. Set clientauth_cert_domain (to the part after the "@" in your email addresses).
4. (optional) Set disable_loginpage = true to prevent password-based logins.

Users will be automatically logged in when they access the site, with the
username taken from the email address in their certificate.

Setting up third party optional dependencies:

## To enable MongoDB:

In order to specify MongoDB in server.config as the storage option for storing deltas, accounts and attachments - you need to install according to instructions at: http://www.mongodb.org/downloads.
Or on Ubuntu Linux you can use the following command:
    sudo apt-get install mongodb-org

## To enable Solr (Currently Disabled):

In order to specify Solr in server.config as the search type - you need to install Solr according to instructions at: http://www.apache.org/dyn/closer.cgi/lucene/solr/4.9.1.
Or, you can use built in Ant script, i.e. run:
    ant get-third-party-solr-dep
This will download and unzip the Solr distribution into third_party/solr folder.
You can then run the Solr server with:
    run-solr.sh
for Linux/Mac or:
    run-solr.bat
for Windows.
