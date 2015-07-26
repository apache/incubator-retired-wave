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

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Tests shared between implementations of MutableIndexability.
 *
 *
 */
public abstract class MutableIndexabilityTestBase extends TestCase {

  protected abstract MutableIndexability getIndexability();

  public void testReadingUnindexable() {
    MutableIndexability indexability = getIndexability();
    indexability.setIndexability(p("public@a.gwave.com"), IndexDecision.NO);
    assertEquals(IndexDecision.NO, indexability.getIndexability(p("public@a.gwave.com")));
  }

  public void testReadingIndexable() {
    MutableIndexability indexability = getIndexability();
    indexability.setIndexability(p("public@a.gwave.com"), IndexDecision.YES);
    assertEquals(IndexDecision.YES, indexability.getIndexability(p("public@a.gwave.com")));
  }

  public void testDefaultsToNull() {
    MutableIndexability indexability = getIndexability();
    assertEquals(null, indexability.getIndexability(p("public@a.gwave.com")));
  }

  public void testSettable() {
    MutableIndexability indexability = getIndexability();
    indexability.setIndexability(p("public@a.gwave.com"), IndexDecision.NO);
    assertEquals(IndexDecision.NO, indexability.getIndexability(p("public@a.gwave.com")));
  }

  public void testSettingNullClearsAssignment() {
    MutableIndexability indexability = getIndexability();
    indexability.setIndexability(p("public@a.gwave.com"), IndexDecision.NO);
    assertEquals(IndexDecision.NO, indexability.getIndexability(p("public@a.gwave.com")));
    indexability.setIndexability(p("public@a.gwave.com"), null);
    assertEquals(null, indexability.getIndexability(p("public@a.gwave.com")));
  }

  public void testGetIndexDecisions() {
    MutableIndexability indexability = getIndexability();
    indexability.setIndexability(p("public@a.gwave.com"), IndexDecision.NO);
    indexability.setIndexability(p("joe@example.com"), IndexDecision.YES);
    indexability.setIndexability(p("null@example.com"), null);
    assertEquals(CollectionUtils.immutableSet(p("public@a.gwave.com"), p("joe@example.com")),
        indexability.getIndexDecisions());
  }

  protected ParticipantId p(String address) {
    return new ParticipantId(address);
  }
}
