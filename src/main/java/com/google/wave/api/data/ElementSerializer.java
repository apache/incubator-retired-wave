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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.wave.api.Attachment;
import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.FormElement;
import com.google.wave.api.Gadget;
import com.google.wave.api.Image;
import com.google.wave.api.Installer;
import com.google.wave.api.Line;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to support serializing Elements from and to XML.
 *
 *
 */
public abstract class ElementSerializer {

  // Two maps to easily look up what to serialize
  private static final Map<ElementType, ElementSerializer> typeToSerializer = Maps.newHashMap();
  private static final Map<String, ElementSerializer> tagToSerializer = Maps.newHashMap();

  private static final String CAPTION_TAG = "caption";
  private static final String CLICK_TAG = "click";
  private static final String ATTACHMENT_STR = "attachment";
  private static final String CAPTION_STR = "caption";

  /** The attachment URL regular expression */
  private static final Pattern ATTACHMENT_URL_PATTERN = Pattern.compile(
      "attachment_url\\\"\\ value\\=\\\"([^\\\"]*)\\\"");

  /** The attachment MIME type regular expression */
  private static final Pattern MIME_TYPE_PATTERN = Pattern.compile(
      "mime_type\\\"\\ value\\=\\\"([^\\\"]*)\\\"");

  /** Attachment Download Host URL */
  private static String attachmentDownloadHostUrl = "";

  public static void setAttachmentDownloadHostUrl(String attachmentDownloadHostUrl){
    ElementSerializer.attachmentDownloadHostUrl = attachmentDownloadHostUrl;
  }

  public static XmlStringBuilder apiElementToXml(Element e) {
    ElementSerializer serializer = typeToSerializer.get(e.getType());
    if (serializer == null) {
      return null;
    }
    return serializer.toXml(e);
  }

  public static Element xmlToApiElement(Document doc, Doc.E element, Wavelet wavelet) {
    if (element == null) {
      return null;
    }
    ElementSerializer serializer = tagToSerializer.get(doc.getTagName(element));
    if (serializer == null) {
      return null;
    }
    return serializer.fromXml(doc, element, wavelet);
  }

  public static String tagNameForElementType(ElementType lookup) {
    ElementSerializer serializer = typeToSerializer.get(lookup);
    if (serializer != null) {
      return serializer.tagName;
    }
    return null;
  }

  public static Map<Integer, Element> serialize(Document doc, Wavelet wavelet) {
    Map<Integer, Element> result = Maps.newHashMap();
    ApiView apiView = new ApiView(doc, wavelet);

    Doc.N node = Blips.getBody(doc);
    if (node != null) {
      // The node is the body; we're after its children
      node = doc.getFirstChild(node);
    }
    while (node != null) {
      E element = doc.asElement(node);
      if (element != null) {
        Element apiElement = xmlToApiElement(doc, element, wavelet);
        if (apiElement != null) {
          result.put(apiView.transformToTextOffset(doc.getLocation(element)), apiElement);
        }
      }
      node = doc.getNextSibling(node);
    }
    return result;
  }

  private static void register(ElementSerializer serializer) {
    typeToSerializer.put(serializer.elementType, serializer);
    tagToSerializer.put(serializer.tagName, serializer);
  }

