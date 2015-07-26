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

package org.waveprotocol.wave.model.account;

import org.waveprotocol.wave.model.adt.ObservableStructuredValue;
import org.waveprotocol.wave.model.adt.docbased.CompoundInitializer;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedStructuredValue;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Map;

/**
 * A single assignment of a role to an address, backed by a document. The role
 * and address are both stored as attributes on a single element.
 *
 */
class DocumentBasedAssignment implements ObservableAssignment, ObservableStructuredValue.Listener<
    DocumentBasedAssignment.Key, String> {
  /*
   * Used by DocumentBasedStructuredValue to store the two attributes.
   */
  public enum Key {
    ADDRESS {
      @Override
      public String toString() {
        return "address";
      }
    },
    ROLE {
      @Override
      public String toString() {
        return "role";
      }
    },
  }

  static class AssignmentInitializer {
    private final String address;
    private final Role role;

    public AssignmentInitializer(Assignment assignment) {
      this.address = assignment.getParticipant().getAddress();
      this.role = assignment.getRole();
    }
  }


  public static <E> DocumentBasedAssignment create(
      DocumentEventRouter<? super E, E, ?> router, E container) {
    DocumentBasedAssignment assignment = new DocumentBasedAssignment(
        DocumentBasedStructuredValue.create(router, container, Serializer.STRING, Key.class));
    assignment.value.addListener(assignment);
    return assignment;
  }

  static <E> Factory<E, ObservableAssignment, AssignmentInitializer> factory() {
    return new Factory<E, ObservableAssignment, AssignmentInitializer>() {
      @Override
      public ObservableAssignment adapt(DocumentEventRouter<? super E, E, ?> router, E element) {
        return DocumentBasedAssignment.create(router, element);
      }

      @Override
      public Initializer createInitializer(AssignmentInitializer initialState) {
        return new CompoundInitializer(
            DocumentBasedStructuredValue.createInitialiser(Serializer.STRING,
                CollectionUtils.immutableMap(Key.ADDRESS, initialState.address, Key.ROLE,
                    RoleSerializer.INSTANCE.toString(initialState.role))));
      }
    };
  }

  private final ObservableStructuredValue<Key, String> value;
  private final CopyOnWriteSet<ObservableAssignment.Listener> listeners = CopyOnWriteSet.create();

  public DocumentBasedAssignment(ObservableStructuredValue<Key, String> value) {
    this.value = value;
  }

  @Override
  public void addListener(ObservableAssignment.Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ObservableAssignment.Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public void setRole(Role role) {
    value.set(Key.ROLE, RoleSerializer.INSTANCE.toString(role));
  }

  @Override
  public Role getRole() {
    return RoleSerializer.INSTANCE.fromString(value.get(Key.ROLE));
  }

  @Override
  public void onValuesChanged(
      Map<Key, ? extends String> oldValues, Map<Key, ? extends String> newValues) {
    for (ObservableAssignment.Listener l : listeners) {
      l.onChanged();
    }
  }

  @Override
  public void onDeleted() {
    // Do nothing.
    // The element list will report this event anyway.
  }

  @Override
  public ParticipantId getParticipant() {
    return ParticipantId.ofUnsafe(value.get(Key.ADDRESS));
  }

}
