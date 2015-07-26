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

import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument.Method;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Role assignments on a wave serialized to a data document named <code>roles
 * </code> in a wavelet in the following basic format:
 *
 * <pre>
 * <assign address="public@a.gwave.com" role="WRITE" />
 * <assign address="other.user@googlewave.com" role="WRITE" />
 * </pre>
 *
 * Uses {@link Role} to describe what roles can be assigned.
 *
 *  There is one permissions document per wavelet containing multiple addresses.
 * A best effort attempt is made to only mention each address once but in case
 * of concurrency there can be multiple assignments, in that case only the last
 * is used.
 */
public class DocumentBasedRoles implements ObservableRoles {
  public static final String ASSIGN_TAG = "assign";

  private final ObservableElementList<
      ObservableAssignment, DocumentBasedAssignment.AssignmentInitializer> assignments;
  private final CopyOnWriteSet<ObservableRoles.Listener> listeners = CopyOnWriteSet.create();
  private final ObservableElementList.Listener<ObservableAssignment> assignmentListListener;
  private final ObservableAssignment.Listener assignmentListener;

  private DocumentBasedRoles(ObservableElementList<
      ObservableAssignment, DocumentBasedAssignment.AssignmentInitializer> assignments) {
    this.assignments = assignments;

    this.assignmentListener = new ObservableAssignment.Listener() {
      @Override
      public void onChanged() {
        fireOnChanged();
      }
    };

    for (ObservableAssignment a : assignments.getValues()) {
      a.addListener(assignmentListener);
    }

    this.assignmentListListener = new ObservableElementList.Listener<ObservableAssignment>() {
      @Override
      public void onValueAdded(ObservableAssignment entry) {
        entry.addListener(assignmentListener);
        fireOnChanged();
      }

      @Override
      public void onValueRemoved(ObservableAssignment entry) {
        entry.removeListener(assignmentListener);
        fireOnChanged();
      }
    };
    assignments.addListener(assignmentListListener);
  }

  private void fireOnChanged() {
    for (ObservableRoles.Listener l : listeners) {
      l.onChanged();
    }
  }

  /**
   * Creates a Permissions view on top of the document.
   */
  public static DocumentBasedRoles create(final ObservableMutableDocument<?, ?, ?> document) {
    return document.with(new Method<DocumentBasedRoles>() {
      @Override
      public <N, E extends N, T extends N> DocumentBasedRoles exec(
          ObservableMutableDocument<N, E, T> doc) {
        return new DocumentBasedRoles(
            DocumentBasedElementList.create(DefaultDocumentEventRouter.create(doc),
                doc.getDocumentElement(), ASSIGN_TAG, DocumentBasedAssignment.<E>factory()));
      }
    });
  }

  @Override
  public boolean isPermitted(ParticipantId participant, Capability capability) {
    return getRole(participant).isPermitted(capability);
  }

  private Assignment getAssignment(ParticipantId participant) {
    Assignment result = null;
    // TODO(user): Make this faster by denormalizing into an index.
    for (Assignment assignment : assignments.getValues()) {
      if (assignment.getParticipant().equals(participant)) {
        result = assignment;
        // Continue through the list even though a role was found.
        // We take the last role in the case of multiple.
      }
    }
    return result;
  }

  @Override
  public Role getRole(ParticipantId participant) {
    Assignment assignment = getAssignment(participant);
    if (assignment != null) {
      Role role = assignment.getRole();
      if (role != null) {
        return role;
      }
    }
    return Policies.DEFAULT_ROLE;
  }

  @Override
  public void assign(ParticipantId participant, Role role) {
    Role roleToSet = role.equals(Policies.DEFAULT_ROLE) ? null : role;
    ObservableAssignment assignment = null;
    for (ObservableAssignment candidate : assignments.getValues()) {
      if (candidate.getParticipant().equals(participant)) {
        assignment = candidate;
      }
    }
    if (assignment != null) {
      assignment.setRole(roleToSet);
    } else if (roleToSet != null) {
      assignments.add(new DocumentBasedAssignment.AssignmentInitializer(
          new BasicAssignment(participant, roleToSet)));
    }
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);

  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public Iterable<? extends Assignment> getAssignments() {
    return assignments.getValues();
  }
}
