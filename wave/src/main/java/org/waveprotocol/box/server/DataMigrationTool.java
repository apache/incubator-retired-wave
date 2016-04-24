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

package org.waveprotocol.box.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.waveprotocol.box.server.persistence.PersistenceModule;
import org.waveprotocol.box.server.persistence.migration.DeltaMigrator;
import org.waveprotocol.box.server.waveserver.DeltaStore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A cmd line utility to perform data migration from a store type to another
 * one. Initially developed to replicate deltas from a file store to a mongodb
 * store.
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class DataMigrationTool {

  private static void runDeltasMigration(Injector sourceInjector, Injector targetInjector) {

    // We can migrate data from-to any store type,
    // but it is not allowed migrate from-to the same type
    String sourceDeltaStoreType =
        sourceInjector
            .getInstance(Config.class).getString("core.delta_store_type");

    String targetDeltaStoreType =
        targetInjector
                .getInstance(Config.class).getString("core.delta_store_type");

    if (sourceDeltaStoreType.equalsIgnoreCase(targetDeltaStoreType))
      usageError("Source and Target Delta store types must be different");


    DeltaMigrator dm =
        new DeltaMigrator(sourceInjector.getInstance(DeltaStore.class),
            targetInjector.getInstance(DeltaStore.class));

    dm.run();

  }

  private static Module bindCmdLineSettings(String cmdLineProperties) {

    // Get settings from cmd line, e.g.
    // Key = delta_store_type
    // Value = mongodb
    final Map<String, String> propertyMap = new HashMap<>();

    for (String arg : cmdLineProperties.split(",")) {
      String[] argTokens = arg.split("=");
      propertyMap.put(argTokens[0], argTokens[1]);
    }

    return new AbstractModule() {

      @Override
      protected void configure() {
        Config config = ConfigFactory.load().withFallback(
          ConfigFactory.parseFile(new File("application.conf")).withFallback(
            ConfigFactory.parseFile(new File("reference.conf"))));
        bind(Config.class).toInstance(ConfigFactory.parseMap(propertyMap).withFallback(config));
      }
    };

  }

  public static void usageError() {
    usageError("");
  }

  public static void usageError(String msg) {
    System.out.println(msg + "\n");
    System.out.println("Use: DataMigrationTool <data type> <source options> <target options>\n");
    System.out.println("supported data types : deltas");
    System.out
        .println("source options example : core.delta_store_type=file," +
                   "core.delta_store_directory=_deltas");
    System.out
        .println("target options example : core.delta_store_type=mongodb," +
                   "core.mongodb_host=127.0.0.1,core.mongodb_port=27017,core.mongodb_database=wiab");
    System.exit(1);
  }

  public static void main(String... args) {

    if (args.length != 3) usageError();

    String dataType = args[0];

    Module sourceSettings = bindCmdLineSettings(args[1]);
    Injector sourceSettingsInjector = Guice.createInjector(sourceSettings);
    Module sourcePersistenceModule = sourceSettingsInjector.getInstance(PersistenceModule.class);
    Injector sourceInjector = sourceSettingsInjector.createChildInjector(sourcePersistenceModule);


    Module targetSettings = bindCmdLineSettings(args[2]);
    Injector targetSettingsInjector = Guice.createInjector(targetSettings);
    Module targetPersistenceModule = targetSettingsInjector.getInstance(PersistenceModule.class);
    Injector targetInjector = targetSettingsInjector.createChildInjector(targetPersistenceModule);


    if (dataType.equals("deltas")) {
      runDeltasMigration(sourceInjector, targetInjector);

    } else {
      usageError("Wrong data type");
    }


  }

}
