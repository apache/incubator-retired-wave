
# PST - Protocol Buffers String Templating

## Abstract

PST allows to quickly generate Java Sourcecode from Protocol Buffer
specifications by automating the process of boiler-plating, compilation and
field definition via templates.

## Building

Generally, PST is an integral part of the Apache Wave build process, and not
called directly. If you want to build it, you can run

  `gradle build`

but it'll usually will be built for your.

## Standalone use

If you want to test PST directly, you'll need to build a standalone version
with

  `gradle shadowJar`

which allows you check out the example via

  `java -jar build/libs/wave-pst-0.1.jar -d example -f example/example.proto example/example.st`

which will result in example/com/example/Person.java being created from the
Protobuf definition of the Person schema in example/example.proto.

The boilerplate code will be coming from example/example.st, which references
example/broto.st, providing the per-field code definitions.

## Way of working

PST generates the corresponding java code using the following steps:

* protoc is called to create Java code from example/example.proto
* the resulting Example.java is compiled using javac
* each Message definition found in the Protocol Buffer file is matched against
  example/example.st
* the resulting Java code is passed through a styler to make it more human-readable

## Use within Apache Wave

Apache Wave is using proto2 Protocol Buffers as documented in
https://developers.google.com/protocol-buffers/docs/proto .. the task
generateMessages in wave/build.gradle is responsible for combining String
Templates as per http://www.stringtemplate.org/ to Java Classes.

A quick introduction to String Templates can be found under
https://theantlrguy.atlassian.net/wiki/display/ST/Five+minute+Introduction
