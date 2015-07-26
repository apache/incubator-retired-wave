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

package org.waveprotocol.wave.client.wavepanel.view.impl;


/**
 * Implements a structural view by delegating primitive state matters to an
 * intrinsic view object, and structural state matters to a helper. The intent
 * is that the helper is a flyweight handler.
 *
 * @param <I> intrinsic view implementation
 * @param <H> flyweight handler
 */
public abstract class AbstractStructuredView<H, I> {

  protected final H helper;
  protected final I impl;

  AbstractStructuredView(H helper, I impl) {
    this.helper = helper;
    this.impl = impl;
  }

  /** Reveals the intrinsic view object. */
  public final I getIntrinsic() {
    return impl;
  }

  //
  // Equality.
  //

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof AbstractStructuredView<?, ?>)) {
      return false;
    } else {
      return impl.equals(((AbstractStructuredView<?, ?>) obj).impl);
    }
  }

  @Override
  public final int hashCode() {
    return 37 + 11 * impl.hashCode();
  }
}
