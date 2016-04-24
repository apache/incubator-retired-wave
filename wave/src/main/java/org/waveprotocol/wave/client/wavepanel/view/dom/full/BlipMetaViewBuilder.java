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
package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.nonNull;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.closeSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.image;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.open;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openSpanWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.BlipMessages;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 */
public final class BlipMetaViewBuilder implements UiBuilder, IntrinsicBlipMetaView {


  /** An enum for all the components of a blip view. */
  public enum Components implements Component {
    /** The avatar element. */
    AVATAR("A"),
    /** The text inside the information bar. */
    METALINE("M"),
    /** The element for the information bar. */
    METABAR("B"),
    /** The element containing the time text. */
    TIME("T"),
    /** The element containing the document. */
    CONTENT("C"),
    /** The element containing menu options. */
    MENU("N"), ;

    private final String postfix;

    Components(String postfix) {
      this.postfix = postfix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + postfix;
    }
  }

  // The consistent iterator ordering of EnumMap is relied upon, to ensure that
  // the same menu options are always rendered in the same order.
  private final static Map<MenuOption, SafeHtml> MENU_CODES =
      new EnumMap<MenuOption, SafeHtml>(MenuOption.class);

  private final static Map<MenuOption, SafeHtml> MENU_LABELS =
      new EnumMap<MenuOption, SafeHtml>(MenuOption.class);

  private final static StringMap<MenuOption> MENU_OPTIONS = CollectionUtils.createStringMap();

  public static final String OPTION_ID_ATTRIBUTE = "o";
  public static final String OPTION_SELECTED_ATTRIBUTE = "s";
  private static final EnumSet<MenuOption> MENU_OPTIONS_BEFORE_EDITING = EnumSet.of(
      IntrinsicBlipMetaView.MenuOption.REPLY, IntrinsicBlipMetaView.MenuOption.DELETE,
      IntrinsicBlipMetaView.MenuOption.EDIT,
      IntrinsicBlipMetaView.MenuOption.LINK);
  public final static Set<MenuOption> ENABLED_WHILE_EDITING_MENU_OPTIONS_SET = EnumSet.of(
      IntrinsicBlipMetaView.MenuOption.EDIT_DONE);
  public final static Set<MenuOption> DISABLED_WHILE_EDITING_MENU_OPTIONS_SET = MENU_OPTIONS_BEFORE_EDITING;

  /**
   * A unique id for this builder.
   */
  private final String id;
  private final BlipViewBuilder.Css css;

  //
  // Intrinsic state.
  //

  private String time;
  private String metaline;
  private String avatarUrl;
  private boolean read = true;
  private final Set<MenuOption> options = MENU_OPTIONS_BEFORE_EDITING;
  private final Set<MenuOption> selected = EnumSet.noneOf(MenuOption.class);

  //
  // Structural components.
  //

  private final UiBuilder content;

  /**
   * Creates a new blip view builder with the given id.
   *
   * @param id unique id for this builder, it must only contains alphanumeric
   *        characters
   */
  public static BlipMetaViewBuilder create(String id, UiBuilder content) {
    return new BlipMetaViewBuilder(WavePanelResourceLoader.getBlip().css(),
        WavePanelResourceLoader.getBlipMessages(), id, nonNull(content));
  }