  static {
    register(new ElementSerializer("label", ElementType.LABEL) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        String value = e.getProperty("value");
        if (value == null) {
          value = e.getProperty("defaultValue");
        }
        return wrapWithContent(value, "for", e.getProperty("name"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        FormElement formElement = createFormElement(doc, element);
        formElement.setName(doc.getAttribute(element, "for"));
        if (doc.getFirstChild(element) != null) {
          formElement.setDefaultValue(doc.getData(doc.asText(doc.getFirstChild(element))));
          formElement.setValue(doc.getData(doc.asText(doc.getFirstChild(element))));
        }
        return formElement;
      }
    });

    register(new ElementSerializer("input", ElementType.INPUT) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        String value = e.getProperty("value");
        if (value == null) {
          value = e.getProperty("defaultValue");
        }
        return wrapWithContent(value, "name", e.getProperty("name"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        FormElement formElement = createFormElement(
            doc, element, doc.getAttribute(element, "submit"));
        // Set the text content.
        if (doc.getFirstChild(element) != null) {
          formElement.setValue(doc.getData(doc.asText(doc.getFirstChild(element))));
        }
        return formElement;
      }
    });

    register(new ElementSerializer("password", ElementType.PASSWORD) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        String value = e.getProperty("value");
        if (value == null) {
          value = e.getProperty("defaultValue");
        }
        return wrap("name", e.getProperty("name"), "value", value);
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        return createFormElement(doc, element, doc.getAttribute(element, "value"));
      }
    });

    register(new ElementSerializer("textarea", ElementType.TEXTAREA) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        XmlStringBuilder res = XmlStringBuilder.createEmpty();
        String value = e.getProperty("value");
        if (isEmptyOrWhitespace(value)) {
          res.append(XmlStringBuilder.createEmpty().wrap(LineContainers.LINE_TAGNAME));
        } else {
          Splitter splitter = Splitter.on("\n");
          for (String paragraph : splitter.split(value)) {
            res.append(XmlStringBuilder.createEmpty().wrap(LineContainers.LINE_TAGNAME));
            res.append(XmlStringBuilder.createText(paragraph));
          }
        }
        return res.wrap("textarea", "name", e.getProperty("name"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        // Set the text content. We're doing a little mini textview here.
        StringBuilder value = new StringBuilder();
        Doc.N node = doc.getFirstChild(element);
        boolean first = true;
        while (node != null) {
          Doc.T text = doc.asText(node);
          if (text != null) {
            value.append(doc.getData(text));
          }
          Doc.E docElement = doc.asElement(node);
          if (docElement != null &&
              doc.getTagName(docElement).equals(LineContainers.LINE_TAGNAME)) {
            if (first) {
              first = false;
            } else {
              value.append('\n');
            }
          }
          node = doc.getNextSibling(node);
        }
        return createFormElement(doc, element, value.toString());
      }
    });

    register(new ElementSerializer("button", ElementType.BUTTON) {
      @Override
      public XmlStringBuilder toXml(Element element) {
        XmlStringBuilder res = XmlStringBuilder.createEmpty();
        res.append(XmlStringBuilder.createText(element.getProperty("value")).wrap(CAPTION_TAG));
        res.append(XmlStringBuilder.createEmpty().wrap("events"));
        return res.wrap("button", "name", element.getProperty("name"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        FormElement formElement = createFormElement(doc, element);

        Doc.N firstChild = doc.getFirstChild(element);
        // Get the default value from the caption.
        if (firstChild != null && doc.getTagName(doc.asElement(firstChild)).equals(CAPTION_TAG) &&
            doc.getFirstChild(doc.asElement(firstChild)) != null) {
          formElement.setDefaultValue(doc.getData(doc.asText(doc.getFirstChild(
              doc.asElement(firstChild)))));
        }

        // Get the value from the last click event.
        if (firstChild != null &&
            doc.getNextSibling(firstChild) != null &&
            doc.asElement(doc.getFirstChild(doc.getNextSibling(firstChild))) != null &&
            doc.getTagName(doc.asElement(doc.getFirstChild(doc.getNextSibling(
                firstChild)))).equals(CLICK_TAG)) {
          formElement.setValue("clicked");
        } else {
          formElement.setValue(formElement.getDefaultValue());
        }
        return formElement;
      }
    });

    register(new ElementSerializer("radiogroup", ElementType.RADIO_BUTTON_GROUP) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        return wrap("name", e.getProperty("name"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        FormElement formElement = createFormElement(
            doc, element, doc.getAttribute(element, "value"));
        return formElement;
      }
    });

    register(new ElementSerializer("radio", ElementType.RADIO_BUTTON) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        return wrap("name", e.getProperty("name"), "group", e.getProperty("value"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        return new FormElement(getElementType(),
            doc.getAttribute(element, "name"),
            doc.getAttribute(element, "group"));
      }
    });

    register(new ElementSerializer("check", ElementType.CHECK) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        return wrap("name", e.getProperty("name"),
            "submit", e.getProperty("defaultValue"),
            "value", e.getProperty("value"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        FormElement formElement = createFormElement(
            doc, element, doc.getAttribute(element, "value"));
        formElement.setDefaultValue(doc.getAttribute(element, "submit"));
        return formElement;
      }
    });

    register(new ElementSerializer("extension_installer", ElementType.INSTALLER) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        return wrap("manifest", e.getProperty("manifest"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        Installer installer = new Installer();
        installer.setManifest(doc.getAttribute(element, "manifest"));

        return installer;
      }
    });

    register(new ElementSerializer("gadget", ElementType.GADGET) {
      @Override
      public XmlStringBuilder toXml(Element element) {
        XmlStringBuilder res = XmlStringBuilder.createEmpty();
        if (element.getProperties().containsKey("category")) {
          res.append(XmlStringBuilder.createEmpty().wrap(
              "category", "name", element.getProperty("category")));
        }
        if (element.getProperties().containsKey("title")) {
          res.append(XmlStringBuilder.createEmpty().wrap(
              "title", "value", element.getProperty("title")));
        }
        if (element.getProperties().containsKey("thumbnail")) {
          res.append(XmlStringBuilder.createEmpty().wrap(
              "thumbnail", "value", element.getProperty("thumbnail")));
        }
        for (Map.Entry<String, String> property : element.getProperties().entrySet()) {
          if (property.getKey().equals("category") || property.getKey().equals("url") ||
              property.getKey().equals("title") || property.getKey().equals("thumbnail") ||
              property.getKey().equals("author")) {
            continue;
          } else if (property.getKey().equals("pref")) {
            res.append(XmlStringBuilder.createEmpty().wrap("pref", "value", property.getValue()));
          } else {
            res.append(XmlStringBuilder.createEmpty().wrap("state",
                "name", property.getKey(),
                "value", property.getValue()));
          }
        }
        List<String> attributes = Lists.newArrayList("url", element.getProperty("url"));
        if (element.getProperties().containsKey("author")) {
          attributes.add("author");
          attributes.add(element.getProperty("author"));
        }
        if (element.getProperties().containsKey("ifr")) {
          attributes.add("ifr");
          attributes.add(element.getProperty("ifr"));
        }
        String[] asArray = new String[attributes.size()];
        attributes.toArray(asArray);
        return res.wrap("gadget", asArray);
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        Gadget gadget = new Gadget();

        gadget.setUrl(doc.getAttribute(element, "url"));
        String author = doc.getAttribute(element, "author");
        if (author != null) {
          gadget.setAuthor(author);
        }
        String ifr = doc.getAttribute(element, "ifr");
        if (ifr != null) {
          gadget.setIframe(ifr);
        }

        // TODO(user): Streamline this. Maybe use SchemaConstraints.java to
        // get a list of child elements or attributes, then automate this.
        E child = doc.asElement(doc.getFirstChild(element));
        while (child != null) {
          if (doc.getTagName(child).equals("name")) {
            gadget.setProperty("name", doc.getAttribute(child, "value"));
          } else if (doc.getTagName(child).equals("title")) {
            gadget.setProperty("title", doc.getAttribute(child, "value"));
          } else if (doc.getTagName(child).equals("thumbnail")) {
            gadget.setProperty("thumbnail", doc.getAttribute(child, "value"));
          } else if (doc.getTagName(child).equals("pref")) {
            gadget.setProperty("pref", doc.getAttribute(child, "value"));
          } else if (doc.getTagName(child).equals("state")) {
            gadget.setProperty(doc.getAttribute(child, "name"), doc.getAttribute(child, "value"));
          } else if (doc.getTagName(child).equals("category")) {
            gadget.setProperty("category", doc.getAttribute(child, "name"));
          }
          child = doc.asElement(doc.getNextSibling(child));
        }

        return gadget;
      }
    });

    register(new ElementSerializer("img", ElementType.IMAGE) {
      @Override
      public XmlStringBuilder toXml(Element element) {
        XmlStringBuilder res = XmlStringBuilder.createEmpty();
        List<String> attributes = Lists.newArrayList("src", element.getProperty("url"));
        if (element.getProperty("width") != null) {
          attributes.add("width");
          attributes.add(element.getProperty("width"));
        }
        if (element.getProperty("height") != null) {
          attributes.add("height");
          attributes.add(element.getProperty("height"));
        }
        if (element.getProperty("caption") != null) {
          attributes.add("alt");
          attributes.add(element.getProperty("caption"));
        }
        String[] asArray = new String[attributes.size()];
        attributes.toArray(asArray);
        return res.wrap("img", asArray);
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        Image image = new Image();

        if (doc.getAttribute(element, "src") != null) {
          image.setUrl(doc.getAttribute(element, "src"));
        }
        if (doc.getAttribute(element, "alt") != null) {
          image.setCaption(doc.getAttribute(element, "alt"));
        }
        if (doc.getAttribute(element, "width") != null) {
          image.setWidth(Integer.parseInt(doc.getAttribute(element, "width")));
        }
        if (doc.getAttribute(element, "height") != null) {
          image.setHeight(Integer.parseInt(doc.getAttribute(element, "height")));
        }

        return image;
      }
    });

    register(new ElementSerializer("image", ElementType.ATTACHMENT) {
      @Override
      public XmlStringBuilder toXml(Element element) {
        XmlStringBuilder res = XmlStringBuilder.createEmpty();
        if (element.getProperties().containsKey("attachmentId")) {
          if (element.getProperty(CAPTION_STR) != null) {
            res.append(XmlStringBuilder.createText(element.getProperty(CAPTION_STR))
                .wrap("caption"));
          }
          return res.wrap("image", ATTACHMENT_STR, element.getProperty("attachmentId"));
        }
        return res;
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        Map<String, String> properties = Maps.newHashMap();

        String attachmentId = doc.getAttribute(element, ATTACHMENT_STR);
        if (attachmentId != null) {
          properties.put(Attachment.ATTACHMENT_ID, attachmentId);
        }
        String caption = getCaption(doc, element);
        if (caption != null) {
          properties.put(Attachment.CAPTION, caption);
        }
        if (wavelet != null && attachmentId != null) {
          Document attachmentDataDoc =
            wavelet.getDocument(IdConstants.ATTACHMENT_METADATA_PREFIX + "+" + attachmentId);
          if (attachmentDataDoc != null) {
            String dataDocument = attachmentDataDoc.toXmlString();
            if (dataDocument != null) {
              properties.put(Attachment.MIME_TYPE, extractValue(dataDocument, MIME_TYPE_PATTERN));
              properties.put(Attachment.ATTACHMENT_URL,
                  ElementSerializer.attachmentDownloadHostUrl
                  + getAttachmentUrl(dataDocument));
            }
          }
        }
        return new Attachment(properties, null);
      }

      private String getCaption(Document doc, E element) {
        N node = doc.getFirstChild(element);

        while (node != null) {
          E cElement = doc.asElement(node);
          if (cElement != null && doc.getTagName(cElement).equals(CAPTION_TAG) &&
              doc.getFirstChild(cElement) != null) {
            return doc.getData(doc.asText(doc.getFirstChild(cElement)));
          }
          node = doc.getNextSibling(node);
        }
        return null;
      }
    });

    register(new ElementSerializer(LineContainers.LINE_TAGNAME, ElementType.LINE) {

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        Line paragraph = new Line();
        if (doc.getAttribute(element, "t") != null) {
          paragraph.setLineType(doc.getAttribute(element, "t"));
        }
        if (doc.getAttribute(element, "i") != null) {
          paragraph.setIndent(doc.getAttribute(element, "i"));
        }
        if (doc.getAttribute(element, "a") != null) {
          paragraph.setAlignment(doc.getAttribute(element, "a"));
        }
        if (doc.getAttribute(element, "d") != null) {
          paragraph.setDirection(doc.getAttribute(element, "d"));
        }
        return paragraph;
      }

      @Override
      public XmlStringBuilder toXml(Element element) {
        XmlStringBuilder res = XmlStringBuilder.createEmpty();
        // A cast would be nice here, but unfortunately the element
        // gets deserialized as an actual Element
        Line line = new Line(element.getProperties());

        List<String> attributes = Lists.newArrayList();
        if (!isEmptyOrWhitespace(line.getLineType())) {
          attributes.add("t");
          attributes.add(line.getLineType());
        }
        if (!isEmptyOrWhitespace(line.getIndent())) {
          attributes.add("i");
          attributes.add(line.getIndent());
        }
        if (!isEmptyOrWhitespace(line.getAlignment())) {
          attributes.add("a");
          attributes.add(line.getAlignment());
        }
        if (!isEmptyOrWhitespace(line.getDirection())) {
          attributes.add("d");
          attributes.add(line.getDirection());
        }
        String[] asArray = new String[attributes.size()];
        attributes.toArray(asArray);
        return res.wrap(LineContainers.LINE_TAGNAME, asArray);
      }

    });

    register(new ElementSerializer(Blips.THREAD_INLINE_ANCHOR_TAGNAME, ElementType.INLINE_BLIP) {
      @Override
      public XmlStringBuilder toXml(Element e) {
        return XmlStringBuilder.createEmpty().wrap(
            Blips.THREAD_INLINE_ANCHOR_TAGNAME,
            Blips.THREAD_INLINE_ANCHOR_ID_ATTR, e.getProperty("id"));
      }

      @Override
      public Element fromXml(Document doc, E element, Wavelet wavelet) {
        return new Element(ElementType.INLINE_BLIP,
            ImmutableMap.of("id", doc.getAttribute(element, Blips.THREAD_INLINE_ANCHOR_ID_ATTR)));
      }
    });
  }

  private final String tagName;
  private final ElementType elementType;
  protected abstract XmlStringBuilder toXml(Element e);
  protected abstract Element fromXml(Document doc, E element, Wavelet wavelet);

  public String getTagName() {
    return tagName;
  }

  public ElementType getElementType() {
    return elementType;
  }

  protected XmlStringBuilder wrap(String... attributes) {
    return XmlStringBuilder.createEmpty().wrap(tagName, attributes);
  }

  protected XmlStringBuilder wrapWithContent(String content, String... attributes) {
    if (Strings.isNullOrEmpty(content)) {
      return wrap(attributes);
    }
    return XmlStringBuilder.createText(content).wrap(tagName, attributes);
  }

  /**
   * Helper method to create a form element
   * @return a form element of the right type and with the right name and
   *         optionally an initial value.
   */

  protected FormElement createFormElement(Document doc, E element, String initialValue) {
    FormElement formElement = new FormElement(elementType, doc.getAttribute(element, "name"));
    if (initialValue != null) {
      formElement.setValue(initialValue);
      formElement.setDefaultValue(initialValue);
    }
    return formElement;
  }

  protected FormElement createFormElement(Document doc, E element) {
    return createFormElement(doc, element, null);
  }


  public ElementSerializer(String tagName, ElementType elementType) {
    this.tagName = tagName;
    this.elementType = elementType;
  }

  private static boolean isEmptyOrWhitespace(String value) {
    return value == null || CharMatcher.WHITESPACE.matchesAllOf(value);
  }

  private static String getAttachmentUrl(String dataDocument) {
    String rawURL = extractValue(dataDocument, ATTACHMENT_URL_PATTERN);
    return rawURL == null ? "" : rawURL.replace("&amp;", "&");
  }

  // TODO(user): move away from REGEX
  private static String extractValue(String dataDocument, Pattern pattern) {
    Matcher matcher = pattern.matcher(dataDocument);
    return matcher.find() ? matcher.group(1) : null;
  }
}
