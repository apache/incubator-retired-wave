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

package com.google.wave.api.data.converter.v21;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.wave.api.Annotation;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.BlipData;
import com.google.wave.api.Range;
import com.google.wave.api.data.ApiView;
import com.google.wave.api.data.ElementSerializer;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.WaveletData;

import org.waveprotocol.wave.model.account.DocumentBasedRoles;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.TagsDocument;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An implementation of {@link EventDataConverter} for all protocol versions
 * that are less than or equal to v0.21.
 *
 */
public class EventDataConverterV21 implements EventDataConverter {

  @Override
  public WaveletData toWaveletData(Wavelet wavelet, Conversation conversation,
      EventMessageBundle eventMessageBundle) {
    final WaveletData waveletData = new WaveletData();
    waveletData.setCreationTime(wavelet.getCreationTime());
    waveletData.setCreator(wavelet.getCreatorId().getAddress());
    waveletData.setWaveId(ApiIdSerializer.instance().serialiseWaveId(wavelet.getWaveId()));
    waveletData.setWaveletId(ApiIdSerializer.instance().serialiseWaveletId(wavelet.getId()));
    waveletData.setLastModifiedTime(wavelet.getLastModifiedTime());
    waveletData.setParticipants(idsToParticipantIdList(wavelet.getParticipantIds()));
    waveletData.setRootBlipId(conversation.getRootThread().getFirstBlip().getId());
    waveletData.setTitle(getTitle(wavelet, conversation));
    waveletData.setVersion(wavelet.getVersion());

    // Add Data Docs. All data documents are silently name spaced under the
    // robot prefix to avoid conflicts. Any docId containing a '+' will be
    // ignored for now.
    for (String documentId : wavelet.getDocumentIds()) {
      if (IdUtil.isRobotDocId(documentId)) {
        String[] parts = IdUtil.split(documentId);
        if (parts.length == 2) {
          Document document = wavelet.getDocument(documentId);
          String val = XmlStringBuilder.innerXml(document).getXmlString();
          waveletData.setDataDocument(parts[1], val);
        }
      }
    }

    // Add the tags.
    if (wavelet.getDocument(IdConstants.TAGS_DOC_ID) != null) {
      @SuppressWarnings({"unchecked", "rawtypes"})
      TagsDocument tags = new TagsDocument(wavelet.getDocument(IdConstants.TAGS_DOC_ID));
      tags.addListener(new TagsDocument.Listener() {
        @Override
        public void onAdd(String tagName) {
          waveletData.addTag(tagName);
        }
        @Override
        public void onRemove(int tagPosition) {
          // Not called.
        }});
      tags.processInitialState();
    }

    // Add the participant roles.
    ObservableDocument rolesDocument = wavelet.getDocument(IdConstants.ROLES_DATA_DOC_ID);
    if (rolesDocument != null) {
      DocumentBasedRoles roles = DocumentBasedRoles.create(rolesDocument);
      for (ParticipantId participantId : wavelet.getParticipantIds()) {
        waveletData.setParticipantRole(participantId.getAddress(),
            roles.getRole(participantId).name());
      }
    }
    return waveletData;
  }

  @Override
  public BlipData toBlipData(ConversationBlip blip, Wavelet wavelet,
      EventMessageBundle eventMessageBundle) {
    ConversationBlip parentBlip = findBlipParent(blip);
    BlipData blipData = new BlipData();
    blipData.setCreator(blip.getAuthorId().getAddress());
    blipData.setContributors(idsToParticipantIdList(blip.getContributorIds()));
    blipData.setBlipId(blip.getId());
    blipData.setLastModifiedTime(blip.getLastModifiedTime());
    blipData.setVersion(blip.getLastModifiedVersion());
    blipData.setParentBlipId(parentBlip == null ? null : parentBlip.getId());
    blipData.setWaveId(ApiIdSerializer.instance().serialiseWaveId(wavelet.getWaveId()));
    blipData.setWaveletId(ApiIdSerializer.instance().serialiseWaveletId(wavelet.getId()));
    blipData.setChildBlipIds(toBlipIdList(findBlipChildren(blip)));

    ApiView apiView = new ApiView(blip.getContent(), wavelet);
    // Set content.
    blipData.setContent(apiView.apiContents());
    // Set Annotations.
    blipData.setAnnotations(extractAnnotations(blip.getContent(), apiView));
    // blip.getContent().rangedAnnotations(0, blip.getContent().size(), null),
    // Set Form Elements.
    blipData.setElements(ElementSerializer.serialize(blip.getContent(), wavelet));
    return blipData;
  }

