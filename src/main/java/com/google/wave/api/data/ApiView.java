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

package com.google.wave.api.data;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.Gadget;
import com.google.wave.api.Line;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Doc.T;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.List;

/**
 * Class to represent a document in api view.
 *
 *
 */
public class ApiView {

  /**
   * Simple class to export info about elements in the ApiView.
   */
  public static class ElementInfo {
    public final Element element;
    public final int apiPosition;
    public final int xmlPosition;

    public ElementInfo(Element element, int apiPosition, int xmlPosition) {
      this.element = element;
      this.apiPosition = apiPosition;
      this.xmlPosition = xmlPosition;
    }
  }

  /**
   * Storage class to store a bit of the view. It's more a struct than a class.
   * Either the content or the element field is set.
   */
  private static class Bit {
    public String string;
    public Element element;
    private int xmlPos;
    private int xmlSize;

    Bit(Element element, int xmlPos, int xmlSize) {
      this.element = element;
      this.string = null;
      this.xmlPos = xmlPos;
      this.xmlSize = xmlSize;
    }

    Bit(String string, int xmlPos) {
      this.element = null;
      this.string = string;
      this.xmlPos = xmlPos;
      this.xmlSize = string.length();
    }

    /**
     * @returns the length of the specified bit. 1 for an element, the string
     *          length for a string.
     */
    public int size() {
      if (string != null) {
        return string.length();
      }
      return 1;
    }
  }

  private final Document doc;
  private final List<Bit> bits = Lists.newArrayList();
  private Wavelet wavelet;

  public ApiView(Document doc, Wavelet wavelet) {
    this.doc = doc;
    this.wavelet = wavelet;
    parse(doc);
  }

  private void parse(Document doc) {
    E bodyElement = Blips.getBody(doc);
    if (bodyElement != null) {
      N child = doc.getFirstChild(bodyElement);
      while (child != null) {
        T asText = doc.asText(child);
        int xmlPos = doc.getLocation(child);
        if (asText != null) {
          bits.add(new Bit(doc.getData(asText), xmlPos));
        } else {
          E xmlElement = doc.asElement(child);
          if (xmlElement != null) {
            Element element = ElementSerializer.xmlToApiElement(doc, xmlElement, wavelet);
            // element can be null, but we still want to note that there
            // was something unknown.
            N next = doc.getNextSibling(child);
            int xmlSize;
            if (next != null) {
              xmlSize = doc.getLocation(next) - xmlPos;
            } else {
              // At the end of the document. XmlSize is the rest.
              xmlSize = doc.size() - 1 - xmlPos;
            }
            bits.add(new Bit(element, xmlPos, xmlSize));
          }
        }
        child = doc.getNextSibling(child);
      }
    }
  }

  /**
   * Delete the stuff between start and end not including end.
   */
  public void delete(int start, int end) {
    int len = end - start;
    Pair<Integer, Integer> where = locate(start);
    int index = where.first;
    if (index == bits.size()) {
      // outside
      return;
    }
    int offset = where.second;
    int xmlStart = bits.get(index).xmlPos + offset;
    int xmlEnd = xmlStart;
    while (len > 0) {
      Bit bit = bits.get(index);
      if (bit.string == null) {
        // deleting an element:
        len -= 1;
        shift(index + 1, -bit.xmlSize);
        xmlEnd += bit.xmlSize;
        bits.remove(index);
      } else {
        // deleting a string bit
        int todelete = bit.string.length() - offset;
        if (todelete > len) {
          todelete = len;
        }
        shift(index + 1, -todelete);
        xmlEnd += todelete;
        len -= todelete;
        if (offset > 0) {
          bit.string = bit.string.substring(0, offset) + bit.string.substring(offset + todelete);
          index += 1;
          offset = 0;
        } else {
          if (todelete < bit.string.length()) {
            bit.string = bit.string.substring(todelete);
          } else {
            bits.remove(index);
          }
        }
      }
    }
    doc.deleteRange(xmlStart, xmlEnd);
  }

  public void insert(int pos, Element element) {
    XmlStringBuilder xml = ElementSerializer.apiElementToXml(element);
    int beforeSize = doc.size();
    Pair<Integer, Integer> where = locate(pos);
    int index = where.first;
    if (index == bits.size()) {
      // outside. append.
      Bit last = bits.get(bits.size() - 1);
      Point<Doc.N> point = doc.locate(last.xmlPos + last.xmlSize);
      doc.insertXml(point, xml);
      bits.add(new Bit(element, last.xmlPos + last.xmlSize, doc.size() - beforeSize));
      return;
    }
    int offset = where.second;
    Bit bit = bits.get(index);
    Point<Doc.N> point = doc.locate(bit.xmlPos + offset);
    doc.insertXml(point, xml);
    int xmlSize = doc.size() - beforeSize;
    if (bit.string != null && offset > 0) {
      shift(index + 1, xmlSize);
      String leftOver = bit.string.substring(offset);
      bit.string = bit.string.substring(0, offset);
      bit.xmlSize = offset;
      int nextIndex = bit.xmlPos + bit.xmlSize;
      bits.add(index + 1, new Bit(element, nextIndex, xmlSize));
      nextIndex += xmlSize;
      bits.add(index + 2, new Bit(leftOver, nextIndex));
    } else {
      bits.add(index, new Bit(element, bits.get(index).xmlPos, xmlSize));
      shift(index + 1, xmlSize);
    }
  }

