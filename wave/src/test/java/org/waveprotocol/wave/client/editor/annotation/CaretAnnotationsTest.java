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

package org.waveprotocol.wave.client.editor.annotation;

import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations.AnnotationResolver;

import junit.framework.TestCase;

import java.util.HashSet;

/**
 * Small tests for the object which handles annotations to be
 * retrospectively applied to content inserted at the caret.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class CaretAnnotationsTest extends TestCase {
  /** String used for the annotations key, can be anything. */
  private final String TEST_KEY = "TEST_KEY!@&^#!";

  /** String used for the annotations value, can be anything. */
  private final String TEST_VALUE = "TEST_VALUE!@*#^&!@)#";

  /** String used for the value returned by the resolver, can be anything. */
  private final String RESOLVER_VALUE = "RESOLVER_KEY_!_!@)_#";

  /** Make sure the simple set/get work as required. */
  public void testBasic() {
    // initialise:
    CaretAnnotations annotations = new CaretAnnotations();

    // simple set-get
    annotations.setAnnotation(TEST_KEY, TEST_VALUE);
    assertTrue("Must have a set annotation.",
        annotations.hasAnnotation(TEST_KEY));
    assertTrue("Must be annotated with the key/value pair set.",
        annotations.isAnnotated(TEST_KEY, TEST_VALUE));
    assertEquals("Get annotation must match the set value.",
        annotations.getAnnotation(TEST_KEY), TEST_VALUE);
    assertTrue("Must have the set annotation in key set.",
        annotations.getAnnotationKeys().contains(TEST_KEY));

    // reset the key to null (not the same as remove)
    annotations.setAnnotation(TEST_KEY, null);
    assertTrue("Must have a set annotation.",
        annotations.hasAnnotation(TEST_KEY));
    assertFalse("Must no longer be annotated with the key/value pair set.",
        annotations.isAnnotated(TEST_KEY, TEST_VALUE));
    assertNull("Get annotation must match the set value.",
        annotations.getAnnotation(TEST_KEY));
    assertTrue("Must have the set annotation in key set.",
        annotations.getAnnotationKeys().contains(TEST_KEY));

    // remove the key
    annotations.removeAnnotation(TEST_KEY);
    assertFalse("Must not have a removed annotation.",
        annotations.hasAnnotation(TEST_KEY));
    assertNull("Get annotation for removed key should be null.",
        annotations.getAnnotation(TEST_KEY));
    assertFalse("Must not have removed annotation in key set.",
        annotations.getAnnotationKeys().contains(TEST_KEY));

    // test clearing, then setting multiple:
    annotations.setAnnotation(TEST_KEY, TEST_VALUE);
    assertEquals("Must have the right size key set.",
        annotations.getAnnotationKeys().size(), 1);
    annotations.clear();
    assertEquals("Must have empty key set after clear.",
        annotations.getAnnotationKeys().size(), 0);

    // test setting many:
    for (int i = 0; i < 10; i++) {
      annotations.setAnnotation("" + i, "" + i);
    }
    assertEquals("Must have the right number of annotations.",
        annotations.getAnnotationKeys().size(), 10);
    assertEquals("Must keep annotations when setting multiple.",
        annotations.getAnnotation("4"), "4"); // check random one in the middle.

  }

  /** Ensure the resolver is being delegated to when needed. */
  public void testResolver() {
    // initialise:
    CaretAnnotations annotations = new CaretAnnotations();
    MockResolver mockResolver = new MockResolver();
    annotations.setAnnotationResolver(mockResolver);

    // make sure a known annotation isn't resolved.
    annotations.setAnnotation(TEST_KEY, TEST_VALUE);
    annotations.getAnnotation(TEST_KEY);
    assertFalse("Known annotations shouldn't be resolved.",
        mockResolver.wasResolved(TEST_KEY));

    // check that unknown annotations were resolved properly:
    annotations.removeAnnotation(TEST_KEY);
    assertEquals("Unknown annotations should be fetched from resolver.",
        RESOLVER_VALUE, annotations.getAnnotation(TEST_KEY));
    assertTrue("Unknown annotations should be fetched from resolver.",
        mockResolver.wasResolved(TEST_KEY));

    // unset resolver, ensure unknown keys are no longer resolved:
    annotations.setAnnotationResolver(null);
    assertNull("Unknown annotations should be null when no resolver present.",
        annotations.getAnnotation(TEST_KEY));
  }

  /** Check that applying the annotations to a document is correct. */
  public void testApply() {
    // TODO(patcoleman): add code for this test,
    // requires encorporating mutable document mocks into this.
  }

  /** Mock resolver that checks whether it is called appropriately. */
  private class MockResolver implements AnnotationResolver {
    HashSet<String> calledSet = new HashSet<String>();

    @Override
    public String getAnnotation(String key) {
      calledSet.add(key);
      return RESOLVER_VALUE;
    }

    /** Utility that traces which annotations this has resolved. */
    public boolean wasResolved(String key) {
      return calledSet.contains(key);
    }
  };
}
