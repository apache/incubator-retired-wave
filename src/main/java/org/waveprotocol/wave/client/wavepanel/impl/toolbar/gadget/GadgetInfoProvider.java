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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import java.util.List;

public interface GadgetInfoProvider {
  public interface Listener {
    void onUpdate();
  }

  public enum GadgetCategoryType {
    ALL("All"), GAME("Game"), IMAGE("Image"), MAP("Map"), MUSIC("Music"), PRODUCTIVITY(
        "Productivity"), SEARCH("Search"), TEAM("Team"), TIME("Time"), TRAVEL("Travel"), UTILITY(
        "Utility"), VIDEO("Video"), VOICE("Voice"), VOTING("Voting"), OTHER("Other"), ;

    private final String type;

    private GadgetCategoryType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }

    public static GadgetCategoryType of(String type) {
      for (GadgetCategoryType g : GadgetCategoryType.values()) {
        if (type.equalsIgnoreCase((g.getType()))) {
          return g;
        }
      }
      return ALL;
    }
  }

  public static class GadgetInfo {
    private final String name;
    private final String description;
    private final GadgetCategoryType primaryCategory;
    private final GadgetCategoryType secondaryCategory;
    private final String gadgetUrl;
    private final String author;
    private final String submittedBy;
    private final String imageUrl;

    public GadgetInfo(String name, String description, GadgetCategoryType primaryCategory,
        GadgetCategoryType secondaryCategory, String gadgetUrl, String author, String submittedBy,
        String imageUrl) {
      this.name = name;
      this.description = description;
      this.primaryCategory = primaryCategory;
      this.secondaryCategory = secondaryCategory;
      this.gadgetUrl = gadgetUrl;
      this.author = author;
      this.submittedBy = submittedBy;
      this.imageUrl = imageUrl;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public GadgetCategoryType getPrimaryCategory() {
      return primaryCategory;
    }

    public GadgetCategoryType getSecondaryCategory() {
      return secondaryCategory;
    }

    public String getGadgetUrl() {
      return gadgetUrl;
    }

    public String getAuthor() {
      return author;
    }

    public String getSubmittedBy() {
      return submittedBy;
    }

    public String getImageUrl() {
      return imageUrl;
    }
  }

  /**
   * Returns a filtered list of gadget info. The gadgets are filtered both on
   * their name and, their description, their primary and secondary category.
   *
   * @param filter the string to filter gadget. No string filtering is done if
   *        empty string is provided.
   * @param category the category to filter the primary or secondary category
   *        group on.
   * @return the filtered gadget list.
   */
  public abstract List<GadgetInfo> getGadgetInfoList(String filter, String category);

  /**
   * Start the loading of the gadget list. Notifies a Listener through the
   * onUpdate() method when the loading is done.
   */
  public abstract void startLoadingGadgetList();

  /**
   * Set the listener to be called when the loading of the gadget list is
   * completed.
   *
   * @param listener the listener that will be called when the gadget list is
   *        loaded.
   */
  public abstract void setListener(Listener listener);
}
