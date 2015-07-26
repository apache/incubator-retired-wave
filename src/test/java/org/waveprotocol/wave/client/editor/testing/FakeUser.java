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

package org.waveprotocol.wave.client.editor.testing;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor.SelectionSource;
import org.waveprotocol.wave.client.editor.impl.HtmlView;

import org.waveprotocol.wave.model.document.util.Point;

/**
 * Simulates the behaviour of a browser updating the DOM due to a user typing
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FakeUser implements SelectionSource{

  public enum Action {
    MOVE,
    TYPE,
    DELETE,
    BACKSPACE,
    SPLIT
  }

  private final HtmlView htmlView;

  private Point<Node> caret;

  public FakeUser(HtmlView htmlView) {
    this.htmlView = htmlView;
  }

  @SuppressWarnings("unchecked")  // NOTE(zdwang): This is for (Point<Node>) action[1]
  public void run(TypingExtractor extractor, Object... actions) throws HtmlMissing, HtmlInserted {
    for (Object a : actions) {
      Object[] action = (Object[]) a;
      Point<Node> sel = getSelectionStart();
      Text txt = sel == null ? null : sel.getContainer().<Text>cast();
      Action type = (Action) action[0];
      switch (type) {
        case MOVE:
          //extractor.flush();
          setCaret((Point<Node>) action[1]);
          break;
        case TYPE:
          String typed = (String) action[1];
          extractor.somethingHappened(getSelectionStart());
          if (sel.isInTextNode()) {
            txt.insertData(sel.getTextOffset(), (String) action[1]);
            moveCaret(((String) action[1]).length());
          } else {
            txt = Document.get().createTextNode(typed);
            sel.getContainer().insertBefore(txt, sel.getNodeAfter());
            setCaret(Point.inText((Node)txt, typed.length()));
          }
          break;
        case BACKSPACE:
        case DELETE:
          extractor.somethingHappened(getSelectionStart());
          int amount = (Integer) action[1];
          if (type == Action.BACKSPACE) {
            moveCaret(-amount);
          }
          deleteText(amount);
          break;
        case SPLIT:
          sel.getContainer().<Text>cast().splitText(sel.getTextOffset());
          moveCaret(0);
          break;
      }
    }
  }

  private void deleteText(int amount) {
    Point<Node> sel = getSelectionStart();
    Text txt = sel == null ? null : sel.getContainer().<Text>cast();
    int startIndex = sel.getTextOffset(), len;
    while (amount > 0) {
      if (txt == null || !DomHelper.isTextNode(txt)) {
        throw new RuntimeException("Action ran off end of text node");
      }
      String data = txt.getData();
      int remainingInNode = data.length() - startIndex;
      if (remainingInNode >= amount) {
        len = amount;
      } else {
        len = remainingInNode;
      }
      txt.setData(data.substring(0, startIndex) + data.substring(startIndex + len));
      amount -= len;
      startIndex = 0;
      txt = htmlView.getNextSibling(txt).cast();
    }
    moveCaret(0);
  }

  public void moveCaret(int distance) {
    Point<Node> caret = getSelectionStart();
    if (!caret.isInTextNode()) {
      Node before = Point.nodeBefore(htmlView, caret.asElementPoint());
      if (DomHelper.isTextNode(before)) {
        caret = Point.inText(before, before.<Text>cast().getLength());
      } else if (DomHelper.isTextNode(caret.getNodeAfter())) {
        caret = Point.inText(caret.getNodeAfter(), 0);
      } else {
        throw new RuntimeException("Unimplemented/Invalid");
      }
    }
    Text nodelet = caret.getContainer().cast();
    int offset = caret.getTextOffset() + distance;
    while (offset < 0) {
      nodelet = htmlView.getPreviousSibling(nodelet).cast();
      if (nodelet == null || !DomHelper.isTextNode(nodelet)) {
        throw new RuntimeException("Action ran off end of text node");
      }
      offset += nodelet.getLength();
    }
    while (offset > nodelet.getLength()) {
      offset -= nodelet.getLength();
      nodelet = htmlView.getPreviousSibling(nodelet).cast();
      if (nodelet == null || !DomHelper.isTextNode(nodelet)) {
        throw new RuntimeException("Action ran off end of text node");
      }
    }
    setCaret(Point.inText((Node)nodelet, offset));
  }

  private void setCaret(Point<Node> point) {
    caret = point;
  }

  public static Object move(Point<Node> caret) {
    return new Object[]{Action.MOVE, caret, caret};
  }

//TODO(danilatos): Support non-collapsed selections
//  public static Object move(Point<Node> start, Point<Node> end) {
//    return new Object[]{Action.MOVE, start, end};
//  }

  public static Object type(String text) {
    return new Object[]{Action.TYPE, text};
  }

  public static Object del() {
    return del(1);
  }

  public static Object del(int len) {
    return new Object[]{Action.DELETE, len};
  }

  public static Object bksp() {
    return bksp(1);
  }

  public static Object bksp(int len) {
    return new Object[]{Action.BACKSPACE, len};
  }

  public static Object split() {
    return new Object[]{Action.SPLIT};
  }

  @Override
  public Point<Node> getSelectionEnd() {
    return caret;
  }

  @Override
  public Point<Node> getSelectionStart() {
    return caret;
  }
}
