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

package org.apache.wave.pst;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Exception caused by any errors caused in code generation.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class PstException extends Exception {

  public static final class TemplateException extends Exception {
    private final String templateName;

    public TemplateException(String templateName, String message, Throwable cause) {
      super(message, cause);
      this.templateName = templateName;
    }

    public TemplateException(String templateName, Throwable cause) {
      super(cause);
      this.templateName = templateName;
    }

    /**
     * @return the name of the template being parsed when the exception occurred
     */
    public String getTemplateName() {
      return templateName;
    }
  }

  private final ImmutableList<TemplateException> exceptions;

  public PstException(List<TemplateException> exceptions) {
    super();
    this.exceptions = ImmutableList.copyOf(exceptions);
  }

  /**
   * @return all exceptions caused
   */
  public ImmutableList<TemplateException> getTemplateExceptions() {
    return exceptions;
  }
}
