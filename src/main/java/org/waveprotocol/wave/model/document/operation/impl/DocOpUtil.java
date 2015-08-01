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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMapBuilder;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationComponentType;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpComponentType;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.algorithm.AnnotationsNormalizer;
import org.waveprotocol.wave.model.document.operation.algorithm.RangeNormalizer;
import org.waveprotocol.wave.model.document.operation.util.ExplodedDocOp;
import org.waveprotocol.wave.model.document.parser.AnnotationParser;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.document.parser.XmlParserFactory;
import org.waveprotocol.wave.model.document.parser.XmlPullParser;
import org.waveprotocol.wave.model.operation.OpCursorException;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DocOpUtil {

  private DocOpUtil() { /* utility class, not to be instantiated */ }

  /**
   * Name of the XML Processing Instruction used for annotations.
   *
   * Refer to this variable only. Do not use a string literal.
   */
  public static final String PI_TARGET = "a";

  public static DocInitialization asInitialization(final DocOp op) {
    if (op instanceof DocInitialization) {
      return (DocInitialization) op;
    } else {
      return new AbstractBufferedDocInitialization() {
        @Override
        public void apply(DocInitializationCursor c) {
          op.apply(InitializationCursorAdapter.adapt(c));
        }

        @Override
        public void applyComponent(int i, DocInitializationCursor c) {
          op.applyComponent(i, InitializationCursorAdapter.adapt(c));
        }

        @Override
        public AnnotationBoundaryMap getAnnotationBoundary(int i) {
          return op.getAnnotationBoundary(i);
        }

        @Override
        public String getCharactersString(int i) {
          return op.getCharactersString(i);
        }

        @Override
        public Attributes getElementStartAttributes(int i) {
          return op.getElementStartAttributes(i);
        }

        @Override
        public String getElementStartTag(int i) {
          return op.getElementStartTag(i);
        }

        @Override
        public DocInitializationComponentType getType(int i) {
          DocOpComponentType t = op.getType(i);
          if (t instanceof DocInitializationComponentType) {
            return (DocInitializationComponentType) t;
          } else {
            throw new UnsupportedOperationException(
                "Initialization with unexpected component " + t + ": " + op);
          }
        }

        @Override
        public int size() {
          return op.size();
        }
      };
    }
  }

  public static String toConciseString(DocOp op) {
    final StringBuilder b = new StringBuilder();
    op.apply(createConciseStringBuilder(op, b));
    return b.toString();
  }

  public static DocOpCursor createConciseStringBuilder(DocOp op, final StringBuilder b) {
    return new DocOpCursor() {
      @Override
      public void deleteCharacters(String chars) {
        b.append("--" + literalString(chars) + "; ");
      }

      @Override
      public void deleteElementEnd() {
        b.append("x>; ");
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
        b.append("x< " + type + " " + toConciseString(attrs) + "; ");
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        b.append("r@ " + toConciseString(oldAttrs) + " " + toConciseString(newAttrs) + "; ");
      }

      @Override
      public void retain(int distance) {
        b.append("__" + distance + "; ");
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        b.append("u@ " + toConciseString(attrUpdate) + "; ");
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
        b.append("|| " + toConciseString(map) + "; ");
      }

      @Override
      public void characters(String chars) {
        b.append("++" + literalString(chars) + "; ");
      }

      @Override
      public void elementEnd() {
        b.append(">>; ");
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
        b.append("<< " + type + " " + toConciseString(attrs) + "; ");
      }
    };
  }

  public static String toConciseString(Attributes attributes) {
    if (attributes.isEmpty()) {
      return "{}";
    }
    StringBuilder b = new StringBuilder();
    b.append("{ ");
    boolean first = true;
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      if (first) {
        first = false;
      } else {
        b.append(", ");
      }
      b.append(entry.getKey());
      b.append("=");
      b.append(literalString(entry.getValue()));
    }
    b.append(" }");
    return b.toString();
  }

  public static String toConciseString(AttributesUpdate update) {
    if (update.changeSize() == 0) {
      return "{}";
    }
    StringBuilder b = new StringBuilder();
    b.append("{ ");
    for (int i = 0; i < update.changeSize(); ++i) {
      if (i > 0) {
        b.append(", ");
      }
      b.append(update.getChangeKey(i));
      b.append(": ");
      b.append(literalString(update.getOldValue(i)));
      b.append(" -> ");
      b.append(literalString(update.getNewValue(i)));
    }
    b.append(" }");
    return b.toString();
  }

  public static String toConciseString(AnnotationBoundaryMap map) {
    StringBuilder b = new StringBuilder();
    b.append("{ ");
    boolean notEmpty = false;
    for (int i = 0; i < map.endSize(); ++i) {
      if (notEmpty) {
        b.append(", ");
      } else {
        notEmpty = true;
      }
      b.append(literalString(map.getEndKey(i)));
    }
    for (int i = 0; i < map.changeSize(); ++i) {
      if (notEmpty) {
        b.append(", ");
      } else {
        notEmpty = true;
      }
      b.append(literalString(map.getChangeKey(i)));
      b.append(": ");
      b.append(literalString(map.getOldValue(i)));
      b.append(" -> ");
      b.append(literalString(map.getNewValue(i)));
    }
    b.append(" }");
    return notEmpty ? b.toString() : "{}";
  }

  private static String escapeLiteral(String string) {
    return string.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String literalString(String string) {
    return (string == null) ? "null" : "\"" + escapeLiteral(string) + "\"";
  }

  /**
   * Generates the minimal, normalised XML representation of a document
   * initialisation.
   *
   * It is safe to use the return value of this method to compare the equality
   * of two documents.
   *
   * @param op must be well-formed
   * @return XML String representation, with annotations represented by a
   *         nonstandard processing instruction notation.
   */
  public static String toXmlString(DocInitialization op) {
    return toPrettyXmlString(op, -1);
  }

  /**
   * Same as {@link #toXmlString(DocInitialization)}, but pretty-prints
   * @param indent indent level
   */
  public static String toPrettyXmlString(DocInitialization op, final int indent) {

    //TODO(danilatos): Actually implement indent.

    final StringBuilder b = new StringBuilder();
    buildXmlString(op, indent, b);
    return b.toString();
  }

  /**
   * Variant of {@link #toPrettyXmlString(DocInitialization, int)} that accepts a
   * StringBuilder instead
   */
  public static void buildXmlString(DocInitialization op, final int indent,
      final StringBuilder b) {

    try {
      op.apply(new DocInitializationCursor() {
        Map<String, String> currentAnnotations = new HashMap<String, String>();
        TreeMap<String, String> changes = new TreeMap<String, String>();
        // Deque not supported by GWT :(
        ArrayList<String> tags = new ArrayList<String>();

        String elementPart;

        @Override
        public void annotationBoundary(AnnotationBoundaryMap map) {
          changes.clear();
          for (int i = 0; i < map.changeSize(); i++) {
            String key = map.getChangeKey(i);
            String value = map.getNewValue(i);
            if (!equal(currentAnnotations.get(key), value)) {
              // removal not necessary if null, get will return the same in either case.
              currentAnnotations.put(key, value);
              changes.put(key, value);
            }
          }
          for (int i = 0; i < map.endSize(); i++) {
            String key = map.getEndKey(i);
            if (currentAnnotations.get(key) != null) {
              currentAnnotations.remove(key);
              changes.put(key, null);
            }
          }

          if (changes.isEmpty()) {
            return;
          }

          if (elementPart != null) {
            b.append(elementPart + ">");
            elementPart = null;
          }
          b.append("<?" + PI_TARGET);
          for (Map.Entry<String, String> entry : changes.entrySet()) {
            if (entry.getValue() != null) {
              b.append(" \"" + xmlTextEscape(annotationEscape(entry.getKey())) + "\"");
              b.append("=");
              b.append("\"" + xmlTextEscape(annotationEscape(entry.getValue())) + "\"");
            } else {
              // This code renders ending annotations and annotations that are
              // changed to null the same way, which is OK since we are
              // only concerned with DocIntializations.  (It's, in fact, the
              // only correct solution since our test cases use this code for
              // equality comparison of documents.)
              b.append(" \"" + xmlTextEscape(annotationEscape(entry.getKey())) + "\"");
            }
          }
          b.append("?>");
        }

        @Override
        public void characters(String chars) {
          if (elementPart != null) {
            b.append(elementPart + ">");
            elementPart = null;
          }
          b.append(xmlTextEscape(chars));
        }

        @Override
        public void elementStart(String type, Attributes attrs) {
          if (elementPart != null) {
            b.append(elementPart + ">");
            elementPart = null;
          }
          elementPart = "<" + type + (attrs.isEmpty() ? "" : " " + attributeString(attrs));
          tags.add(type);
        }

        @Override
        public void elementEnd() {
          if (elementPart != null) {
            b.append(elementPart + "/>");
            elementPart = null;
            assert tags.size() > 0;
            tags.remove(tags.size() - 1);
          } else {
            String tag;
            tag = tags.remove(tags.size() - 1);
            b.append("</" + tag + ">");
          }
        }

        private boolean equal(String a, String b) {
          return a == null ? b == null : a.equals(b);
        }

      });
    } catch (RuntimeException e) {
      throw new RuntimeException("toXmlString: DocInitialization was probably ill-formed", e);
    }
  }

  public static String debugToXmlString(DocInitialization op) {
    try {
      return toXmlString(op);
    } catch (OpCursorException e) {
      // This exception is probably due to some internal validity problem with the operation,
      // e.g. a lazily evaluated compose implementation.
      // Because this is similar to an OperationException, we should catch it and return
      // something for debug purposes, rather than have the method simply crash.

      // Append the identity hashCode to decrease the probability of two error return values
      // being equal, in case they're being used for equality comparisons.
      // They shouldn't, toXmlString() is better for this.
      return "toXmlString: DocInitialization was internally broken. " +
          "(" + Integer.toHexString(System.identityHashCode(op)) + ")";
    }
  }

  public static String attributeString(Attributes attributes) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> e : attributes.entrySet()) {
      if (first) {
        first = false;
      } else {
        b.append(" ");
      }
      // We're just writing null with no quotes if the value is null.
      // This is acceptable since it only occurs in updateAttributes,
      // which is a processing instruction, so we define the format of it.
      //
      // TODO: We should escape ' and " and < and & etc. in the value.
      b.append(e.getKey() + "=" + (e.getValue() == null ? "null"
          : "\"" + xmlAttrEscape(e.getValue()) + "\""));
    }
    return b.toString();
  }

  /**
   * Warning: escapes only the double quotation marks! (is that officially
   * enough, if it is to be surrounded by double quotation marks?)
   *
   * @param attrValue
   */
  public static String xmlAttrEscape(String attrValue) {
    return attrValue
        .replaceAll("\"", "&quot;");
  }

  public static String xmlTextEscape(String text) {
    return text
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
  }

  public static String annotationEscape(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("?", "\\q");
  }

  public static String xmlTextUnEscape(String escaped) {
    return escaped
        .replaceAll("&gt;", ">")
        .replaceAll("&lt;", "<")
        .replaceAll("&amp;", "&");
  }

  public static String annotationUnEscape(String escaped) {
    return escaped
        .replace("\\q", "?")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\");
  }

  public static DocOp normalize(DocOp in) {
    EvaluatingDocOpCursor<DocOp> n = new AnnotationsNormalizer<DocOp>(
        new RangeNormalizer<DocOp>(new DocOpBuffer()));
    in.apply(n);
    return n.finish();
  }

  public static DocInitialization normalize(DocInitialization in) {
    EvaluatingDocOpCursor<DocOp> n = new AnnotationsNormalizer<DocOp>(
        new RangeNormalizer<DocOp>(new DocOpBuffer()));
    in.apply(n);
    return asInitialization(n.finish());
  }

  /**
   * Computes the number of items of the document that an op applies to, prior
   * to its application.
   */
  public static int initialDocumentLength(DocOp op) {
    final int[] size = { 0 };
    op.apply(new DocOpCursor() {
      @Override
      public void deleteCharacters(String chars) {
        size[0] += chars.length();
      }

      @Override
      public void deleteElementEnd() {
        size[0]++;
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
        size[0]++;
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        size[0]++;
      }

      @Override
      public void retain(int itemCount) {
        size[0] += itemCount;
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        size[0]++;
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
      }

      @Override
      public void characters(String chars) {
      }

      @Override
      public void elementEnd() {
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
      }
    });
    return size[0];
  }

  /**
   * Computes the number of items of the document that an op produces when
   * applied.
   */
  public static int resultingDocumentLength(DocOp op) {
    final int[] size = { 0 };
    op.apply(new DocOpCursor() {
      @Override
      public void deleteCharacters(String chars) {
      }

      @Override
      public void deleteElementEnd() {
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        size[0]++;
      }

      @Override
      public void retain(int itemCount) {
        size[0] += itemCount;
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        size[0]++;
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
      }

      @Override
      public void characters(String chars) {
        size[0] += chars.length();
      }

      @Override
      public void elementEnd() {
        size[0]++;
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
        size[0]++;
      }
    });
    return size[0];
  }

  public static String[] visualiseOpWithDocument(final DocInitialization doc, final DocOp op) {
    final StringBuilder docB = new StringBuilder();
    final StringBuilder opB = new StringBuilder();
    final StringBuilder indicesB = new StringBuilder();

    final StringBuilder[] builders = { docB, opB, indicesB };

    final DocInitialization exploded = ExplodedDocOp.explode(doc);

    final int numDocComponents = exploded.size();

    final DocOpCursor opStringifier = createConciseStringBuilder(op, opB);

    final DocInitializationBuffer target = new DocInitializationBuffer();
    new Runnable() {
      int index = 0;
      int docItem = 0;

      private void runTarget(int itemCount) {
        indicesB.append(docItem);
        docItem += itemCount;
        while (index < numDocComponents && itemCount > 0) {
          exploded.applyComponent(index, target);
          if (exploded.getType(index) != DocOpComponentType.ANNOTATION_BOUNDARY) {
            itemCount--;
          }
          index++;
        }
      }

      private void matchUp() {
        int max = 0;
        for (StringBuilder b : builders) {
          max = Math.max(max, b.length());
        }
        for (StringBuilder b : builders) {
          while (b.length() < max) {
            b.append(' ');
          }
        }
      }

      @Override
      public void run() {
        op.apply(new DocOpCursor() {

          @Override
          public void deleteCharacters(String chars) {
            opStringifier.deleteCharacters(chars);
            runTarget(chars.length());
            matchUp();
          }

          @Override
          public void deleteElementEnd() {
            opStringifier.deleteElementEnd();
            runTarget(1);
            matchUp();
          }

          @Override
          public void deleteElementStart(String type, Attributes attrs) {
            opStringifier.deleteElementStart(type, attrs);
            runTarget(1);
            matchUp();
          }

          @Override
          public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
            opStringifier.replaceAttributes(oldAttrs, newAttrs);
            runTarget(1);
            matchUp();
          }

          @Override
          public void retain(int itemCount) {
            opStringifier.retain(itemCount);
            runTarget(itemCount);
            matchUp();
          }

          @Override
          public void updateAttributes(AttributesUpdate attrUpdate) {
            opStringifier.updateAttributes(attrUpdate);
            runTarget(1);
            matchUp();
          }

          @Override
          public void annotationBoundary(AnnotationBoundaryMap map) {
            opStringifier.annotationBoundary(map);
            matchUp();
          }

          @Override
          public void characters(String chars) {
            opStringifier.characters(chars);
            matchUp();
          }

          @Override
          public void elementEnd() {
            opStringifier.elementEnd();
            matchUp();
          }

          @Override
          public void elementStart(String type, Attributes attrs) {
            opStringifier.elementStart(type, attrs);
          }

        });
        runTarget(1);
      }
    }.run();
    buildXmlString(target.finish(), -1, docB);

    return new String[] { docB.toString(), opB.toString(), indicesB.toString() };
  }


  /**
   * The inverse of toXmlString. Takes an XML representation of a document
   * initialization and returns a DocInitialization.
   *
   * @param text
   * @return a DocInitialization
   * @throws XmlParseException
   */
  public static DocInitialization docInitializationFromXml(String text) throws XmlParseException {
    XmlPullParser p = XmlParserFactory.unbuffered(text);
    DocOpBuilder builder = new DocOpBuilder();
    while (p.hasNext()) {
      switch (p.next()) {
        case START_ELEMENT:
          builder.elementStart(p.getTagName(), AttributesImpl.fromStringMap(p.getAttributes()));
          continue;
        case END_ELEMENT:
          builder.elementEnd();
          continue;
        case TEXT:
          builder.characters(p.getText());
          continue;
        case PROCESSING_INSTRUCTION:
          String name = p.getProcessingInstructionName();
          AnnotationBoundaryMapBuilder anBuilder = new AnnotationBoundaryMapBuilder();
          if (PI_TARGET.equals(name)) {
            List<Pair<String, String>> parseAnnotations =
                AnnotationParser.parseAnnotations(p.getProcessingInstructionValue());
            for (Pair<String, String> ann : parseAnnotations) {
              final String key = ann.first;
              final String oldValue = null;
              final String newValue = ann.second;
              if (newValue == null) {
                anBuilder.end(key);
              } else {
                anBuilder.change(key, oldValue, newValue);
              }
            }
            builder.annotationBoundary(anBuilder.build());
          }
          continue;
      }
    }

    DocOp op = builder.build();
    return DocOpUtil.asInitialization(op);
  }
}
