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

package org.waveprotocol.wave.client.widget.toolbar;

import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarButtonView;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarClickButton;
import org.waveprotocol.wave.client.widget.toolbar.buttons.ToolbarToggleButton;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.List;

/**
 * Tests for {@link GroupingToolbar}.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */

public final class GroupingToolbarTest extends TestCase {

  /**
   * A fake item to use in {@link PojoToolbar}.
   */
  private interface FakeItem extends ToolbarButtonView {}

  /**
   * An in-memory fake toolbar.
   */
  private static final class PojoToolbar implements GroupingToolbar.View {
    private final List<ToolbarButtonView> items = CollectionUtils.newArrayList();

    public List<ToolbarButtonView> getDisplayedItems() {
      List<ToolbarButtonView> displayedItems = CollectionUtils.newArrayList();
      for (ToolbarButtonView item : items) {
        if (!(item instanceof FakeItem)) {
          displayedItems.add(item);
        }
      }
      return displayedItems;
    }

    @Override
    public int indexOf(ToolbarButtonView button) {
      return items.indexOf(button);
    }

    @Override
    public ToolbarClickButton insertClickButton(int beforeIndex) {
      ToolbarClickButton button = mock(ToolbarClickButton.class);
      items.add(beforeIndex, button);
      return button;
    }

    @Override
    public SubmenuToolbarView insertSubmenu(int beforeIndex) {
      SubmenuToolbarView submenu = mock(SubmenuToolbarView.class);
      items.add(beforeIndex, submenu);
      return submenu;
    }

    @Override
    public ToolbarToggleButton insertToggleButton(int beforeIndex) {
      ToolbarToggleButton button = mock(ToolbarToggleButton.class);
      items.add(beforeIndex, button);
      return button;
    }

    @Override
    public ToolbarClickButton addClickButton() {
      ToolbarClickButton button = mock(ToolbarClickButton.class);
      items.add(button);
      return button;
    }

    @Override
    public ToolbarView addGroup() {
      return new GroupingToolbar(this, addFakeItem());
    }

    private ToolbarButtonView addFakeItem() {
      FakeItem fakeItem = mock(FakeItem.class);
      items.add(fakeItem);
      return fakeItem;
    }

    @Override
    public SubmenuToolbarView addSubmenu() {
      SubmenuToolbarView submenu = mock(SubmenuToolbarView.class);
      items.add(submenu);
      return submenu;
    }

    @Override
    public ToolbarToggleButton addToggleButton() {
      ToolbarToggleButton button = mock(ToolbarToggleButton.class);
      items.add(button);
      return button;
    }

    @Override
    public void clearItems() {
      items.clear();
    }
  }

  private PojoToolbar toolbar;

  @Override
  public void setUp() {
    toolbar = new PojoToolbar();
  }

  /**
   * Tests that a single group has the buttons in the correct order.
   */
  public void testSingleGroup() {
    ToolbarView group = toolbar.addGroup();
    ToolbarButtonView button1 = group.addClickButton();
    ToolbarButtonView button2 = group.addToggleButton();
    ToolbarButtonView button3 = group.addSubmenu();
    assertEquals(toolbar.getDisplayedItems(), ImmutableList.of(button1, button2, button3));
  }

  /**
   * Tests that a single group surrounded by non-grouped buttons are in the
   * correct order.
   */
  public void testSingleGroupWithOtherButtons() {
    ToolbarButtonView button1 = toolbar.addClickButton();
    ToolbarView group = toolbar.addGroup();
    ToolbarButtonView button2 = group.addClickButton();
    ToolbarButtonView button3 = group.addToggleButton();
    ToolbarButtonView button4 = group.addSubmenu();
    ToolbarButtonView button5 = toolbar.addToggleButton();
    assertEquals(
        toolbar.getDisplayedItems(), ImmutableList.of(button1, button2, button3, button4, button5));
  }

  /**
   * Tests that multiple singleton groups have the buttons in the right order.
   */
  public void testMultipleSingletonGroups() {
    ToolbarButtonView button1 = toolbar.addGroup().addClickButton();
    ToolbarButtonView button2 = toolbar.addGroup().addToggleButton();
    ToolbarButtonView button3 = toolbar.addGroup().addSubmenu();
    assertEquals(toolbar.getDisplayedItems(), ImmutableList.of(button1, button2, button3));
  }

  /**
   * Tests that multiple adjacent groups have the buttons in the correct order
   * when populated sequentially.
   */
  public void testMultipleSequentialGroups() {
    ToolbarView group1 = toolbar.addGroup();
    ToolbarView group2 = toolbar.addGroup();
    ToolbarView group3 = toolbar.addGroup();

    ToolbarButtonView button11 = group1.addClickButton();
    ToolbarButtonView button12 = group1.addToggleButton();
    ToolbarButtonView button13 = group1.addSubmenu();

    ToolbarButtonView button21 = group2.addClickButton();
    ToolbarButtonView button22 = group2.addToggleButton();
    ToolbarButtonView button23 = group2.addSubmenu();

    ToolbarButtonView button31 = group3.addClickButton();
    ToolbarButtonView button32 = group3.addToggleButton();
    ToolbarButtonView button33 = group3.addSubmenu();

    assertEquals(toolbar.getDisplayedItems(), ImmutableList.of(
        button11,
        button12,
        button13,
        button21,
        button22,
        button23,
        button31,
        button32,
        button33));
  }

  /**
   * Tests that multiple adjacent groups have the buttons in the correct order
   * when populated "horizontally" (so to speak).
   */
  public void testMultipleHorizontalGroups() {
    ToolbarView group1 = toolbar.addGroup();
    ToolbarView group2 = toolbar.addGroup();
    ToolbarView group3 = toolbar.addGroup();

    ToolbarButtonView button11 = group1.addClickButton();
    ToolbarButtonView button21 = group2.addClickButton();
    ToolbarButtonView button31 = group3.addClickButton();

    ToolbarButtonView button12 = group1.addToggleButton();
    ToolbarButtonView button22 = group2.addToggleButton();
    ToolbarButtonView button32 = group3.addToggleButton();

    ToolbarButtonView button13 = group1.addSubmenu();
    ToolbarButtonView button23 = group2.addSubmenu();
    ToolbarButtonView button33 = group3.addSubmenu();

    assertEquals(toolbar.getDisplayedItems(), ImmutableList.of(
        button11,
        button12,
        button13,
        button21,
        button22,
        button23,
        button31,
        button32,
        button33));
  }
}
