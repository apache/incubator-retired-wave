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

package org.waveprotocol.wave.client.editor.content.paragraph;

import org.waveprotocol.wave.client.editor.content.HasImplNodelets;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Alignment;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Direction;

/**
 * Interface for updating HTML as a result of rendering logic decisions.
 *
 * Note that currently this is independent of ParagraphHelper.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ParagraphHtmlRenderer extends Renderer {

  /**
   * Update most of the rendering
   *
   * @param element
   * @param type paragraph type
   * @param listStyle only meaningful for list paragraphs. e.g. "decimal". can be null.
   * @param indent indentation level
   * @param alignment text alignment
   * @param direction text direction (e.g. rtl)
   */
  public void updateRendering(HasImplNodelets element,
      String type, String listStyle, int indent,
      Alignment alignment, Direction direction);

  /**
   * Update the list item number, e.g. for decimal lists.
   *
   * The other attributes are sort of inter-dependent, but this one is updated
   * by completely separate logic - so no need to call updateRendering code.
   *
   * @param element
   * @param value
   */
  public void updateListValue(HasImplNodelets element, int value);
}