  @VisibleForTesting
  BlipMetaViewBuilder(BlipViewBuilder.Css css, BlipMessages messages, String id, UiBuilder content) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    this.css = css;
    this.id = id;
    this.content = content;
    buildMenuModel(messages);
  }

  @Override
  public void setAvatar(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  @Override
  public void setTime(String time) {
    this.time = time;
  }

  @Override
  public void setMetaline(String metaline) {
    this.metaline = metaline;
  }

  @Override
  public void setRead(boolean read) {
    this.read = read;
  }

  @Override
  public void enable(Set<MenuOption> options) {
    this.options.addAll(options);
  }

  @Override
  public void disable(Set<MenuOption> options) {
    this.options.removeAll(options);
    this.selected.removeAll(options);
  }

  @Override
  public void select(MenuOption option) {
    this.selected.add(option);
  }

  @Override
  public void deselect(MenuOption option) {
    this.selected.remove(option);
  }

  //
  // DomImpl nature.
  //

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    // HACK HACK HACK
    // This code should be automatically generated from UiBinder template, not
    // hand written.

    open(output, id, css.meta(), TypeCodes.kind(Type.META));
    {
      // Author avatar.
      image(output, Components.AVATAR.getDomId(id), css.avatar(), EscapeUtils.fromString(avatarUrl),
          EscapeUtils.fromPlainText("author"), null);

      // Metabar.
      open(output, Components.METABAR.getDomId(id),
          css.metabar() + " " + (read ? css.read() : css.unread()), null);
      {
        open(output, Components.MENU.getDomId(id), css.menu(), null);
        menuBuilder(options, selected, css).outputHtml(output);
        close(output);

        // Time.
        open(output, Components.TIME.getDomId(id), css.time(), null);
        if (time != null) {
          output.appendEscaped(time);
        }
        close(output);

        // Metaline.
        open(output, Components.METALINE.getDomId(id), css.metaline(), null);
        if (metaline != null) {
          output.appendEscaped(metaline);
        }
        close(output);
      }
      close(output);

      // Content.
      open(output, Components.CONTENT.getDomId(id), css.contentContainer(), "document");
      content.outputHtml(output);
      close(output);
    }
    close(output);
  }

  /**
   * Creates a builder for a blip menu.
   */
  public static UiBuilder menuBuilder(final Set<MenuOption> options, final Set<MenuOption> selected,
      final BlipViewBuilder.Css css) {
    return new UiBuilder() {
      @Override
      public void outputHtml(SafeHtmlBuilder out) {
        for (MenuOption option : options) {
          out.append(EscapeUtils.fromSafeConstant("|"));
          String style = selected.contains(option) //
              ? css.menuOption() + css.menuOptionSelected() : css.menuOption();
          String extra = OPTION_ID_ATTRIBUTE + "='" + MENU_CODES.get(option).asString() + "'"
              + (selected.contains(option) ? " " + OPTION_SELECTED_ATTRIBUTE + "='s'" : "");
          openSpanWith(out, null, style, TypeCodes.kind(Type.MENU_ITEM), extra);
          out.append(MENU_LABELS.get(option));
          closeSpan(out);
        }
      }
    };
  }

  public static MenuOption getMenuOption(String id) {
    MenuOption option = MENU_OPTIONS.get(id);
    if (option == null) {
      throw new IllegalArgumentException("No such option: " + id);
    }
    return option;
  }

  public static SafeHtml getMenuOptionId(MenuOption option) {
    SafeHtml code = MENU_CODES.get(option);
    if (code == null) {
      throw new IllegalArgumentException("No such option: " + option);
    }
    return code;
  }

  private static void buildMenuModel(BlipMessages messages) {
    MENU_CODES.put(MenuOption.EDIT, EscapeUtils.fromSafeConstant("e"));
    MENU_CODES.put(MenuOption.EDIT_DONE, EscapeUtils.fromSafeConstant("x"));
    MENU_CODES.put(MenuOption.REPLY, EscapeUtils.fromSafeConstant("r"));
    MENU_CODES.put(MenuOption.DELETE, EscapeUtils.fromSafeConstant("d"));
    MENU_CODES.put(MenuOption.LINK, EscapeUtils.fromSafeConstant("l"));
    MENU_LABELS.put(MenuOption.EDIT, EscapeUtils.fromSafeConstant(messages.edit()));
    MENU_LABELS.put(MenuOption.EDIT_DONE, EscapeUtils.fromSafeConstant(messages.done()));
    MENU_LABELS.put(MenuOption.REPLY, EscapeUtils.fromSafeConstant(messages.reply()));
    MENU_LABELS.put(MenuOption.DELETE, EscapeUtils.fromSafeConstant(messages.delete()));
    MENU_LABELS.put(MenuOption.LINK, EscapeUtils.fromSafeConstant(messages.link()));
    for (MenuOption option : MENU_CODES.keySet()) {
      MENU_OPTIONS.put(MENU_CODES.get(option).asString(), option);
    }
    assert MENU_CODES.keySet().equals(MENU_LABELS.keySet());
    assert MENU_OPTIONS.countEntries() == MENU_CODES.size();
    assert new HashSet<MenuOption>(Arrays.asList(MenuOption.values())).equals(MENU_LABELS.keySet());
  }
}
