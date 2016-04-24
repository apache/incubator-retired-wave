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

package org.waveprotocol.examples.robots.echoey;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.wave.api.AbstractRobot;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Context;
import com.google.wave.api.Wavelet;
import com.google.wave.api.event.AnnotatedTextChangedEvent;
import com.google.wave.api.event.DocumentChangedEvent;
import com.google.wave.api.event.WaveletBlipCreatedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Robot that echoes the changes to a wave.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class Echoey extends AbstractRobot {

  /** Annotation for text by Echoey */
  private static final String ECHOEY_ANNOTATION = "www.waveprotocol.org/echoey";

  private final Map<String, String> shadowBlipMap;

  public Echoey() {
    shadowBlipMap = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).<String, String>build().asMap();
  }

  @Override
  protected String getRobotName() {
    return "Echoey";
  }

  @Override
  protected String getRobotProfilePageUrl() {
    return "http://www.waveprotocol.org/";
  }

  @Capability(contexts = {Context.SELF, Context.SIBLINGS})
  @Override
  public void onWaveletBlipCreated(WaveletBlipCreatedEvent event) {
    Blip blip = event.getNewBlip();
    if (!isShadowBlip(blip)) {
      createShadowBlip(blip);
    }
  }

  @Capability(contexts = {Context.SELF, Context.SIBLINGS})
  @Override
  public void onDocumentChanged(DocumentChangedEvent event) {
    Blip blip = event.getBlip();
    if (!isShadowBlip(blip)) {
      createOrUpdateShadowBlip(blip);
    }
  }

  @Capability(contexts = {Context.SELF, Context.SIBLINGS})
  @Override
  public void onAnnotatedTextChanged(AnnotatedTextChangedEvent event) {
    Blip blip = event.getBlip();
    if (!isShadowBlip(blip)) {
      createOrUpdateShadowBlip(blip);
    }
  }

  /**
   * Creates a new shadow blip.
   *
   *  Note that this method will not know the id of the new blip since the
   * server will generate it.
   *
   * @param blip the blip to create a new shadow blip for.
   */
  private void createShadowBlip(Blip blip) {
    Blip newBlip = blip.continueThread();
    newBlip.all().replace(blip.getContent());
    newBlip.all().annotate(ECHOEY_ANNOTATION, blip.getBlipId());
  }

  /**
   * Creates or updates an existing shadow blip that should shadow the given
   * blip.
   *
   * @param blipToShadow the blip that should be shadowed.
   */
  private void createOrUpdateShadowBlip(Blip blipToShadow) {
    Wavelet wavelet = blipToShadow.getWavelet();
    String blipId = blipToShadow.getBlipId();
    if (shadowBlipMap.containsKey(blipId)) {
      Blip shadowBlip = wavelet.getBlip(shadowBlipMap.get(blipId));
      updateShadowBlip(shadowBlip, blipToShadow);
    } else {
      updateShadowMap(wavelet);

      if (!shadowBlipMap.containsKey(blipId)) {
        createShadowBlip(blipToShadow);
      } else {
        // Update existing shadow Blip
        Blip shadowBlip = wavelet.getBlip(shadowBlipMap.get(blipId));
        updateShadowBlip(shadowBlip, blipToShadow);
      }
    }
  }

  /**
   * Updates an existing shadow blip.
   *
   * @param shadowBlip the blip that is shadowing.
   * @param blipToShadow the blip being shadowed.
   */
  private void updateShadowBlip(Blip shadowBlip, Blip blipToShadow) {
    Preconditions.checkNotNull(shadowBlip, "Shadow blip can't be null");
    Preconditions.checkNotNull(blipToShadow, "Blip to shadow can't be null");
    shadowBlip.all().replace(blipToShadow.getContent());

    for (Annotation annotation : blipToShadow.getAnnotations()) {
      if (annotation.getName().equals(ECHOEY_ANNOTATION)) {
        continue;
      }
      shadowBlip.range(annotation.getRange().getStart(), annotation.getRange().getEnd()).annotate(
          annotation.getName(), annotation.getValue());
    }
    shadowBlip.all().annotate(ECHOEY_ANNOTATION, blipToShadow.getBlipId());

    shadowBlipMap.put(blipToShadow.getBlipId(), shadowBlip.getBlipId());
  }

  /**
   * Updates the shadow map for a given wavelet.
   */
  private void updateShadowMap(Wavelet wavelet) {
    for (Blip blip : wavelet.getBlips().values()) {
      if (isShadowBlip(blip)) {
        String shadowBlipId = blip.getBlipId();
        if (!shadowBlipMap.containsValue(shadowBlipId)) {
          String originalBlipId = blip.getAnnotations().get(ECHOEY_ANNOTATION).get(0).getValue();
          shadowBlipMap.put(originalBlipId, shadowBlipId);
        }
      }
    }
  }

  /**
   * Return true if the given blip is a blip in which Echoey has written.
   */
  private boolean isShadowBlip(Blip blip) {
    List<Annotation> annotations = blip.getAnnotations().get(ECHOEY_ANNOTATION);
    return annotations != null && !annotations.isEmpty();
  }
}
