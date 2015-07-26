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

package org.waveprotocol.wave.model.document.util;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.util.ChainedData;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.DataDomain;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Iterator;

/**
 * StringMap based implementation
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public class AnnotationRegistryImpl implements AnnotationRegistry {

  private static class HandlerData {
    final StringMap<AnnotationMutationHandler> handlers = CollectionUtils.createStringMap();
    final StringMap<AnnotationBehaviour> behaviours = CollectionUtils.createStringMap();
  }

  private static final DataDomain<HandlerData, HandlerData> handlerDataDomain =
      new DataDomain<HandlerData, HandlerData>() {
        @Override
        public void compose(HandlerData target, HandlerData changes, HandlerData base) {
          target.handlers.clear();
          copyInto(target, base);
          copyInto(target, changes);
        }

        private void copyInto(final HandlerData target, HandlerData source) {
          target.handlers.putAll(source.handlers);
          target.behaviours.putAll(source.behaviours);
        }

        @Override
        public HandlerData empty() {
          return new HandlerData();
        }

        @Override
        public HandlerData readOnlyView(HandlerData modifiable) {
          return modifiable;
        }
      };

  public static final AnnotationRegistryImpl ROOT = new AnnotationRegistryImpl();

  private final ChainedData<HandlerData, HandlerData> data;

  @VisibleForTesting
  public AnnotationRegistryImpl() {
    data = new ChainedData<HandlerData, HandlerData>(handlerDataDomain);
  }

  @VisibleForTesting
  private AnnotationRegistryImpl(AnnotationRegistryImpl parent) {
    data = new ChainedData<HandlerData, HandlerData>(parent.data);
  }

  /**
   * Create a copy of this registry for further extension.
   *
   * TODO(user): Consider more efficient propagation of changes to children.
   */
  @Override
  public AnnotationRegistryImpl createExtension() {
    AnnotationRegistryImpl child = new AnnotationRegistryImpl(this);
    return child;
  }

  @Override
  public void registerHandler(String prefix, AnnotationMutationHandler handler) {
    Util.validatePrefix(prefix);
    data.modify().handlers.put(prefix, handler);
  }

  @Override
  public void registerBehaviour(String prefix, AnnotationBehaviour behaviour) {
    Util.validatePrefix(prefix);
    data.modify().behaviours.put(prefix, behaviour);
  }

  @Override
  public Iterator<AnnotationMutationHandler> getHandlers(final String prefix) {
    final String parts = prefix + "/";

    final StringMap<AnnotationMutationHandler> handlers = data.inspect().handlers;
    return new Iterator<AnnotationMutationHandler>() {
      int fromIndex = -1;
      AnnotationMutationHandler next;

      {
        getNext();
      }

      @Override
      public boolean hasNext() {
        return next != null;
      }

      @Override
      public AnnotationMutationHandler next() {
        AnnotationMutationHandler ret = next;
        getNext();
        return ret;
      }

      private void getNext() {
        while ((fromIndex = parts.indexOf('/', fromIndex + 1)) != -1) {
          AnnotationMutationHandler handler = handlers.get(parts.substring(0, fromIndex));
          if (handler != null) {
            next = handler;
            return;
          }
        }
        next = null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("getHandlers iterator: remove");
      }
    };
  }

  @Override
  public Iterator<AnnotationBehaviour> getBehaviours(final String prefix) {
    final String parts = prefix + "/";

    final StringMap<AnnotationBehaviour> behaviours = data.inspect().behaviours;
    return new Iterator<AnnotationBehaviour>() {
      int fromIndex = -1;
      AnnotationBehaviour next;

      {
        getNext();
      }

      @Override
      public boolean hasNext() {
        return next != null;
      }

      @Override
      public AnnotationBehaviour next() {
        AnnotationBehaviour ret = next;
        getNext();
        return ret;
      }

      private void getNext() {
        while ((fromIndex = parts.indexOf('/', fromIndex + 1)) != -1) {
          AnnotationBehaviour behaviour = behaviours.get(parts.substring(0, fromIndex));
          if (behaviour != null) {
            next = behaviour;
            return;
          }
        }
        next = null;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("getBehaviours iterator: remove");
      }
    };
  }

  @Override
  public AnnotationBehaviour getClosestBehaviour(String key) {
    // TODO(patcoleman): optimise?
    AnnotationBehaviour closest = null;
    Iterator<AnnotationBehaviour> behaviours = getBehaviours(key);
    while (behaviours.hasNext()) {
      closest = behaviours.next();
    }
    return closest;
  }
}
