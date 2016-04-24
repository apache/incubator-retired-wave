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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.wave.api.Element;
import com.google.wave.api.Gadget;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.Range;
import com.google.wave.api.data.ApiView;
import com.google.wave.api.data.DocumentHitIterator;
import com.google.wave.api.data.ElementSerializer;
import com.google.wave.api.impl.DocumentModifyAction;
import com.google.wave.api.impl.DocumentModifyAction.BundledAnnotation;
import com.google.wave.api.impl.DocumentModifyAction.ModifyHow;
import com.google.wave.api.impl.DocumentModifyQuery;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.gadget.GadgetXmlUtil;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Map;

/**
 * Implements the "document.modify" operations.
 *
 * <p>
 * The documentModify operation has three bits: the where: where is the
 * modification applied. a range, index, element or annotation can be specified.
 * Any of these is the converted to a range. If nothing appropriate is
 * specified, the entire document is taken. the how: do we insert (before),
 * append or replace? the what: what is inserted/replaced.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DocumentModifyService implements OperationService {

  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    String blipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);

    DocumentModifyAction modifyAction =
        OperationUtil.getRequiredParameter(operation, ParamsProperty.MODIFY_ACTION);

    OpBasedWavelet wavelet = context.openWavelet(operation, participant);
    ObservableConversation conversation =
        context.openConversation(operation, participant).getRoot();
    Document doc = context.getBlip(conversation, blipId).getContent();

    ApiView view = new ApiView(doc, wavelet);
    DocumentHitIterator hitIterator = getDocumentHitIterator(operation, view);

    switch (modifyAction.getModifyHow()) {
      case ANNOTATE:
        annotate(operation, doc, view, hitIterator, modifyAction);
        break;
      case CLEAR_ANNOTATION:
        clearAnnotation(operation, doc, view, hitIterator, modifyAction);
        break;
      case DELETE:
        delete(operation, view, hitIterator);
        break;
      case INSERT:
        insert(operation, doc, view, hitIterator, modifyAction);
        break;
      case INSERT_AFTER:
        insertAfter(operation, doc, view, hitIterator, modifyAction);
        break;
      case REPLACE:
        replace(operation, doc, view, hitIterator, modifyAction);
        break;
      case UPDATE_ELEMENT:
        updateElement(operation, doc, view, hitIterator, modifyAction);
        break;
      default:
        throw new UnsupportedOperationException(
            "Unsupported ModifyHow " + modifyAction.getModifyHow());
    }
  }

  /**
   * Returns the {@link DocumentHitIterator} for the area that needs to be
   * modified as specified in the robot operation.
   *
   * @param operation the operation that specifies where the modifications need
   *        to be applied.
   * @param view the {@link ApiView} of the document that needs to be modified.
   * @throws InvalidRequestException if more than one "where" parameter is
   *         specified.
   */
  private DocumentHitIterator getDocumentHitIterator(OperationRequest operation, ApiView view)
      throws InvalidRequestException {
    Range range = OperationUtil.getOptionalParameter(operation, ParamsProperty.RANGE);
    Integer index = OperationUtil.getOptionalParameter(operation, ParamsProperty.INDEX);
    DocumentModifyQuery query =
        OperationUtil.getOptionalParameter(operation, ParamsProperty.MODIFY_QUERY);

    DocumentHitIterator hitIterator;
    if (range != null) {
      if (index != null || query != null) {
        throw new InvalidRequestException(
            "At most one parameter out of RANGE, INDEX and MODIFY_QUERY must be specified",
            operation);
      }
      // Use the specified range
      hitIterator = new DocumentHitIterator.Singleshot(range);
    } else if (index != null) {
      if (query != null) { // range is null.
        throw new InvalidRequestException(
            "At most one parameter out of RANGE, INDEX and MODIFY_QUERY must be specified",
            operation);
      }
      // Use exactly the location of the index
      hitIterator = new DocumentHitIterator.Singleshot(new Range(index, index + 1));
    } else if (query != null) { // range and index are both null.
      // Use the query
      hitIterator = new DocumentHitIterator.ElementMatcher(
          view, query.getElementMatch(), query.getRestrictions(), query.getMaxRes());
    } else {
      // Take entire document since nothing appropriate was specified
      hitIterator = new DocumentHitIterator.Singleshot(new Range(0, view.apiContents().length()));
    }
    return hitIterator;
  }

  /**
   * Annotates the given ranges of the document as indicated by the
   * {@link DocumentModifyAction}.
   *
   * @param operation the operation to execute.
   * @param doc the document to annotate.
   * @param view the view of the document.
   * @param hitIterator iterates over the ranges to annotate, specified in
   *        {@link ApiView} offset.
   * @param modifyAction the {@link DocumentModifyAction} specifying what the
   *        annotation is.
   * @throws InvalidRequestException if the annotation could not be set.
   */
  private void annotate(OperationRequest operation, Document doc, ApiView view,
      DocumentHitIterator hitIterator, DocumentModifyAction modifyAction)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        modifyAction.getModifyHow() == ModifyHow.ANNOTATE, "This method only supports ANNOTATE");

    String annotationKey = modifyAction.getAnnotationKey();

    int valueIndex = 0;
    Range range = hitIterator.next();
    while (range != null) {
      int start = view.transformToXmlOffset(range.getStart());
      int end = view.transformToXmlOffset(range.getEnd());
      setDocumentAnnotation(
          operation, doc, start, end, annotationKey, modifyAction.getValue(valueIndex));

      valueIndex++;
      range = hitIterator.next();
    }
  }

  /**
   * Clears the annotation for the given ranges of the document as indicated by
   * the {@link DocumentModifyAction}.
   *
   * @param operation the operation to execute.
   * @param doc the document to annotate.
   * @param view the view of the document.
   * @param hitIterator iterates over the ranges to remove the annotation from,
   *        specified in {@link ApiView} offset.
   * @param modifyAction the {@link DocumentModifyAction} specifying what the
   *        key of the annotation is annotation is.
   * @throws InvalidRequestException if the annotation could not be set.
   */
  private void clearAnnotation(OperationRequest operation, Document doc, ApiView view,
      DocumentHitIterator hitIterator, DocumentModifyAction modifyAction)
      throws InvalidRequestException {
    Preconditions.checkArgument(modifyAction.getModifyHow() == ModifyHow.CLEAR_ANNOTATION,
        "This method only supports CLEAR_ANNOTATION");

    String annotationKey = modifyAction.getAnnotationKey();

    Range range = hitIterator.next();
    while (range != null) {
      int start = view.transformToXmlOffset(range.getStart());
      int end = view.transformToXmlOffset(range.getEnd());
      setDocumentAnnotation(operation, doc, start, end, annotationKey, null);

      range = hitIterator.next();
    }
  }

  /**
   * Sets the annotation for a document.
   *
   * @param operation the operation requesting the annotation to be set.
   * @param doc the document to change the annotation in.
   * @param start where the annotation should start.
   * @param end where the annotation should end.
   * @param key the key of the annotation.
   * @param value the value of the annotation.
   * @throws InvalidRequestException if the annotation could not be set.
   */
  private void setDocumentAnnotation(
      OperationRequest operation, Document doc, int start, int end, String key, String value)
      throws InvalidRequestException {
    try {
      doc.setAnnotation(start, end, key, value);
    } catch (IndexOutOfBoundsException e) {
      throw new InvalidRequestException(
          "Can't set annotation for out of bounds indices " + e.getMessage(), operation, e);
    }
  }

  /**
   * Deletes ranges of elements from a document as specified by the iterator.
   *
   * @param operation the operation to execute.
   * @param view the view of the document.
   * @param hitIterator iterates over the ranges of elements to delete.
   * @throws InvalidRequestException if the specified range was invalid.
   */
  private void delete(OperationRequest operation, ApiView view, DocumentHitIterator hitIterator)
      throws InvalidRequestException {
    Range range = hitIterator.next();
    while (range != null) {
      int start = range.getStart();
      int end = range.getEnd();

      if (start == 0) {
        // Can't delete the first new line.
        start = 1;
      }

      if (start >= end) {
        throw new InvalidRequestException(
            "Invalid range specified, " + start + ">=" + end, operation);
      }

      // Delete using the view.
      view.delete(start, end);
      // Shift the iterator to match the updated document.
      hitIterator.shift(start, end - start);

      range = hitIterator.next();
    }
  }

  /**
   * Inserts elements at the position specified by the hitIterator.
   *
   * @param operation the operation that wants to insert elements.
   * @param doc the document to insert elements in.
   * @param view the {@link ApiView} of that document.
   * @param hitIterator the iterator over the places where to insert.
   * @param modifyAction the action that specifies what to insert.
   * @throws InvalidRequestException if something goes wrong.
   */
  private void insert(OperationRequest operation, Document doc, ApiView view,
      DocumentHitIterator hitIterator, DocumentModifyAction modifyAction)
      throws InvalidRequestException {
    int valueIndex = 0;
    Range range = hitIterator.next();
    while (range != null) {
      int insertAt = range.getStart();

      int inserted = insertInto(operation, doc, view, insertAt, modifyAction, valueIndex);
      hitIterator.shift(insertAt, inserted);

      valueIndex++;
      range = hitIterator.next();
    }
  }

  /**
   * Inserts elements after the position specified by the hitIterator.
   *
   * @param operation the operation that wants to insert elements.
   * @param doc the document to insert elements in.
   * @param view the {@link ApiView} of that document.
   * @param hitIterator the iterator over the places where to insert.
   * @param modifyAction the action that specifies what to insert.
   * @throws InvalidRequestException if something goes wrong.
   */
  private void insertAfter(OperationRequest operation, Document doc, ApiView view,
      DocumentHitIterator hitIterator, DocumentModifyAction modifyAction)
      throws InvalidRequestException {
    int valueIndex = 0;
    Range range = hitIterator.next();
    while (range != null) {
      int insertAt = range.getEnd();

      int inserted = insertInto(operation, doc, view, insertAt, modifyAction, valueIndex);
      hitIterator.shift(insertAt, inserted);

      valueIndex++;
      range = hitIterator.next();
    }
  }

  /**
   * Replaces elements at the position specified by the hitIterator with the
   * elements specified in the {@link DocumentModifyAction}.
   *
   * @param operation the operation that wants to replace elements.
   * @param doc the document to replace elements in.
   * @param view the {@link ApiView} of that document.
   * @param hitIterator the iterator over the places where to replace elements.
   * @param modifyAction the action that specifies what to replace the elements
   *        with.
   * @throws InvalidRequestException if something goes wrong.
   */
  private void replace(OperationRequest operation, Document doc, ApiView view,
      DocumentHitIterator hitIterator, DocumentModifyAction modifyAction)
      throws InvalidRequestException {
    int valueIndex = 0;
    Range range = hitIterator.next();
    while (range != null) {
      int replaceAt = range.getStart();

      int numInserted = insertInto(operation, doc, view, replaceAt, modifyAction, valueIndex);

      // Remove the text after what was inserted (so it looks like it has been
      // replaced).
      view.delete(replaceAt + numInserted, range.getEnd() + numInserted);

      // Shift the iterator from the start of the replacement with the amount of
      // characters that have been added.
      int numRemoved = Math.min(0, range.getStart() - range.getEnd());
      hitIterator.shift(replaceAt, numInserted + numRemoved);

      valueIndex++;
      range = hitIterator.next();
    }
  }

  /**
   * Inserts elements into the document at a specified location.
   *
   * @param operation the operation that wants to insert elements.
   * @param doc the document to insert elements in.
   * @param view the {@link ApiView} of that document.
   * @param insertAt the {@link ApiView} value of where to insert elements.
   * @param modifyAction the action that specifies what to insert.
   * @param valueIndex the index to use for
   *        {@link DocumentModifyAction#getValue(int)}, to find out what to
   *        insert.
   * @throws InvalidRequestException if something goes wrong.
   */
  private int insertInto(OperationRequest operation, Document doc, ApiView view, int insertAt,
      DocumentModifyAction modifyAction, int valueIndex) throws InvalidRequestException {

    if (modifyAction.hasTextAt(valueIndex)) {
      String toInsert = modifyAction.getValue(valueIndex);

      if (insertAt == 0) {
        // Make sure that we have a newline as first character.
        if (!toInsert.isEmpty() && toInsert.charAt(0) != '\n') {
          toInsert = '\n' + toInsert;
        }
      }

      // Insert text.
      view.insert(insertAt, toInsert);

      // Do something with annotations?
      if (modifyAction.getBundledAnnotations() != null) {
        int annotationStart = view.transformToXmlOffset(insertAt);
        int annotationEnd = view.transformToXmlOffset(insertAt + toInsert.length());

        for (RangedAnnotation<String> annotation :
            doc.rangedAnnotations(annotationStart, annotationEnd, null)) {
          setDocumentAnnotation(
              operation, doc, annotationStart, annotationEnd, annotation.key(), null);
        }

        for (BundledAnnotation ia : modifyAction.getBundledAnnotations()) {
          setDocumentAnnotation(operation, doc, annotationStart, annotationEnd, ia.key, ia.value);
        }
      }
      return toInsert.length();
    } else {
      Element element = modifyAction.getElement(valueIndex);
      if (element != null) {
        if (element.isGadget()) {
          Gadget gadget = (Gadget) element;
          XmlStringBuilder xml =
              GadgetXmlUtil.constructXml(gadget.getUrl(), "", gadget.getAuthor(), null,
                  gadget.getProperties());
          // TODO (Yuri Z.) Make it possible to specify a location to insert the
          // gadget and implement insertion at the specified location.
          LineContainers.appendLine(doc, xml);
        } else if (element.isFormElement()) {
          XmlStringBuilder xml = ElementSerializer.apiElementToXml(element);
          LineContainers.appendLine(doc, xml);
        } else {
          // TODO(ljvderijk): Inserting other elements.
          throw new UnsupportedOperationException(
              "Can't insert other elements than text and gadgets at the moment");
        }
      }
      // should return 1 since elements have a length of 1 in the ApiView;
      return 1;
    }
  }

  /**
   * Updates elements in the document.
   * <b>Note</b>: Only gadget elements are supported, for now.
   *
   * @param operation the operation the operation that wants to update elements.
   * @param doc the document to update elements in.
   * @param view the {@link ApiView} of that document.
   * @param hitIterator the iterator over the places where to update elements.
   * @param modifyAction the action that specifies what to update.
   * @throws InvalidRequestException if something goes wrong.
   */
  private void updateElement(OperationRequest operation, Document doc, ApiView view,
      DocumentHitIterator hitIterator, DocumentModifyAction modifyAction)
      throws InvalidRequestException {
    Range range = null;
    for (int index = 0; ((range = hitIterator.next()) != null); ++index) {
      Element element = modifyAction.getElement(index);
      if (element != null) {
        if (element.isGadget()) {
          int xmlStart = view.transformToXmlOffset(range.getStart());
          Doc.E docElem = Point.elementAfter(doc, doc.locate(xmlStart));
          updateExistingGadgetElement(doc, docElem, element);
        } else {
          // TODO (Yuri Z.) Updating other elements.
          throw new UnsupportedOperationException(
              "Can't update other elements than gadgets at the moment");
        }
      }
    }
  }

  /**
   * Updates the existing gadget element properties.
   *
   * @param doc the document to update elements in.
   * @param existingElement the gadget element to update.
   * @param element the element that describes what existingElement should be
   *        updated with.
   * @throws InvalidRequestException
   */
  private void updateExistingGadgetElement(Document doc, Doc.E existingElement,
      Element element) throws InvalidRequestException {
    Preconditions.checkArgument(element.isGadget(),
        "Called with non-gadget element type %s", element.getType());

    String url = element.getProperty("url");
    if (url != null) {
      doc.setElementAttribute(existingElement, "url", url);
    }
    Map<String, Doc.E> children = Maps.newHashMap();
    for (Doc.N child = doc.getFirstChild(existingElement); child != null; child =
        doc.getNextSibling(child)) {
      Doc.E childAsElement = doc.asElement(child);
      if (childAsElement != null) {
        String key = doc.getTagName(childAsElement);
        if (key.equals("state")) {
          key = key + " " + doc.getAttributes(childAsElement).get("name");
        }
        children.put(key, childAsElement);
      }
    }

    for (Map.Entry<String, String> property : element.getProperties().entrySet()) {
      // TODO (Yuri Z.) Support updating gadget metadata (author, title, thumbnail...)
      // and user preferences.
      String key = null;
      String tag = null;
      if (property.getKey().equals("title") || property.getKey().equals("thumbnail")
          || property.getKey().equals("author")) {
        key = property.getKey();
        tag = property.getKey();
      } else if (!property.getKey().equals("name") && !property.getKey().equals("pref")
          && !property.getKey().equals("url")) {
        // A state variable.
        key = "state " + property.getKey();
        tag = "state";
      } else {
        continue;
      }

      String val = property.getValue();
      Doc.E child = children.get(key);
      if (val == null) {
        // Delete the property if value is null.
        if (child == null) {
          // Property does not exist, skipping.
          continue;
        }
        doc.deleteNode(child);
      } else {
        if (child != null) {
          if (tag.equals("state")) {
            doc.setElementAttribute(child, "value", val);
          } else {
            doc.emptyElement(child);
            Point<Doc.N> point = Point.<Doc.N> inElement(child, null);
            doc.insertText(point, val);
          }
        } else {
          XmlStringBuilder xml = GadgetXmlUtil.constructStateXml(property.getKey(), val);
          doc.insertXml(Point.<Doc.N> inElement(existingElement, null), xml);
        }
      }
    }
  }

  public static DocumentModifyService create() {
    return new DocumentModifyService();
  }
}
