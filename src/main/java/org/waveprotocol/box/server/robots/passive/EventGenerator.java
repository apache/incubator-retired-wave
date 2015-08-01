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

package org.waveprotocol.box.server.robots.passive;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.wave.api.BlipData;
import com.google.wave.api.Context;
import com.google.wave.api.Gadget;
import com.google.wave.api.data.converter.ContextResolver;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.*;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.Capability;
import com.google.wave.api.robot.RobotName;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.conversation.*;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Doc.T;
import org.waveprotocol.wave.model.document.DocHandler;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.AnnotationChanged;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.AttributesModified;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentInserted;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.operation.wave.BasicWaveletOperationContextFactory;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.*;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.wave.opbased.WaveletListenerImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Robot API Events from operations applied to a Wavelet.
 *
 * <p>
 * Events that exist in the API:
 * <li>WaveletBlipCreated (DONE)</li>
 * <li>WaveletBlipRemoved (DONE)</li>
 * <li>WaveletParticipantsChanged (DONE)</li>
 * <li>WaveletSelfAdded (DONE)</li>
 * <li>WaveletSelfRemoved (DONE)</li>
 * <li>DocumentChanged (DONE)</li>
 * <li>AnnotatedTextChanged (DONE)</li>
 * <li>FormButtonClicked (TBD)</li>
 * <li>GadgetStateChanged (DONE)</li>
 * <li>BlipContributorChanged (TBD)</li>
 * <li>WaveletTagsChanged (TBD)</li>
 * <li>WaveletTitleChanged (TBD)</li>
 * <li>BlipSubmitted (Will not be supported, submit ops will be phased out)</li>
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class EventGenerator {

  private static class EventGeneratingWaveletListener extends WaveletListenerImpl {
    @SuppressWarnings("unused")
    private final Map<EventType, Capability> capabilities;

    /**
     * Creates a {@link WaveletListener} which will generate events according to
     * the capabilities.
     *
     * @param capabilities the capabilities which we are interested in.
     */
    public EventGeneratingWaveletListener(Map<EventType, Capability> capabilities) {
      this.capabilities = capabilities;
    }
    // TODO(ljvderijk): implement more events. This class should listen for
    // non-conversational blip changes and robot data documents as indicated by
    // IdConstants.ROBOT_PREFIX
  }

  private class EventGeneratingConversationListener extends ConversationListenerImpl {
    private final Map<EventType, Capability> capabilities;
    private final Conversation conversation;
    private final EventMessageBundle messages;

    // Event collectors
    private final List<String> participantsAdded = Lists.newArrayList();
    private final List<String> participantsRemoved = Lists.newArrayList();

    // Changes for each delta
    private ParticipantId deltaAuthor;
    private Long deltaTimestamp;

    /**
     * Creates a {@link ObservableConversation.Listener} which will generate
     * events according to the capabilities.
     *
     * @param conversation the conversation we are observing.
     * @param capabilities the capabilities which we are interested in.
     * @param messages the bundle to put the events in.
     */
    public EventGeneratingConversationListener(Conversation conversation,
        Map<EventType, Capability> capabilities, EventMessageBundle messages, RobotName robotName) {
      this.conversation = conversation;
      this.capabilities = capabilities;
      this.messages = messages;
    }

    /**
     * Prepares this listener for events coming from a single delta.
     *
     * @param author the author of the delta.
     * @param timestamp the timestamp of the delta.
     */
    public void deltaBegin(ParticipantId author, long timestamp) {
      Preconditions.checkState(
          deltaAuthor == null && deltaTimestamp == null, "DeltaEnd wasn't called");
      Preconditions.checkNotNull(author, "Author should not be null");
      Preconditions.checkNotNull(timestamp, "Timestamp should not be null");

      deltaAuthor = author;
      deltaTimestamp = timestamp;
    }

    @Override
    public void onParticipantAdded(ParticipantId participant) {
      if (capabilities.containsKey(EventType.WAVELET_PARTICIPANTS_CHANGED)) {
        boolean removedBefore = participantsRemoved.remove(participant.getAddress());
        if (!removedBefore) {
          participantsAdded.add(participant.getAddress());
        }
      }

      // This deviates from Google Wave production which always sends this
      // event, even if it wasn't present in your capabilities.
      if (capabilities.containsKey(EventType.WAVELET_SELF_ADDED) && participant.equals(robotId)) {
        // The robot has been added
        String rootBlipId = ConversationUtil.getRootBlipId(conversation);
        WaveletSelfAddedEvent event = new WaveletSelfAddedEvent(
            null, null, deltaAuthor.getAddress(), deltaTimestamp, rootBlipId);
        addEvent(event, capabilities, rootBlipId, messages);
      }
    }

    @Override
    public void onParticipantRemoved(ParticipantId participant) {
      if (capabilities.containsKey(EventType.WAVELET_PARTICIPANTS_CHANGED)) {
        participantsRemoved.add(participant.getAddress());
      }

      if (capabilities.containsKey(EventType.WAVELET_SELF_REMOVED) && participant.equals(robotId)) {
        String rootBlipId = ConversationUtil.getRootBlipId(conversation);
        WaveletSelfRemovedEvent event = new WaveletSelfRemovedEvent(
            null, null, deltaAuthor.getAddress(), deltaTimestamp, rootBlipId);
        addEvent(event, capabilities, rootBlipId, messages);
      }
    }

    @Override
    public void onBlipAdded(ObservableConversationBlip blip) {
      if (capabilities.containsKey(EventType.WAVELET_BLIP_CREATED)) {
        String rootBlipId = ConversationUtil.getRootBlipId(conversation);
        WaveletBlipCreatedEvent event = new WaveletBlipCreatedEvent(
            null, null, deltaAuthor.getAddress(), deltaTimestamp, rootBlipId, blip.getId());
        addEvent(event, capabilities, rootBlipId, messages);
      }
    }

    @Override
    public void onBlipDeleted(ObservableConversationBlip blip) {
      if (capabilities.containsKey(EventType.WAVELET_BLIP_REMOVED)) {
        String rootBlipId = ConversationUtil.getRootBlipId(conversation);
        WaveletBlipRemovedEvent event = new WaveletBlipRemovedEvent(
            null, null, deltaAuthor.getAddress(), deltaTimestamp, rootBlipId, blip.getId());
        addEvent(event, capabilities, rootBlipId, messages);
      }
    }

    /**
     * Generates the events that are collected over the span of one delta.
     */
    public void deltaEnd() {
      if (!participantsAdded.isEmpty() || !participantsRemoved.isEmpty()) {
        String rootBlipId = ConversationUtil.getRootBlipId(conversation);

        WaveletParticipantsChangedEvent event =
            new WaveletParticipantsChangedEvent(null, null, deltaAuthor.getAddress(),
                deltaTimestamp, rootBlipId, participantsAdded, participantsRemoved);
        addEvent(event, capabilities, rootBlipId, messages);
      }
      clearOncePerDeltaCollectors();

      deltaAuthor = null;
      deltaTimestamp = null;
    }

    /**
     * Clear the data structures responsible for collecting data for events that
     * should only be fired once per delta.
     */
    private void clearOncePerDeltaCollectors() {
      participantsAdded.clear();
      participantsRemoved.clear();
    }
  }

  private class EventGeneratingDocumentHandler implements DocHandler {

    /** Public so we can manage the subscription */
    public final ObservableDocument doc;

    private final ConversationBlip blip;
    private final Map<EventType, Capability> capabilities;
    private final EventMessageBundle messages;
    private ParticipantId deltaAuthor;
    private Long deltaTimestamp;

    /**
     * Set to true if a {@link DocumentChangedEvent} has been generated by this
     * handler.
     */
    private boolean documentChangedEventGenerated;

    private EventDataConverter converter;
    private Wavelet wavelet;

    public EventGeneratingDocumentHandler(ObservableDocument doc, ConversationBlip blip,
        Map<EventType, Capability> capabilities, EventMessageBundle messages,
        ParticipantId deltaAuthor, Long deltaTimestamp, Wavelet wavelet,
        EventDataConverter converter) {
      this.doc = doc;
      this.blip = blip;
      this.capabilities = capabilities;
      this.messages = messages;
      this.converter = converter;
      this.wavelet = wavelet;
      setAuthorAndTimeStamp(deltaAuthor, deltaTimestamp);
    }

    @Override
    public void onDocumentEvents(EventBundle<N, E, T> event) {
      Iterable<DocumentEvent<N, E, T>> eventComponents = event.getEventComponents();

      for (DocumentEvent<N, E, T> eventComponent : eventComponents) {
        if (eventComponent.getType() == DocumentEvent.Type.ANNOTATION_CHANGED) {
          if (capabilities.containsKey(EventType.ANNOTATED_TEXT_CHANGED)) {
            AnnotationChanged<N, E, T> anotationChangedEvent =
                (AnnotationChanged<N, E, T>) eventComponent;
            AnnotatedTextChangedEvent apiEvent =
                new AnnotatedTextChangedEvent(null, null, deltaAuthor.getAddress(), deltaTimestamp,
                    blip.getId(), anotationChangedEvent.key, anotationChangedEvent.newValue);
            addEvent(apiEvent, capabilities, blip.getId(), messages);
          }
        } else {
          // used to distinguish between attribute changes and gadget state
          // changes
          Boolean gadgetStateChangeEvent = false;
          if (eventComponent.getType() == DocumentEvent.Type.ATTRIBUTES) {
            if (capabilities.containsKey(EventType.GADGET_STATE_CHANGED)) {
              Map<String, String> oldState = new HashMap<>();
              Integer index = -1;
              try {
                AttributesModified<N, E, T> attributesModified =
                    (AttributesModified<N, E, T>) eventComponent;
                // When a gadget state changes, the AttributesModifies event has
                // always
                // an oldValue map of the form {"value", something} (key is
                // always value).
                // To obtain the key of the changed state, the attribute "name"
                // has to be obtained
                // from the Element of the AttributesModified event.
                String name =
                    ((org.waveprotocol.wave.model.document.raw.impl.Element) attributesModified
                        .getElement()).getAttribute("name");
                String oldValue = attributesModified.getOldValues().get("value");
                if (name != null || oldValue != null) {
                  oldState.put(name, oldValue);
                }
                BlipData b = converter.toBlipData(blip, wavelet, messages);
                Map<Integer, com.google.wave.api.Element> elements = b.getElements();
                Set<Integer> keys = elements.keySet();
                // The gadget element provided by the eventComponent
                org.waveprotocol.wave.model.document.raw.impl.Element rawGadget =
                    ((Node) attributesModified.getElement()).getParentElement();
                for (Integer key : keys) {
                  try {
                    Gadget gadget = (Gadget) elements.get(key);
                    if (sameGadgets(rawGadget, gadget)) {
                      index = key;
                      break;
                    }
                  } catch (ClassCastException e) {
                    // if it is not a gadget we do not compare them
                  }
                }
              } catch (ClassCastException e) {
                e.printStackTrace();
              }
              if (oldState.size() != 0 && index != -1) {
                // if the attribute changed belongs to a gadget
                gadgetStateChangeEvent = true;
                final GadgetStateChangedEvent gadgetEvent =
                    new GadgetStateChangedEvent(null, messages, deltaAuthor.getAddress(),
                        deltaTimestamp, blip.getId(), index, oldState);
                addEvent(gadgetEvent, capabilities, blip.getId(), messages);
              }
            }
          }
          if (capabilities.containsKey(EventType.FORM_BUTTON_CLICKED)) {
            if (eventComponent.getType() == DocumentEvent.Type.CONTENT_INSERTED) {
              ContentInserted<N, E, T> contentInserted = (ContentInserted<N, E, T>) eventComponent;
              org.waveprotocol.wave.model.document.raw.impl.Element elementInserted =
                ((org.waveprotocol.wave.model.document.raw.impl.Element)
                  contentInserted.getSubtreeElement());
              if (elementInserted.getTagName().equals("click")) {
                FormButtonClickedEvent buttonClickedEvent =
                    new FormButtonClickedEvent(null, null,
                        elementInserted.getAttribute("clicker"), Long.decode(elementInserted
                            .getAttribute("time")), blip.getId(), elementInserted
                            .getParentElement().getAttribute("name"));
                addEvent(buttonClickedEvent, capabilities, blip.getId(), messages);
              }
            }
          }
          if (capabilities.containsKey(EventType.DOCUMENT_CHANGED)
              && !documentChangedEventGenerated && !gadgetStateChangeEvent) {
            DocumentChangedEvent apiEvent =
                new DocumentChangedEvent(null, null, deltaAuthor.getAddress(), deltaTimestamp,
                    blip.getId());
            addEvent(apiEvent, capabilities, blip.getId(), messages);
            // Only one documentChangedEvent should be generated per bundle.
            documentChangedEventGenerated = true;
          }
        }
      }
    }

    /**
     * Sets the author and timestamp for the events that will be coming in.
     * Should be changed at least for every delta that will touch the document
     * that the handler is listening to.
     *
     * @param author the author of the delta.
     * @param timestamp the timestamp at which the delta is applied.
     */
    public void setAuthorAndTimeStamp(ParticipantId author, long timestamp) {
      Preconditions.checkNotNull(author, "Author should not be null");
      Preconditions.checkNotNull(timestamp, "Timestamp should not be null");
      this.deltaAuthor = author;
      this.deltaTimestamp = timestamp;
    }

    /**
     * Check if an {@link org.waveprotocol.wave.model.document.raw.impl.Element}
     * is and a {@link Gadget}
     *
     * @param rawElement
     * @param element
     * @return
     */
    private boolean sameGadgets(org.waveprotocol.wave.model.document.raw.impl.Element rawElement,
        Gadget element) {
      String ifr1 = rawElement.getAttribute("ifr");
      String ifr2 = element.getProperty("ifr");
      return (ifr1 != null && ifr1.equals(ifr2));
    }
  }

  /**
   * Adds an {@link Event} to the given {@link EventMessageBundle}.
   *
   * If a blip id is specified this will be added to the
   * {@link EventMessageBundle}'s required blips list with the context given by
   * the robot's capabilities. If a robot does not specify a context for this
   * event the default context will be used. Ergo this code is not responsible
   * for filtering operations that a robot is not interested in.
   *
   * @param event to add.
   * @param capabilities the capabilities to get the context from.
   * @param blipId id of the blip this event is related to, may be null.
   * @param messages {@link EventMessageBundle} to edit.
   */
  private void addEvent(Event event, Map<EventType, Capability> capabilities, String blipId,
      EventMessageBundle messages) {
    if (!isEventFilteredOut(event)) {
      // Add the given blip to the required blip lists with the context
      // specified by the robot's capabilities.
      if (!Strings.isNullOrEmpty(blipId)) {
        Capability capability = capabilities.get(event.getType());
        List<Context> contexts;
        if (capability == null) {
          contexts = Capability.DEFAULT_CONTEXT;
        } else {
          contexts = capability.getContexts();
        }
        messages.requireBlip(blipId, contexts);
      }
      // Add the event to the bundle.
      messages.addEvent(event);
    }
  }

  /**
   * Checks whether the event should be filtered out. It can happen
   * if the robot received several deltas where in some delta it is added to
   * the wavelet but it didn't receive the WAVELET_SELF_ADDED event yet.
   * Or if robot already received WAVELET_SELF_REMOVED
   * event - then it should not receive events after that.
   *
   * @param event  the event to filter.
   * @return true if the event should be filtered out
   */
  protected boolean isEventFilteredOut(Event event) {
    boolean isEventSuspensionOveriden = false;
    if (event.getType().equals(EventType.WAVELET_SELF_REMOVED)) {
      // Stop processing events.
      isEventProcessingSuspended = true;
      // Allow robot receive WAVELET_SELF_REMOVED event, but suspend after that.
      isEventSuspensionOveriden = true;
    }
    if (event.getType().equals(EventType.WAVELET_SELF_ADDED)) {
      // Start processing events.
      isEventProcessingSuspended = false;
    }
    if ((isEventProcessingSuspended && !isEventSuspensionOveriden)
        || event.getModifiedBy().equals(robotName.toParticipantAddress())) {
      // Robot was removed from wave or this is self generated event.
      return true;
    }
    return false;
  }

  /**
   * The name of the Robot to which this {@link EventGenerator} belongs. Used
   * for events where "self" is important.
   */
  private final RobotName robotName;

  /** Used to create conversations. */
  private final ConversationUtil conversationUtil;

  /**
   * Indicates that robot was removed from wavelet and thus event processing
   * should be suspended.
   */
  private boolean isEventProcessingSuspended;

  private final ParticipantId robotId;

  /**
   * Constructs a new {@link EventGenerator} for the robot with the given name.
   *
   * @param robotName the name of the robot.
   * @param conversationUtil used to create conversations.
   */
  public EventGenerator(RobotName robotName, ConversationUtil conversationUtil) {
    this.robotName = robotName;
    this.conversationUtil = conversationUtil;
    this.robotId = ParticipantId.ofUnsafe(robotName.toParticipantAddress());
  }

  /**
   * Generates the {@link EventMessageBundle} for the specified capabilities.
   *
   * @param waveletAndDeltas for which the events are to be generated
   * @param capabilities the capabilities to filter events on
   * @param converter converter for generating the API implementations of
   *        WaveletData and BlipData.
   * @returns true if an event was generated, false otherwise
   */
  public EventMessageBundle generateEvents(WaveletAndDeltas waveletAndDeltas,
      Map<EventType, Capability> capabilities, EventDataConverter converter) {
    EventMessageBundle messages = new EventMessageBundle(robotName.toEmailAddress(), "");
    ObservableWaveletData snapshot =
        WaveletDataUtil.copyWavelet(waveletAndDeltas.getSnapshotBeforeDeltas());
    isEventProcessingSuspended = !snapshot.getParticipants().contains(robotId);

    if (robotName.hasProxyFor()) {
      // This robot is proxying so set the proxy field.
      messages.setProxyingFor(robotName.getProxyFor());
    }

    // Sending any operations will cause an exception.
    OpBasedWavelet wavelet =
        new OpBasedWavelet(snapshot.getWaveId(), snapshot,
            // This doesn't thrown an exception, the sinks will
            new BasicWaveletOperationContextFactory(null),
            ParticipationHelper.DEFAULT, SilentOperationSink.VOID, SilentOperationSink.VOID);

    ObservableConversation conversation = getRootConversation(wavelet);

    if (conversation == null) {
      return messages;
    }

    // Start listening
    EventGeneratingConversationListener conversationListener =
        new EventGeneratingConversationListener(conversation, capabilities, messages, robotName);
    conversation.addListener(conversationListener);
    EventGeneratingWaveletListener waveletListener =
        new EventGeneratingWaveletListener(capabilities);
    wavelet.addListener(waveletListener);

    Map<String, EventGeneratingDocumentHandler> docHandlers = Maps.newHashMap();
    try {
      for (TransformedWaveletDelta delta : waveletAndDeltas.getDeltas()) {
        // TODO(ljvderijk): Set correct timestamp and hashed version once
        // wavebus sends them along
        long timestamp = 0L;
        conversationListener.deltaBegin(delta.getAuthor(), timestamp);

        for (WaveletOperation op : delta) {
          // Check if we need to attach a doc handler.
          if ((op instanceof WaveletBlipOperation)) {
            attachDocHandler(conversation, op, docHandlers, capabilities, messages,
                delta.getAuthor(), timestamp, wavelet, converter);
          }
          op.apply(snapshot);
        }
        conversationListener.deltaEnd();
      }
    } catch (OperationException e) {
      throw new IllegalStateException("Operation failed to apply when generating events", e);
    } finally {
      conversation.removeListener(conversationListener);
      wavelet.removeListener(waveletListener);
      for (EventGeneratingDocumentHandler docHandler : docHandlers.values()) {
        docHandler.doc.removeListener(docHandler);
      }
    }

    if (messages.getEvents().isEmpty()) {
      // No events found, no need to resolve contexts
      return messages;
    }

    // Resolve the context of the bundle now that all events have been
    // processed.
    ContextResolver.resolveContext(messages, wavelet, conversation, converter);

    return messages;
  }

  /**
   * Attaches a doc handler to the blip the operation applies to.
   *
   * @param conversation the conversation the op is to be applied to.
   * @param op the op to be applied
   * @param docHandlers the list of attached dochandlers.
   * @param capabilities the capabilities of the robot.
   * @param messages the bundle to put the generated events in.
   * @param deltaAuthor the author of the events generated.
   * @param timestamp the timestamp at which these events occurred.
   */
  private void attachDocHandler(ObservableConversation conversation, WaveletOperation op,
      Map<String, EventGeneratingDocumentHandler> docHandlers,
      Map<EventType, Capability> capabilities, EventMessageBundle messages,
      ParticipantId deltaAuthor, long timestamp, Wavelet wavelet, EventDataConverter converter) {
    WaveletBlipOperation blipOp = (WaveletBlipOperation) op;
    String blipId = blipOp.getBlipId();
    // Ignoring the documents outside the conversation such as tags
    // and robot data docs.
    ObservableConversationBlip blip = conversation.getBlip(blipId);
    if (blip != null) {
      String blipId1 = blip.getId();

      EventGeneratingDocumentHandler docHandler = docHandlers.get(blipId1);
      if (docHandler == null) {
        ObservableDocument doc = (ObservableDocument) blip.getContent();
        docHandler =
            new EventGeneratingDocumentHandler(doc, blip, capabilities, messages, deltaAuthor,
                timestamp, wavelet, converter);
        doc.addListener(docHandler);
        docHandlers.put(blipId1, docHandler);
      } else {
        docHandler.setAuthorAndTimeStamp(deltaAuthor, timestamp);
      }
    }
  }

  /**
   * Returns the root conversation from the given wavelet. Or null if there is
   * none.
   *
   * @param wavelet the wavelet to get the conversation from.
   */
  private ObservableConversation getRootConversation(ObservableWavelet wavelet) {
    if (!WaveletBasedConversation.waveletHasConversation(wavelet)) {
      // No conversation present, bail.
      return null;
    }

    ObservableConversation conversation = conversationUtil.buildConversation(wavelet).getRoot();
    if (conversation.getRootThread().getFirstBlip() == null) {
      // No root blip is present, this will cause Robot API code
      // to fail when resolving the context of events. This might be fixed later
      // on by making changes to the ContextResolver.
      return null;
    }
    return conversation;
  }
}
