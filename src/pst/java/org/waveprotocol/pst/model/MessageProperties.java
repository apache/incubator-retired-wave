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

package org.waveprotocol.pst.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Container for the Properties of a message.
 *
 * @author kalman@google.com
 */
public final class MessageProperties {

  private static final String PACKAGE_SUFFIX = "package.suffix";
  private static final String FILE_EXTENSION = "file.extension";
  private static final String TEMPLATE_NAME = "template.name";

  private final Properties properties;
  private boolean useInt52;

  private MessageProperties(Properties properties) {
    this.properties = properties;
  }

  public static MessageProperties createFromFile(File propertiesFile) throws FileNotFoundException,
      IOException {
    Properties properties = new Properties();
    properties.load(new FileReader(propertiesFile));
    return new MessageProperties(properties);
  }

  public static MessageProperties createEmpty() {
    return new MessageProperties(new Properties());
  }

  /**
   * @return the package suffix, or null if one isn't specified.
   */
  public String getPackageSuffix() {
    return properties.getProperty(PACKAGE_SUFFIX);
  }

  /**
   * @return whether a package suffix has been specified.
   */
  public boolean hasPackageSuffix() {
    return getPackageSuffix() != null;
  }

  /**
   * @return the file extension, or null if it isn't specified.
   */
  public String getFileExtension() {
    return properties.getProperty(FILE_EXTENSION);
  }

  /**
   * @return whether a file extension has been specified.
   */
  public boolean hasFileExtension() {
    return getFileExtension() != null;
  }

  /**
   * @return the template name, or null if it isn't specified.
   */
  public String getTemplateName() {
    return properties.getProperty(TEMPLATE_NAME);
  }

  /**
   * @return whether a template name has been specified.
   */
  public boolean hasTemplateName() {
    return getTemplateName() != null;
  }

  /**
   * Sets the global int52 type property
   */
  public void setUseInt52(boolean useInt52) {
    this.useInt52 = useInt52;
  }

  /**
   * @return the int52 type or null if it isn't specified.
   */
  public boolean getUseInt52() {
    return useInt52;
  }
}
