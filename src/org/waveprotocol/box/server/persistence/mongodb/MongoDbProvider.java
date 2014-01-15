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

package org.waveprotocol.box.server.persistence.mongodb;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import org.waveprotocol.box.server.persistence.PersistenceStartException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Class to lazily setup and manage the MongoDb connection.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 *
 */
public class MongoDbProvider {
  private static final Logger LOG = Logger.getLogger(MongoDbProvider.class.getName());

  /** Location of the MongoDB properties file in the classpath. */
  private static final String PROPERTIES_LOC =
      "org/waveprotocol/box/server/persistence/mongodb/mongodb.properties";

  /** Name of the property that stores the host. */
  private static final String HOST_PROPERTY = "mongoDbHost";

  /** Name of the property that stores the port. */
  private static final String PORT_PROPERTY = "mongoDbPort";

  /** Name of the property that stores the name of the database. */
  private static final String DATABASE_NAME_PROPERTY = "mongoDbDatabase";

  /**
   * Our {@link MongoClient} instance, should be accessed by getMongo unless during
   * start().
   */
  private Mongo mongo;

  /**
   * Our lazily loaded {@link Properties} instance.
   */
  private Properties properties;

  /**
   * Lazily instantiated {@link MongoDbStore}.
   */
  private MongoDbStore mongoDbStore;

  /**
   * Separated store for Deltas {@link MongoDbDeltaStore}
   */
  private MongoDbDeltaStore mongoDbDeltaStore;
  
  /** Stores whether we have successfully setup a live {@link Mongo} instance. */
  private boolean isRunning;

  /**
   * Constructs a new empty {@link MongoDbProvider}.
   */
  public MongoDbProvider() {
  }

  /**
   * Starts the {@link Mongo} instance and explicitly checks whether it is
   * actually alive.
   *
   * @throws PersistenceStartException if we can't make a connection to MongoDb.
   */
  private void start() {
    Preconditions.checkState(!isRunning(), "Can't start after a connection has been established");

    ensurePropertiesLoaded();

    String host = properties.getProperty(HOST_PROPERTY);
    int port = Integer.parseInt(properties.getProperty(PORT_PROPERTY));
    try {
      // New MongoDB Client, see http://docs.mongodb.org/manual/release-notes/drivers-write-concern/
      mongo = new MongoClient(host, port);
    } catch (UnknownHostException e) {
      throw new PersistenceStartException("Unable to resolve the MongoDb hostname", e);
    }

    try {
      // Check to see if we are alive
      mongo.getDB(getDatabaseName()).command("ping");
    } catch (MongoException e) {
      throw new PersistenceStartException("Can't ping MongoDb", e);
    }

    isRunning = true;
    LOG.info("Started MongoDb persistence");
  }

  /**
   * Ensures that the properties for MongoDb are loaded.
   *
   * @throws PersistenceStartException if the properties can not be loaded.
   */
  private void ensurePropertiesLoaded() {
    if (properties != null) {
      // Already loaded
      return;
    }
    Properties properties = new Properties();
    try {
      properties.load(ClassLoader.getSystemResourceAsStream(PROPERTIES_LOC));
    } catch (IOException e) {
      throw new PersistenceStartException("Unable to load Properties for MongoDb", e);
    }
    this.properties = properties;
  }

  /**
   * Returns the {@link DB} with the name that is specified in the properties
   * file.
   */
  private DB getDatabase() {
    return getDatabaseForName(getDatabaseName());
  }

  /**
   * Returns the name of the database as specified in the properties file.
   */
  private String getDatabaseName() {
    ensurePropertiesLoaded();
    return properties.getProperty(DATABASE_NAME_PROPERTY);
  }

  /**
   * Returns a {@link DB} instance for the database with the given name
   *
   * @param name the name of the database
   */
  @VisibleForTesting
  DB getDatabaseForName(String name) {
    return getMongo().getDB(name);
  }

  /**
   * Return the {@link Mongo} instance that we are managing.
   */
  private Mongo getMongo() {
    if (!isRunning) {
      start();
    }
    return mongo;
  }

  /**
   * Returns true iff the {@link MongoDbProvider} is running.
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Returns a {@link MongoDbStore} instance created from the settings in this
   * provider.
   */
  public MongoDbStore provideMongoDbStore() {
    if (mongoDbStore == null) {
      mongoDbStore = new MongoDbStore(getDatabase());
    }
    return mongoDbStore;
  }
  
  /**
   * Returns a {@link MongoDbDeltaStore} instance created from the settings in this 
   * provider.
   */
  public MongoDbDeltaStore provideMongoDbDeltaStore() {
    if (mongoDbDeltaStore == null) {
      mongoDbDeltaStore = new MongoDbDeltaStore(getDatabase());
    }
    
    return mongoDbDeltaStore;
    
  }
  
}
