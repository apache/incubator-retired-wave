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


import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocCompare;
import org.waveprotocol.wave.model.schema.account.AccountSchemas;
import org.waveprotocol.wave.model.testing.BasicFactories;

/**
 *
 */

public class DocumentBasedIndexabilityTest extends MutableIndexabilityTestBase {
  DocumentBasedIndexability indexability;
  ObservableDocument doc;

  @Override
  protected MutableIndexability getIndexability() {
    ObservableDocument doc =
        BasicFactories.createDocument(AccountSchemas.INDEXABILITY_SCHEMA_CONSTRAINTS);
    DefaultDocEventRouter router = DefaultDocEventRouter.create(doc);
    return DocumentBasedIndexability.create(router);
  }

  @Override
  public void setUp() {
    initFromDoc("");
  }

  private void initFromDoc(final String content) {
    ObservableDocument doc =
        BasicFactories.createDocument(AccountSchemas.INDEXABILITY_SCHEMA_CONSTRAINTS, content);
    DefaultDocEventRouter router = DefaultDocEventRouter.create(doc);
    indexability = DocumentBasedIndexability.create(router);
  }

  public void testReadingUnindexableFromDoc() {
    initFromDoc("<index address=\"public@a.gwave.com\" i=\"NO\"/>");
    assertEquals(IndexDecision.NO, indexability.getIndexability(p("public@a.gwave.com")));
  }

  public void testReadingIndexableFromDoc() {
    initFromDoc("<index address=\"public@a.gwave.com\" i=\"YES\"/>");
    assertEquals(IndexDecision.YES, indexability.getIndexability(p("public@a.gwave.com")));
  }

//  public void testPersistsValuesInDocument() {
//    indexability.setIndexability(p("public@a.gwave.com"), IndexDecision.NO);
//    assertTrue(DocCompare.equivalent(
//        DocCompare.ALL, "<index address=\"public@a.gwave.com\" i=\"NO\"/>", doc));
//  }
//
//  public void testPersistsDefaultsWhenNeeded() {
//    indexability.setIndexability(p("joe@example.com"), IndexDecision.NO);
//    indexability.setIndexability(p("joe@example.com"), IndexDecision.YES);
//    assertTrue(DocCompare.equivalent(
//        DocCompare.ALL, "<index address=\"joe@example.com\" i=\"YES\"/>", doc));
//  }
}
