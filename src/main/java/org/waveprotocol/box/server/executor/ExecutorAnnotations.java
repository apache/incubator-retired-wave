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

package org.waveprotocol.box.server.executor;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Executor annotations.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public interface ExecutorAnnotations {

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface ClientServerExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface DeltaPersistExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface IndexExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface ListenerExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface LookupExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface StorageContinuationExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface WaveletLoadExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface ContactExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface RobotConnectionExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface RobotGatewayExecutor {
  }

  @Retention(RUNTIME)
  @BindingAnnotation
  public @interface XmppExecutor {
  }

  @BindingAnnotation
  public @interface SolrExecutor {
  }
}
