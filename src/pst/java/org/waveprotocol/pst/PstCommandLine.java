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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.waveprotocol.pst.style.PstStyler;
import org.waveprotocol.pst.style.Styler;

import java.io.File;
import java.util.Map;

/**
 * Encapsulates the command line options to protobuf-stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class PstCommandLine {

  private static final String DEFAULT_OUTPUT_DIR = ".";
  private static final String DEFAULT_PROTO_PATH = ".";
  private static final Map<String, Styler> STYLERS = ImmutableMap.<String, Styler> builder()
      .put("none", Styler.EMPTY)
      .put("pst", new PstStyler())
      .build();

  private final CommandLine cl;

  public PstCommandLine(String... args) throws ParseException {
    cl = new BasicParser().parse(getOptions(), args);
    checkArgs();
  }

  private void checkArgs() throws ParseException {
    if (!hasFile()) {
      throw new ParseException("Must specify file");
    }
    if (cl.getArgList().isEmpty()) {
      throw new ParseException("Must specify at least one template");
    }
  }

  private static Options getOptions() {
    Options options = new Options();
    options.addOption("h", "help", false, "Show this help");
    options.addOption("f", "file", true, "The protobuf specification file to use");
    options.addOption("d", "dir", true, String.format(
        "The base directory to output generated files to (default: %s)", DEFAULT_OUTPUT_DIR));
    options.addOption("s", "styler", true, "The styler to use, if any (default: none). " +
        "Available options: " + STYLERS.keySet());
    options.addOption("i", "save_pre_styled", false, "Save the intermediate pre-styled files");
    options.addOption("j", "save_java", false, "Save the protoc-generated Java file, if any");
    options.addOption("I", "proto_path", true, "Extra path to search for proto extensions. "
        + "This needs to be specified if the target file is a .proto file with any of the PST-"
        + "specific extensions, in which case the path should include both PST source "
        + "base and the protoc source base; i.e., /PATH/TO/PST/src:/PATH/TO/PROTOC/src");
    options.addOption("t", "int52", true,
        "Specifies if pst should store 64-bit integers should be serialized to"
            + "doubles which will use 52-bit precision. It's useful "
            + "when data is meant to be serialized/deserialized in JavaScript, since it doesn't "
            + "support 64-bit integers (default: false).");
    return options;
  }

  public boolean hasHelp() {
    return cl.hasOption('h');
  }

  // NOTE: private because it's always true, checked in checkArgs().
  private boolean hasFile() {
    return cl.hasOption('f');
  }

  public static void printHelp() {
    new HelpFormatter().printHelp(
        PstMain.class.getSimpleName() + " [options] templates...", getOptions());
  }

  public File getProtoFile() {
    return new File(cl.getOptionValue('f'));
  }

  @SuppressWarnings("unchecked")
  public Iterable<File> getTemplateFiles() {
    return Iterables.transform(cl.getArgList(), new Function<String, File>() {
      @Override public File apply(String filename) {
        return new File(filename);
      }
    });
  }

  public File getOutputDir() {
    return new File(cl.hasOption('d') ? cl.getOptionValue('d') : DEFAULT_OUTPUT_DIR);
  }

  public File getProtoPath() {
    return new File(cl.hasOption('I') ? cl.getOptionValue('I') : DEFAULT_PROTO_PATH);
  }

  public Styler getStyler() {
    if (cl.hasOption('s')) {
      String stylerName = cl.getOptionValue('s');
      if (STYLERS.containsKey(stylerName)) {
        return STYLERS.get(stylerName);
      } else {
        System.err.println("WARNING: unrecognised styler: " + stylerName + ", using none");
        return Styler.EMPTY;
      }
    } else {
      return Styler.EMPTY;
    }
  }

  public boolean shouldSavePreStyled() {
    return cl.hasOption('p');
  }

  public boolean shouldSaveJava() {
    return cl.hasOption('j');
  }

  public boolean shouldUseInt52() {
    return !cl.hasOption('t') //
        || (cl.hasOption('t') && "true".equals(cl.getOptionValue('t')));
  }
}
