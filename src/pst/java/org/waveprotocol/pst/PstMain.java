/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.pst;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.protobuf.Descriptors.FileDescriptor;

import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.List;

/**
 * Entry point for command line protobuf-stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class PstMain {

  public static void main(String[] args) {
    PstCommandLine cl = null;
    try {
      cl = new PstCommandLine(args);
    } catch (ParseException e) {
      System.err.println("Error parsing command line arguments: " + e.getMessage());
      PstCommandLine.printHelp();
      System.exit(1);
    }

    if (cl.hasHelp()) {
      PstCommandLine.printHelp();
      System.exit(0);
    }

    FileDescriptor fd = PstFileDescriptor.load(
        cl.getProtoFile().getPath(),
        cl.shouldSaveJava() ? cl.getOutputDir() : Files.createTempDir(),
        cl.getProtoPath());
    if (fd == null) {
      System.err.println("Error: cannot find file descriptor for " + cl.getProtoFile());
      System.exit(1);
    }

    boolean failed = false;

    List<File> templates = Lists.newArrayList();
    for (File maybeTemplate : cl.getTemplateFiles()) {
      if (maybeTemplate.exists()) {
        templates.add(maybeTemplate);
      } else {
        System.err.println("ERROR: template " + maybeTemplate.getPath() + " does not exist.");
        failed = true;
      }
    }

    Pst pst = new Pst(cl.getOutputDir(), fd, cl.getStyler(), templates, cl.shouldSavePreStyled(),
        cl.shouldUseInt52());
    try {
      pst.run();
    } catch (PstException e) {
      System.err.printf("ERROR: generation failed for %d/%d templates:\n",
          e.getTemplateExceptions().size(), templates.size());
      for (PstException.TemplateException te : e.getTemplateExceptions()) {
        System.err.println('\n' + te.getTemplateName() + " failed:");
        te.printStackTrace(System.err);
      }
      failed = true;
    }

    if (failed) {
      System.exit(1);
    }
  }
}