  /**
   * Finds the children of a blip, defined as the next sibling blip and the
   * first blip in each reply thread.
   *
   * @param blip the blip.
   * @return the children of the given blip.
   */
  @Override
  public List<ConversationBlip> findBlipChildren(ConversationBlip blip) {
    List<ConversationBlip> siblings = Lists.newArrayList();
    ConversationBlip nextSibling = findNextSibling(blip);
    if (nextSibling != null) {
      siblings.add(nextSibling);
    }
    for (ConversationThread reply : blip.getReplyThreads()) {
      if (reply.getFirstBlip() != null) {
        siblings.add(reply.getFirstBlip());
      }
    }
    return siblings;
  }

  /**
   * Finds the parent of a blip. The parent is the preceding blip in the thread,
   * or the blip to which the thread is a reply for the first blip in a thread.
   * The first blip of the root thread has no parent.
   *
   * @param blip the blip.
   * @return the blip's parent, or {@code null} if the blip is the first blip
   *     in a conversation.
   */
  @Override
  public ConversationBlip findBlipParent(ConversationBlip blip) {
    ConversationThread containingThread = blip.getThread();
    if (containingThread.getFirstBlip() == blip
        && containingThread != blip.getConversation().getRootThread()) {
      return containingThread.getParentBlip();
    }
    return findPreviousSibling(blip);
  }

  /**
   * Converts a collection of {@link ParticipantId}s to a list of addresses.
   *
   * @param participantIds the participant ids to convert.
   * @return a list of addresses.
   */
  public List<String> idsToParticipantIdList(Collection<ParticipantId> participantIds) {
    List<String> addresses = Lists.newArrayListWithCapacity(participantIds.size());
    for (ParticipantId id : participantIds) {
      addresses.add(id.getAddress());
    }
    return addresses;
  }

  /**
   * Finds the previous sibling of a blip in a thread. The first blip in a
   * thread has no previous sibling.
   *
   * @param blip the blip.
   * @return the previous sibling of the blip, or {@code null}.
   */
  @VisibleForTesting
  static ConversationBlip findPreviousSibling(ConversationBlip blip) {
    ConversationThread thread = blip.getThread();
    ConversationBlip previous = null;
    for (ConversationBlip sibling : thread.getBlips()) {
      if (sibling == blip) {
        break;
      }
      previous = sibling;
    }
    return previous;
  }

  /**
   * Finds the next sibling of a blip in a thread. The last blip in a thread has
   * no next sibling.
   *
   * @param blip the blip.
   * @return the next sibling of the blip, or {@code null} if blip is the last
   *     blip in a thread.
   */
  @VisibleForTesting
  static ConversationBlip findNextSibling(ConversationBlip blip) {
    ConversationThread thread = blip.getThread();
    Iterator<? extends ConversationBlip> blips = thread.getBlips().iterator();
    boolean foundBlip = false;
    while (!foundBlip && blips.hasNext()) {
      if (blips.next() == blip) {
        foundBlip = true;
      }
    }
    return blips.hasNext() ? blips.next() : null;
  }

  /**
   * Retrieves the title of a {@link Wavelet}.
   *
   * @param wavelet The {@link Wavelet} to retrieve the title from.
   * @param conversation The wavelet conversation
   * @return the title of the {@link Wavelet}, or an empty string if it has
   *     no title.
   */
  private static String getTitle(Wavelet wavelet, Conversation conversation) {
    ConversationThread rootThread = conversation.getRootThread();
    if (rootThread == null) {
      return "";
    }
    ConversationBlip firstBlip = rootThread.getFirstBlip();
    if (firstBlip == null) {
      return "";
    }
    Document doc = firstBlip.getContent();
    return TitleHelper.extractTitle(doc);
  }

  /**
   * Extracts the blip ids of the given list of blips.
   *
   * @param children the blips.
   * @return the blip ids of the blips.
   */
  private static List<String> toBlipIdList(List<ConversationBlip> children) {
    List<String> ids = Lists.newArrayListWithCapacity(children.size());
    for (ConversationBlip child : children) {
      ids.add(child.getId());
    }
    return ids;
  }

  /**
   * Extracts all annotations that span inside the body tag of the given
   * document.
   *
   * @param doc the document to get the annotations from.
   * @param apiView provides a utility function to convert an xml offset point
   *     into text offset.
   * @return the annotations represented as a list of {@link Annotation}.
   */
  private static List<Annotation> extractAnnotations(Document doc, ApiView apiView) {
    List<Annotation> result = Lists.newArrayList();
    for (RangedAnnotation<String> annotation : doc.rangedAnnotations(0, doc.size(), null)) {
      if (annotation.key() != null && annotation.value() != null) {
        int start = apiView.transformToTextOffset(annotation.start());
        int end = apiView.transformToTextOffset(annotation.end());
        result.add(new Annotation(annotation.key(), annotation.value(), new Range(start, end)));
      }
    }
    return result;
  }
}