  public void insert(int pos, String content) {
    boolean first = true;
    for (String paragraph : Splitter.on("\n").split(content)) {
      if (first) {
        first = false;
      } else {
        insert(pos, new Line());
        pos++;
      }
      Pair<Integer, Integer> where = locate(pos);
      int index = where.first;
      if (index == bits.size()) {
        // outside. append.
        Bit last = bits.get(bits.size() - 1);
        bits.add(new Bit(paragraph, last.xmlPos + last.xmlSize));
        doc.insertText(last.xmlPos + last.xmlSize, paragraph);
      } else {
        int offset = where.second;
        Bit bit = bits.get(index);
        doc.insertText(bit.xmlPos + offset, paragraph);
        if (bit.string != null) {
          // if it's a string, add to the existing node
          bit.string = bit.string.substring(0, offset) + paragraph + bit.string.substring(offset);
          bit.xmlSize += paragraph.length();
        } else {
          // if it's an element, insert just before
          bits.add(index, new Bit(paragraph, bits.get(index).xmlPos - paragraph.length()));
        }
        shift(index + 1, paragraph.length());
      }
      pos += paragraph.length();
    }
  }

  /**
   * Increment the xmlPos of everything from bitIndex and up by delta
   *
   * @param bitIndex
   * @param delta
   */
  private void shift(int bitIndex, int delta) {
    for (int i = bitIndex; i < bits.size(); i++) {
      bits.get(i).xmlPos += delta;
    }
  }

  /**
   * Find which bit contains offset.
   *
   * @param offset
   * @return the index of the bit plus whatever was left over or null when
   *         offset is outside the document.
   */
  private Pair<Integer, Integer> locate(int offset) {
    int index = 0;
    while (bits.size() > index && bits.get(index).size() <= offset) {
      offset -= bits.get(index).size();
      index++;
    }
    return Pair.of(index, offset);
  }

  /**
   * @returns the api representation of the current contents
   */
  public String apiContents() {
    StringBuilder res = new StringBuilder();
    for (Bit bit : bits) {
      if (bit.string != null) {
        res.append(bit.string);
      } else {
        if (bit.element != null && bit.element.getType().equals(ElementType.LINE)) {
          res.append('\n');
        } else {
          res.append(' ');
        }
      }
    }
    return res.toString();
  }

  /**
   * @returns a list of ElementInfo's describing the elements in view.
   */
  public List<ElementInfo> getElements() {
    List<ElementInfo> res = Lists.newArrayList();
    int index = 0;
    for (Bit bit : bits) {
      if (bit.element != null) {
        res.add(new ElementInfo(bit.element, index, bit.xmlPos));
      }
      index += bit.size();
    }
    return res;
  }

  /**
   * Transforms the given {@code xmlOffset} into the text offset.
   *
   * @param xmlOffset the xml offset to transform.
   * @returns the text offset corresponding to the given xml offset.
   *
   * @throws IllegalArgumentException if the given {@code xmlOffset} is out of
   *         range.
   */
  public int transformToTextOffset(int xmlOffset) {
    // Make sure that the offset is valid.
    Preconditions.checkArgument(xmlOffset >= 0);
    Preconditions.checkArgument(xmlOffset <= doc.size());

    // Find the right bit that contains the xml offset.
    int index = 0;
    int textOffset = 0;
    while (index < bits.size()
        && bits.get(index).xmlPos + bits.get(index).xmlSize - 1 < xmlOffset) {
      Bit bit = bits.get(index++);
      textOffset += bit.string != null ? bit.string.length() : 1;
    }

    // Check if it is beyond the last bit, which is the closing </body> tag. In
    // this case, just return textOffset.
    if (index == bits.size()) {
      return textOffset;
    }

    // Return the offset.
    Bit bit = bits.get(index);
    if (bit.element != null) {
      return textOffset;
    }
    return textOffset + xmlOffset - bit.xmlPos;
  }

  /**
   * @returns the xml index corresponding to the passed apiIndex.
   */
  public int transformToXmlOffset(int apiIndex) {
    Pair<Integer, Integer> where = locate(apiIndex);
    int index = where.first;
    int offset = where.second;
    if (index == bits.size()) {
      // We're beyond the last bit. Return last bit + offset.
      Bit last = bits.get(bits.size() - 1);
      return last.xmlPos + last.xmlSize + offset;
    }
    return bits.get(index).xmlPos + offset;
  }

  /**
   * Legacy support method. Return the index of the element that looks like the
   * one we passed for some value of looks like.
   */
  public int locateElement(Element element) {
    int index = 0;
    for (Bit bit : bits) {
      if (bit.element != null && bit.element.getType().equals(element.getType())) {
        if (element.getType().equals(ElementType.GADGET)) {
          if (propertyMatch(bit.element, element, Gadget.URL)) {
            return index;
          }
        } else if (element.getType().equals(ElementType.LABEL)) {
          if (propertyMatch(bit.element, element, "for")) {
            return index;
          }
        } else if (elementMatch(element, bit.element)) {
          return index;
        }
      }
      index += bit.size();
    }
    return -1;
  }

  private boolean propertyMatch(Element element1, Element element2, String prop) {
    String val1 = element1.getProperty(prop);
    String val2 = element2.getProperty(prop);
    return val1 != null && val1.equals(val2);
  }

  private boolean elementMatch(Element element1, Element element2) {
    // TODO(ljvderijk): Elements should define their own equals method for each
    // different type, improvements to the ElementSerializer can also be made.
    return element1.getProperties().equals(element2.getProperties());
  }

  /**
   * Call reparse when modifications to the underlying documents have been made
   * and the api view needs to be updated.
   *
   * <p>
   * TODO(user): Remove this once everything useful can be done through
   * ApiView.
   */
  public void reparse() {
    bits.clear();
    parse(doc);
  }
}
