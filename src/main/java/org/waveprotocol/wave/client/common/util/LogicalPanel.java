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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * A panel that can adopt and orphan widgets that attach and detach themselves
 * from existing elements.
 *
 */
public interface LogicalPanel {
  //
  // These names are intentionally different from Panel's adopt() and orphan()
  // methods, because those methods are protected.
  //

  /**
   * Logically attaches a widget to this panel, without any physical DOM change.
   * No assumptions are made about the physical location of {@code child}.
   *
   * @param child widget to adopt
   */
  void doAdopt(Widget child);

  /**
   * Logically detaches a child from this panel, without any physical DOM
   * change. No assumptions are made about the physical location of {@code child}.
   *
   * @param child widget to orphan
   */
  void doOrphan(Widget child);

  /** Canonical implementation */
  public abstract class Impl extends ComplexPanel implements LogicalPanel {
    @Override
    public void doAdopt(Widget child) {
      Preconditions.checkArgument(child != null && child.getParent() == null, "Not an orphan");
      getChildren().add(child);
      adopt(child);
    }

    @Override
    public void doOrphan(Widget child) {
      Preconditions.checkArgument(child != null && child.getParent() == this, "Not a child");
      orphan(child);
      getChildren().remove(child);
    }
  }
}
