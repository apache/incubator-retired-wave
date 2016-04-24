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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

/**
 * Icons for the editor toolbar.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public interface EditorToolbarResources extends ClientBundle {
  interface Css extends CssResource {
    String color();
    String backcolor();
    String bold();
    String italic();
    String underline();
    String strikethrough();
    String superscript();
    String subscript();
    String fontSize();
    String fontFamily();
    String heading();
    String indent();
    String outdent();
    String unorderedlist();
    String orderedlist();
    String alignDrop();
    String alignLeft();
    String alignCentre();
    String alignRight();
    String alignJustify();
    String clearFormatting();
    String insertLink();
    String removeLink();
    String insertGadget();
    String insertAttachment();
  }

  @Source("images/edit/backcolor.png") ImageResource backcolor();
  @Source("images/edit/color.png") ImageResource color();
  @Source("images/edit/bold.png") ImageResource bold();
  @Source("images/edit/italic.png") ImageResource italic();
  @Source("images/edit/underline.png") ImageResource underline();
  @Source("images/edit/strike.png") ImageResource strikethrough();
  @Source("images/edit/icon_superscript.png") ImageResource superscript();
  @Source("images/edit/icon_subscript.png") ImageResource subscript();
  @Source("images/edit/icon_font_size.png") ImageResource fontSize();
  @Source("images/edit/icon_heading.png") ImageResource heading();
  @Source("images/edit/font_style.png") ImageResource fontFamily();
  @Source("images/edit/indent.png") ImageResource indent();
  @Source("images/edit/outdent.png") ImageResource outdent();
  @Source("images/edit/unordered_list.png") ImageResource unorderedlist();
  @Source("images/edit/ordered_list.png") ImageResource orderedlist();
  @Source("images/edit/icon_align.png") ImageResource alignDrop();
  @Source("images/edit/icon_align_left.png") ImageResource alignLeft();
  @Source("images/edit/icon_align_centre.png") ImageResource alignCentre();
  @Source("images/edit/icon_align_right.png") ImageResource alignRight();
  @Source("images/edit/icon_align_justify.png") ImageResource alignJustify();
  @Source("images/edit/remove_formatting.png") ImageResource clearFormatting();
  @Source("images/edit/createLink.png") ImageResource insertLink();
  @Source("images/edit/removeLink.png") ImageResource removeLink();
  @Source("images/edit/gadget.png") ImageResource insertGadget();
  @Source("images/edit/attachment.png") ImageResource insertAttachment();

  @Source("EditToolbar.css")
  Css css();

  class Loader {
    final static EditorToolbarResources res = GWT.create(EditorToolbarResources.class);

    static {
      StyleInjector.inject(res.css().getText(), true);
    }
  }
}
