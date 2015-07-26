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

package org.waveprotocol.wave.model.adt.docbased;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */

public class DocumentBasedElementListTest extends TestCase {
  private static final String[] WORD = { "lah", "dee", "dah" };
  private static final String CHILD_TAG = "foo";
  private static final String ATTR_NAME = "bar";
  private static final int PREPOPULATED_SIZE = 3;

  /**
   * A concrete instance of the list that stores string values inside elements with tag foo.
   * A string value is stored in the attribute named bar. Basically we want something like this:
   * <pre>
   *   &lt;container&gt;
   *     &lt;foo bar="lah"/&gt;
   *     &lt;foo bar="dee"/&gt;
   *     ...
   *     &lt;foo bar="dah"/&gt;
   *   &lt;/container&gt;
   * </pre>
   */
  private static class ValueList<E> implements
      Factory<E, BasicValue<String>, String>,
      ObservableElementList<BasicValue<String>, String> {

    private final DocumentBasedElementList<E, BasicValue<String>, String> list;

    protected ValueList(ObservableMutableDocument<? super E, E, ?> document, E parent) {
      list = DocumentBasedElementList.create(DefaultDocumentEventRouter.create(document), parent,
          CHILD_TAG,
      this);
    }

    @Override
    public Initializer createInitializer(String initialValue) {
      return DocumentBasedBasicValue.createInitialiser(Serializer.STRING, ATTR_NAME, initialValue);
    }

    @Override
    public BasicValue<String> adapt(DocumentEventRouter<? super E, E, ?> router, E element) {
      return DocumentBasedBasicValue.create(router, element, Serializer.STRING, ATTR_NAME);
    }

    @Override
    public BasicValue<String> add(String initialState) {
      return list.add(initialState);
    }

    @Override
    public BasicValue<String> add(int index, String initialState) {
      return list.add(index, initialState);
    }

    @Override
    public BasicValue<String> get(int index) {
      return list.get(index);
    }

    @Override
    public int indexOf(BasicValue<String> child) {
      return list.indexOf(child);
    }

    @Override
    public Iterable<BasicValue<String>> getValues() {
      return list.getValues();
    }

    @Override
    public boolean remove(BasicValue<String> child) {
      return list.remove(child);
    }

    @Override
    public void clear() {
      list.clear();
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public void addListener(ObservableElementList.Listener<BasicValue<String>> listener) {
      list.addListener(listener);
    }

    @Override
    public void removeListener(ObservableElementList.Listener<BasicValue<String>> listener) {
      list.removeListener(listener);
    }
  }

  private static class ListChangeObserver
      implements ObservableElementList.Listener<BasicValue<String>> {
    private final List<String> history = new ArrayList<String>();
    private final String name;

    public ListChangeObserver(String name) {
      this.name = name;
    }

    @Override
    public void onValueAdded(BasicValue<String> entry) {
      history.add("a");
    }

    @Override
    public void onValueRemoved(BasicValue<String> entry) {
      history.add("r");
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder(name);
      for (String op : history) {
        builder.append(op);
      }
      return builder.toString();
    }
  }

  /** The setUp method creates this list with no child elements. */
  private ObservableElementList<BasicValue<String>, String> freshList;

  /** The setUp method creates this list with a few children. */
  private ObservableElementList<BasicValue<String>, String> prepopulatedList;

  @Override
  protected void setUp() throws Exception {
    buildListHelper(BasicFactories.observableDocumentProvider().create("data",
        Collections.<String, String>emptyMap()));
  }

  private <N> void buildListHelper(ObservableMutableDocument<N, ?, ?> document) {
    buildList(document);
  }

  private <E> void buildList(ObservableMutableDocument<? super E, E, ?> document) {
    /* initially blank list */
    {
      E blankParent = document.createChildElement(
          document.getDocumentElement(), "blank", Collections.<String, String>emptyMap());
      freshList = new ValueList<E>(document, blankParent);
    }
    /* list pre-populated with children and some other content */
    {
      E prepopulatedParent = document.createChildElement(
          document.getDocumentElement(), "preopopulated", Collections.<String, String>emptyMap());

      // We are adding a non-list element to test if it is correctly ignored.
      Map<String, String> attrs = new HashMap<String, String>();
      attrs.put(CHILD_TAG, "1");
      attrs.put("flubble", "huh");
      document.createChildElement(prepopulatedParent, "random", attrs);

      for (int i = 0; i < PREPOPULATED_SIZE; ++i) {
        attrs = new HashMap<String, String>();
        attrs.put(ATTR_NAME, Integer.toString(i));
        document.createChildElement(prepopulatedParent, CHILD_TAG, attrs);
      }

      prepopulatedList = new ValueList<E>(document, prepopulatedParent);
    }
  }

  public void testEmptyList() {
    assertEquals(0, freshList.size());
  }

  public void testAdd() {
    for (int index = 0; index < WORD.length; ++index) {
      BasicValue<String> value = freshList.add(null);
      value.set(WORD[index]);
      assertEquals("The list should have " + (index + 1) + " elements",
          index + 1, freshList.size());
      assertEquals("The " + (index + 1) + "th value should be " + WORD[index],
          WORD[index], freshList.get(index).get());
    }
  }

  public void testAddWithInitialState() {
    BasicValue<String> value = freshList.add("baz");
    assertEquals("baz", value.get());
  }

  public void testRemove() {
    assertEquals(0, freshList.size());
    BasicValue<String> value = freshList.add(null);
    assertEquals(1, freshList.size());
    assertTrue(freshList.remove(value));
    assertEquals(0, freshList.size());
    assertFalse(freshList.remove(value));
  }

  public void testClear() {
    assertEquals(0, freshList.size());
    freshList.clear();
    assertEquals(0, freshList.size());
    // TODO(user): Should we have isEmpty?
    assertFalse(prepopulatedList.size() == 0);
    prepopulatedList.clear();
    assertEquals(0, prepopulatedList.size());
    // TODO(user): Add test with parallel removals and additions to cleared list.
  }

  public void testCanIterateOverValues() {
    for (int index = 0; index < WORD.length; ++index) {
      freshList.add(WORD[index]);
    }
    StringBuilder terms = new StringBuilder();
    int index = 0;
    for (BasicValue<String> word : freshList.getValues()) {
      assertEquals(WORD[index++], word.get());
    }
  }

  public void testCanObserveChanges() {
    ListChangeObserver bob = new ListChangeObserver("bob");
    ListChangeObserver ann = new ListChangeObserver("ann");

    assertEquals("bob", bob.toString());
    assertEquals("ann", ann.toString());
    freshList.addListener(bob);
    BasicValue<String> one = freshList.add(null);
    assertEquals("boba", bob.toString());
    assertEquals("ann", ann.toString());
    freshList.addListener(ann);
    BasicValue<String> two = freshList.add(null);
    assertEquals("bobaa", bob.toString());
    assertEquals("anna", ann.toString());
    freshList.remove(two);
    assertEquals("bobaar", bob.toString());
    assertEquals("annar", ann.toString());
    freshList.removeListener(bob);
    BasicValue<String> three = freshList.add(null);
    assertEquals("bobaar", bob.toString());
    assertEquals("annara", ann.toString());
    freshList.removeListener(ann);
    BasicValue<String> four = freshList.add(null);
    assertEquals("bobaar", bob.toString());
    assertEquals("annara", ann.toString());
  }

  public void testPrePopulated() {
    assertEquals(PREPOPULATED_SIZE, prepopulatedList.size());
    for (int i = 0; i < PREPOPULATED_SIZE; ++i) {
      assertEquals(Integer.toString(i), prepopulatedList.get(i).get());
    }
  }

  public void testIndexOf() {
    for (int i = 0; i < PREPOPULATED_SIZE; ++i) {
      BasicValue<String> child = prepopulatedList.get(i);
      assertEquals(i, prepopulatedList.indexOf(child));
    }
    BasicValue<String> last = prepopulatedList.add(null);
    assertEquals(prepopulatedList.size() - 1, prepopulatedList.indexOf(last));
    assertEquals(-1, freshList.indexOf(last));
  }

  public void testAddAtInvalidIndex() {
    try {
      prepopulatedList.add(-1, null);
      fail("Adding at negative index should fail");
    } catch (IndexOutOfBoundsException e) {
      // success
    }
    try {
      prepopulatedList.add(prepopulatedList.size() + 1,null);
      fail("Adding after the end of the list should fail");
    } catch (IndexOutOfBoundsException e) {
      // success
    }
  }

  public void testAddAtIndex() {
    BasicValue<String> last = freshList.add(0, null);
    assertSame(last, freshList.get(0));

    BasicValue<String> oldSecond = prepopulatedList.get(1);
    BasicValue<String> newSecond = prepopulatedList.add(1, null);
    assertSame(newSecond, prepopulatedList.get(1));
    assertSame(oldSecond, prepopulatedList.get(2));

    BasicValue<String> oldFirst = prepopulatedList.get(0);
    BasicValue<String> newFirst = prepopulatedList.add(0, null);
    assertSame(newFirst, prepopulatedList.get(0));
    assertSame(oldFirst, prepopulatedList.get(1));
  }
}
