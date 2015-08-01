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

package org.waveprotocol.wave.client.editor.selection.content;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;

import org.waveprotocol.wave.model.document.util.FilterProduct;
import org.waveprotocol.wave.model.document.util.FilterProduct.SkipStrategy;
import org.waveprotocol.wave.model.document.util.FilteredView;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

/**
 * Skipping strategy for identifying valid cursor containers in conversational wave blips.
 * This is to be used for the product of the persistent filter, and the rendered filter.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class ValidSelectionStrategy implements SkipStrategy<ContentNode> {
  // defines assumed order skip levels are passed to the resolver
  private static final int PERSISTENT_INDEX = 0;
  private static final int RENDERED_INDEX = 1;

  // NOTE(patcoleman) - the intent is for these to be extensible as various tags/doodads
  //   are developed and have their own selection behaviour.

  /** Tag names for local elements where the cursor can be placed. */
  private static final StringSet UNSKIPPED_TRANSIENT_CONTAINERS =
      CollectionUtils.createStringSet();

  /** Tag names for local element containing elts where the cursor can be placed (shallow skip) */
  private static final StringSet SHALLOW_SKIPPED_TRANSIENT_CONTAINERS =
      CollectionUtils.createStringSet();

  /** Tag names for persistent elts where the cursor can't placed, but can be placed in children. */
  private static final StringSet SHALLOW_SKIPPED_PERSISTENT_CONTAINERS =
      CollectionUtils.createStringSet();

  /** Tag names for persistent elements where the cursor can't placed at all. */
  private static final StringSet DEEP_SKIPPED_PERSISTENT_CONTAINERS =
      CollectionUtils.createStringSet();

  /** Static registry for tag names, doodads can call this to assign their selection status. */
  public static void registerTagForSelections(String tag, boolean isPersistent, Skip skipLevel) {
    switch (skipLevel) {
      case NONE:
        if (!isPersistent) { // persistent unskipped by default
          UNSKIPPED_TRANSIENT_CONTAINERS.add(tag);
        }
        break;
      case SHALLOW:
        if (isPersistent) {
          SHALLOW_SKIPPED_PERSISTENT_CONTAINERS.add(tag);
        } else {
          SHALLOW_SKIPPED_TRANSIENT_CONTAINERS.add(tag);
        }
        break;
      case DEEP:
        if (isPersistent) { // transient deep skip by default
          DEEP_SKIPPED_PERSISTENT_CONTAINERS.add(tag);
        }
        break;
    }
  }

  /**
   * Create a selection view out of the filter product of a persistent view, and a rendered view.
   * @param persistentFilter Persistent view of the document
   * @param renderedFilter Rendered view of the document
   * @return Selection filtered view of the document.
   */
  @SuppressWarnings({"unchecked", "rawtypes"}) // TODO(patcoleman) Deal with varargs + generics
  public static FilteredView<ContentNode, ContentElement, ContentTextNode> buildSelectionFilter(
      FilteredView<ContentNode, ContentElement, ContentTextNode> persistentFilter,
      FilteredView<ContentNode, ContentElement, ContentTextNode> renderedFilter) {
    return new FilterProduct(new ValidSelectionStrategy(), persistentFilter, renderedFilter);
  }

  /** Private constructor, build through the static factory instead to ensure correct product. */
  private ValidSelectionStrategy() {}

  @Override
  public Skip resolveSkip(ContentNode node, Skip[] skipLevels) {
    ContentElement elt = node.asElement();
    if (elt == null) {
      return Skip.NONE; // selection ok in text node
    }

    boolean isRendered = (skipLevels[RENDERED_INDEX] == Skip.NONE);

    switch (skipLevels[PERSISTENT_INDEX]) {
      case NONE:
        // persistent, so include if rendered, or not blacklisted
        return isRendered ? Skip.NONE : persistentForExclusion(elt);
      case SHALLOW:
      case DEEP:
        // transient, so only allow selection here if whitelisted
        return transientForInclusion(elt);
    }
    return Skip.INVALID;
  }

  // whitelist of non-persistent nodes that can be used for selection
  private Skip transientForInclusion(ContentElement elt) {
    String tagName = elt.getTagName();
    if(UNSKIPPED_TRANSIENT_CONTAINERS.contains(tagName)) {
      return Skip.NONE;
    } else if (SHALLOW_SKIPPED_TRANSIENT_CONTAINERS.contains(tagName)) {
      return Skip.SHALLOW;
    } else {
      return Skip.DEEP;
    }
  }

  // blacklist of persistent nodes that cannot be used for selection
  private Skip persistentForExclusion(ContentElement elt) {
    String tagName = elt.getTagName();
    if(SHALLOW_SKIPPED_PERSISTENT_CONTAINERS.contains(tagName)) {
      return Skip.SHALLOW;
    } else if (DEEP_SKIPPED_PERSISTENT_CONTAINERS.contains(tagName)) {
      return Skip.DEEP;
    } else {
      return Skip.NONE;
    }
  }
}
