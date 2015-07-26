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

package org.waveprotocol.box.server.robots.agent;

import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.appendLine;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.lastEnteredLineOf;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.wave.api.Blip;
import com.google.wave.api.event.DocumentChangedEvent;
import com.google.wave.api.event.WaveletSelfAddedEvent;

import com.typesafe.config.Config;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.waveprotocol.box.server.CoreSettingsNames;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.box.server.robots.register.RobotRegistrarImpl;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The base for robot agents that run on the WIAB server and interact with users
 * by entering commands as text in the blips.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
public abstract class AbstractCliRobotAgent extends AbstractBaseRobotAgent {

  /** The options for the command. */
  private final Options options;
  private final CommandLineParser parser;
  private final HelpFormatter helpFormatter;

  /**
   * Constructor. Initializes the agent to serve on the URI provided by
   * {@link #getRobotUri()} and ensures that the agent is registered in the
   * Account store.
   *
   * @param injector the injector instance.
   */
  public AbstractCliRobotAgent(Injector injector) {
    this(injector.getInstance(Key.get(String.class, Names.named(CoreSettingsNames.WAVE_SERVER_DOMAIN))),

         injector
            .getInstance(ServerFrontendAddressHolder.class), injector
            .getInstance(AccountStore.class), injector.getInstance(RobotRegistrarImpl.class),
        injector.getInstance(Config.class).getBoolean("security.enable_ssl"));
  }

  /**
   * Constructor. Initializes the agent to serve on the URI provided by
   * {@link #getRobotUri()} and ensures that the agent is registered in the
   * Account store.
   */
  AbstractCliRobotAgent(String waveDomain,
                        ServerFrontendAddressHolder frontendAddressHolder, AccountStore accountStore,
                        RobotRegistrar robotRegistrar, Boolean sslEnabled) {
    super(waveDomain, frontendAddressHolder, accountStore, robotRegistrar, sslEnabled);
    parser = new PosixParser();
    helpFormatter = new HelpFormatter();
    options = initOptions();
  }

  /**
   * Displays a short description when the robot is added to a wave.
   */
  @Override
  public void onWaveletSelfAdded(WaveletSelfAddedEvent event) {
    String robotAddress = event.getWavelet().getRobotAddress();
    // Display a short description.
    appendLine(event.getBlip(), "\n" + robotAddress + ": I am listening.\n" + getShortDescription()
        + "\nFor help type " + "\"" + getCommandName()
        + " -help\" on a new line and hit \"Enter\".");
  }

  @Override
  public void onDocumentChanged(DocumentChangedEvent event) {
    Blip blip = event.getBlip();
    String modifiedBy = event.getModifiedBy();
    CommandLine commandLine = null;
    try {
      commandLine = preprocessCommand(blip.getContent());
    } catch (IllegalArgumentException e) {
      appendLine(blip, e.getMessage());
    }
    if (commandLine != null) {
      if (commandLine.hasOption("help")
          // Or if only options.
          || (commandLine.getArgs().length - commandLine.getOptions().length <= 1)) {
        appendLine(blip, getFullDescription());
      } else {
        String robotMessage = maybeExecuteCommand(commandLine, modifiedBy);
        appendLine(blip, robotMessage);
      }
    }
  }

  /**
   * Validates and parses the input for the command.
   *
   * @param blipContent the blip contents.
   * @return the command line {@link CommandLine} object with parsed data from
   *         the blip contents or null in case the content doesn't contain a
   *         command.
   * @throws IllegalArgumentException if illegal arguments passed to the
   *         command.
   */
  protected CommandLine preprocessCommand(String blipContent) throws IllegalArgumentException {
    CommandLine commandLine = null;
    String lastLine = lastEnteredLineOf(blipContent);
    if (lastLine != null) {
      try {
        commandLine = parse(lastLine.split(" "));
      } catch (ParseException e) {
        throw new IllegalArgumentException(e);
      }
      String[] args = commandLine.getArgs();
      if (!args[0].equals(getCommandName())) {
        return null;
      }
      int argsNum = args.length - commandLine.getOptions().length - 1;
      // If there are only options in the command - then it is also invalid and
      // have to display usage anyway.
      if ((argsNum > 0)
          && (argsNum < getMinNumOfArguments() || argsNum > getMaxNumOfArguments())) {
        String message;
        if (getMinNumOfArguments() == getMaxNumOfArguments()) {
          message =
            String.format("Invalid number of arguments. Expected: %d , actual: %d %s",
                getMinNumOfArguments(), argsNum, getUsage());
        } else {
          message =
            String.format(
                "Invalid number of arguments. Expected between %d and %d, actual: %d. %s",
                getMinNumOfArguments(), getMaxNumOfArguments(), argsNum, getUsage());
        }
        throw new IllegalArgumentException(message);
      }
    }
    return commandLine;
  }

  @Override
  protected String getRobotProfilePageUrl() {
    return null;
  }

  /**
   * Returns the command options usage.
   */
  public String getUsage() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter pw = new PrintWriter(stringWriter);
    // HelpFormatter doesn't provide other ways to access defaultWidth, so we
    // forced to access it in a deprecated way.
    // TODO (user) Update this code to remove access of deprecated fields when
    // it will be possible.
    helpFormatter.printHelp(pw, helpFormatter.defaultWidth, getCommandName() + " "
        + getCmdLineSyntax() + " \n", null, options, helpFormatter.defaultLeftPad,
        helpFormatter.defaultDescPad, "", false);
    pw.flush();
    return stringWriter.toString();
  }

  /**
   * Initializes basic options. Override if more options needed.
   *
   * @return the command options.
   */
  protected Options initOptions() {
    // Create Options.
    Options options = new Options();
    // The robot has only "help" option.
    @SuppressWarnings({"static-access", "static"})
    Option help = OptionBuilder.withDescription("Displays help for the command.").create("help");
    options.addOption(help);
    return options;
  }

  protected CommandLine parse(String... args) throws ParseException {
    return getParser().parse(getOptions(), args);
  }

  /**
   * Returns the command line parser.
   */
  protected CommandLineParser getParser() {
    return parser;
  }

  /**
   * Returns the command options.
   */
  protected Options getOptions() {
    return options;
  }

  /**
   * Attempts to execute the command.
   *
   * @param commandLine the commandLine with arguments and/or options entered by
   *        the user.
   * @param modifiedBy the user that entered the content.
   * @return the result message: success or failure.
   */
  protected abstract String maybeExecuteCommand(CommandLine commandLine, String modifiedBy);

  /**
   * Returns the short description of the robot.
   */
  public abstract String getShortDescription();

  /**
   * Returns the full robot description.
   */
  public abstract String getFullDescription();

  /**
   * Returns the command name for the robot.
   */
  public abstract String getCommandName();

  /**
   * Returns the command line syntax.
   */
  public abstract String getCmdLineSyntax();

  /**
   * Returns the command use example.
   */
  public abstract String getExample();

  /**
   * Returns the minimum number of arguments this command accepts. Should be
   * greater than zero and less or equal to {@link #getMaxNumOfArguments()}.
   */
  public abstract int getMinNumOfArguments();

  /**
   * Returns the maximum number of arguments this command accepts.
   */
  public abstract int getMaxNumOfArguments();
}
