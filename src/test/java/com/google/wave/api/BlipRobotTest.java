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

package com.google.wave.api;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.wave.api.Function.BlipContentFunction;
import com.google.wave.api.Function.MapFunction;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.impl.DocumentModifyAction;
import com.google.wave.api.impl.DocumentModifyAction.BundledAnnotation;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Test cases for {@link Blip}.
 */
public class BlipRobotTest extends TestCase {

  private static final String ANNOTATION_KEY = "style/fontWeight";
  private static final String ROOT_BLIP_ID = "b+43";
  private static final String CHILD_BLIP_ID = "b+44";

  private final Map<String, Blip> blips = new HashMap<String, Blip>();
  private Wavelet wavelet;

  @Override
  protected void setUp() throws Exception {
    wavelet = mock(Wavelet.class);
    when(wavelet.getBlips()).thenReturn(blips);
    when(wavelet.getOperationQueue()).thenReturn(new OperationQueue());
    when(wavelet.getWaveId()).thenReturn(WaveId.of("example.com", "wave1"));
    when(wavelet.getWaveletId()).thenReturn(WaveletId.of("example.com", "wavelet1"));
    when(wavelet.getThread(anyString())).thenReturn(new BlipThread("rootThread", -1,
        Lists.newArrayList(ROOT_BLIP_ID, CHILD_BLIP_ID), blips));
  }

  private Blip newBlip(String content, List<Annotation> annotations) {
    return newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null, annotations, content);
  }

  private Blip newBlip(String blipId, List<String> childBlipIds, String parentBlipId) {
    return newBlip(blipId, childBlipIds, parentBlipId,
        Arrays.asList(new Annotation("key", "val", 2, 3)));
  }

  private Blip newBlip(String blipId, List<String> childBlipIds, String parentBlipId,
      List<Annotation> annotations) {
    return newBlip(blipId, childBlipIds, parentBlipId, annotations,
        "\nhello world!\n another line");
  }

  private Blip newBlip(String blipId, List<String> childBlipIds, String parentBlipId,
      List<Annotation> annotations, String content) {
    SortedMap<Integer, Element> elements = new TreeMap<Integer, Element>();
    elements.put(14, new Gadget("http://a/b.xml"));

    Blip blip = new Blip(blipId, childBlipIds, content,
        Arrays.asList("robot@test.com", "user@test.com"), "user@test.com", 1000l, 123l,
        parentBlipId, null, annotations, elements, new ArrayList<String>(), wavelet);
    blips.put(blipId, blip);
    return blip;
  }

  public void testDocumentOperations() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);

    List<BlipContent> newLines = blip.all("\n").values();
    assertEquals(2, newLines.size());
    assertEquals(Arrays.asList(Plaintext.of("\n"), Plaintext.of("\n")), newLines);

    blip.first("world").replace("jupiter");
    String[] bits = blip.getContent().split("\n");
    assertEquals(3, bits.length);
    assertEquals("hello jupiter!", bits[1]);

    blip.range(2, 5).delete();
    assertTrue(blip.getContent().startsWith("\nho jupiter!"));

    blip.first("ho").insertAfter("la");
    assertTrue(blip.getContent().startsWith("\nhola jupiter!"));

    blip.at(3).insert(" ");
    assertTrue(blip.getContent().startsWith("\nho la jupiter!"));

    blip.all().delete();
    blip.at(1).insert("world!");

    blip.first("world").insert(new BlipContentFunction() {
      @Override
      public BlipContent call(BlipContent source) {
        return Plaintext.of("Hello " + source.getText().length() + " ");
      }
    });
    assertEquals("\nHello 5 world!", blip.getContent());
  }

  public void testElementHandling() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    int originalLength = blip.getContent().length();
    String url = "http://www.test.com/image.png";

    blip.append(new Image(url, 100, 100, "Cool pix."));
    assertEquals(2, blip.getElements().size());

    List<BlipContent> result = blip.all(ElementType.IMAGE, Image.restrictByUrl(url)).values();
    assertEquals(1, result.size());

    Element element = result.get(0).asElement();
    assertTrue(element instanceof Image);

    blip.at(1).insert("twelve chars");
    assertTrue(blip.getContent().startsWith("\ntwelve charshello"));
    element = blip.at(originalLength + 12).value().asElement();
    assertTrue(element instanceof Image);

    blip.first("twelve ").delete();
    assertTrue(blip.getContent().startsWith("\nchars"));
    element = blip.at(originalLength + 12 - "twelve ".length()).value().asElement();
    assertTrue(element instanceof Image);

    blip.first("chars").replace(new Image(url, 200, 200, "Yet another cool pix."));
    assertEquals(3, blip.getElements().size());
    assertTrue(blip.getContent().startsWith("\n hello"));
    element = blip.at(1).value().asElement();
    assertTrue(element instanceof Image);
    assertEquals("Yet another cool pix.", ((Image) element).getCaption());

    blip.all().delete();
    blip.append(new Image(url, 100, 100, "Cool pix."));
    blip.append(" some piece of text.");
    assertEquals("\n  some piece of text.", blip.getContent());
    assertEquals(url, ((Image) blip.first(ElementType.IMAGE).value()).getUrl());
    blip.first(ElementType.IMAGE).insertAfter(new BlipContentFunction() {
      @Override
      public BlipContent call(BlipContent source) {
        Image matchedImage = (Image) source;
        return Plaintext.of(matchedImage.getUrl());
      }
    });
    assertEquals("\n " + url + " some piece of text.", blip.getContent());

    blip.all().delete();
    blip.append(new Image(url, 100, 100, "Cool pix."));
    blip.append(" some piece of text.");
    assertEquals("\n  some piece of text.", blip.getContent());
    assertEquals(url, ((Image) blip.first(ElementType.IMAGE).value()).getUrl());
    blip.first(ElementType.IMAGE).replace(new BlipContentFunction() {
      @Override
      public BlipContent call(BlipContent source) {
        Image matchedImage = (Image) source;
        return new Image(matchedImage.getUrl() + "?query=foo", matchedImage.getWidth(),
            matchedImage.getHeight(), matchedImage.getCaption());
      }
    });
    assertEquals(url + "?query=foo", ((Image) blip.first(ElementType.IMAGE).value()).getUrl());
  }

  public void testUpdateElement() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    String url = "http://www.test.com/image.png";
    blip.append(new Image(url, 100, 100, "Cool pix."));
    assertEquals(2, blip.getElements().size());

    // Update the image by appending a query param to the URL.
    blip.first(ElementType.IMAGE).updateElement(new MapFunction() {
      @Override
      public Map<String, String> call(BlipContent source) {
        Image matchedImage = (Image) source;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("url", matchedImage.getUrl() + "?version=newversion");
        return properties;
      }
    });

    Image image = (Image) blip.first(ElementType.IMAGE).value();
    assertEquals(url + "?version=newversion", image.getUrl());
  }

  public void testAnnotationHandling() throws Exception {
    String key = Annotation.FONT_WEIGHT;

    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null,
        Arrays.asList(new Annotation(key, "bold", 3, 6)));
    assertEquals(1, blip.getAnnotations().size());
    assertEquals(new Annotation(key, "bold", 3, 6), blip.getAnnotations().get(key).get(0));

    // Extend the bold annotation.
    blip.range(5, 8).annotate(key, "bold");
    assertEquals(1, blip.getAnnotations().size());
    assertEquals(new Annotation(key, "bold", 3, 8), blip.getAnnotations().get(key).get(0));

    // Clip by adding another annotation with the same key.
    blip.range(4, 12).annotate(key, "italic");
    assertEquals(1, blip.getAnnotations().size());
    assertEquals(2, blip.getAnnotations().get(key).size());
    assertEquals(new Annotation(key, "bold", 3, 4), blip.getAnnotations().get(key).get(0));
    assertEquals(new Annotation(key, "italic", 4, 12), blip.getAnnotations().get(key).get(1));

    // Split the italic annotation.
    blip.range(6, 7).clearAnnotation(key);
    assertEquals(3, blip.getAnnotations().get(key).size());
    assertEquals(new Annotation(key, "bold", 3, 4), blip.getAnnotations().get(key).get(0));
    assertEquals(new Annotation(key, "italic", 4, 6), blip.getAnnotations().get(key).get(1));
    assertEquals(new Annotation(key, "italic", 7, 12), blip.getAnnotations().get(key).get(2));

    // Test names and iteration.
    assertEquals(1, blip.getAnnotations().namesSet().size());

    Annotation[] expected = {
        new Annotation(key, "bold", 3, 4),
        new Annotation(key, "italic", 4, 6),
        new Annotation(key, "italic", 7, 12)
    };
    int index = 0;
    for (Annotation annotation : blip.getAnnotations()) {
      assertEquals(expected[index++], annotation);
    }
    assertEquals(3, index);

    blip.range(3, 5).annotate("foo", "bar");
    assertEquals(2, blip.getAnnotations().namesSet().size());
    assertEquals(3, blip.getAnnotations().get(key).size());
    assertEquals(1, blip.getAnnotations().get("foo").size());
    blip.range(3, 5).clearAnnotation("foo");

    // Clear the whole thing.
    blip.all().clearAnnotation(key);
    assertNull(blip.getAnnotations().get(key));
  }

  public void testBlipOperations() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    assertEquals(1, wavelet.getBlips().size());

    Blip other = blip.reply();
    other.append("hello world");
    assertEquals("\nhello world", other.getContent());
    assertEquals(2, wavelet.getBlips().size());

    Blip inline = blip.insertInlineBlip(3);
    assertEquals("\n", inline.getContent());
    assertEquals(3, wavelet.getBlips().size());
  }

  public void testInsertInlineBlipCantInsertAtTheBeginning() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    assertEquals(1, wavelet.getBlips().size());

    try {
      blip.insertInlineBlip(0);
      fail("Should have thrown an exception when trying to insert inline blip at index 0.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertEquals(1, wavelet.getBlips().size());
  }

  public void testDocumentModify() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    blip.all().replace("a text with text and then some text");
    assertEquals("\na text with text and then some text", blip.getContent());
    blip.at(8).insert("text ");
    blip.all("text").replace("foo", "bar");
    assertEquals("\na foo bar with foo and then some bar", blip.getContent());
  }

  public void testBundledAnnotations() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    blip.all().insert(BundledAnnotation.listOf("style", "bold"), "\nhello");
    assertEquals(2, blip.getAnnotations().size());
    assertEquals(new Annotation("style", "bold", 0, 6), blip.getAnnotations().get("style").get(0));
  }

  public void testProxyFor() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    Blip proxiedBlip = blip.proxyFor("proxyuser");

    assertEquals(blip.getBlipId(), proxiedBlip.getBlipId());
    assertEquals(blip.getWaveId(), proxiedBlip.getWaveId());
    assertEquals(blip.getWaveletId(), proxiedBlip.getWaveletId());

    proxiedBlip.reply();
    List<OperationRequest> pendingOps = proxiedBlip.getOperationQueue().getPendingOperations();
    assertEquals(1, pendingOps.size());
    assertEquals("proxyuser", pendingOps.get(0).getParameter(ParamsProperty.PROXYING_FOR));
  }

  public void testProxyForShouldFailIfProxyIdIsInvalid() throws Exception {
    try {
      newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null).proxyFor("foo@bar.com");
      fail("Should have failed since proxy id is not encoded.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  @SuppressWarnings("unchecked")
  public void testDocumentModifyParametersForUpdateElement() {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);

    Map<String, String> newProperties = new HashMap<String, String>();
    newProperties.put("url", "http://www.google.com/gadget.xml");
    blip.first(ElementType.GADGET).updateElement(newProperties);

    List<OperationRequest> ops = blip.getOperationQueue().getPendingOperations();
    DocumentModifyAction action = (DocumentModifyAction) ops.get(0).getParameter(
        ParamsProperty.MODIFY_ACTION);
    assertEquals("http://www.google.com/gadget.xml", action.getElement(0).getProperty("url"));
  }

  public void testDocumentModifyParametersForAnnotate() {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    blip.all().replace("foo foo foo");

    blip.all("foo").annotate("key", "value1", "value2", "value3");

    List<OperationRequest> ops = blip.getOperationQueue().getPendingOperations();
    DocumentModifyAction action = (DocumentModifyAction) ops.get(ops.size() - 1).getParameter(
        ParamsProperty.MODIFY_ACTION);
    assertEquals("key", action.getAnnotationKey());
    assertEquals(Arrays.asList("value1", "value2", "value3"), action.getValues());
  }

  public void testDocumentModifyParametersForClearAnnotation() {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    blip.all().clearAnnotation("key");

    List<OperationRequest> ops = blip.getOperationQueue().getPendingOperations();
    DocumentModifyAction action = (DocumentModifyAction) ops.get(ops.size() - 1).getParameter(
        ParamsProperty.MODIFY_ACTION);
    assertEquals("key", action.getAnnotationKey());
  }

  public void testDocumentModifyParametersForInsertInsertAfterAndReplace() throws Exception {
    Blip blip = newBlip(ROOT_BLIP_ID, Arrays.asList(CHILD_BLIP_ID), null);
    blip.at(0).insert(new Image("http://a/b.gif", 100, 100, "Foo"), Plaintext.of("bold"),
        Plaintext.of("text"));

    List<OperationRequest> ops = blip.getOperationQueue().getPendingOperations();
    DocumentModifyAction action = (DocumentModifyAction) ops.get(ops.size() - 1).getParameter(
        ParamsProperty.MODIFY_ACTION);
    assertEquals(3, action.getValues().size());
    assertFalse(action.hasTextAt(0));
    assertNull(action.getValues().get(0));
    assertTrue(action.hasTextAt(1));
    assertEquals("bold", action.getValues().get(1));
    assertTrue(action.hasTextAt(2));
    assertEquals("text", action.getValues().get(2));

    assertEquals(3, action.getElements().size());
    Element el = action.getElements().get(0);
    assertTrue(el instanceof Image);
    assertEquals("http://a/b.gif", ((Image) el).getUrl());
    assertNull(action.getElements().get(1));
    assertNull(action.getElements().get(2));
  }

  public void testDeleteRangeThatSpansAcrossAnnotationEndPoint() throws Exception {
    Blip blip = newBlip("\nFoo bar.", Arrays.asList(new Annotation(ANNOTATION_KEY, "bold", 1, 3)));
    // Delete "oo"
    blip.range(2, 4).delete();
    assertEquals("\nF bar.", blip.getContent());
    assertEquals(new Range(1, 2), blip.getAnnotations().get(ANNOTATION_KEY).get(0).getRange());
  }

  public void testInsertBeforeAnnotationStartPoint() {
    Blip blip = newBlip("\nFoo bar.", Arrays.asList(new Annotation(ANNOTATION_KEY, "bold", 4, 9)));
    blip.at(4).insert("d and");
    assertEquals(new Range(9, 14), blip.getAnnotations().get(ANNOTATION_KEY).get(0).getRange());
  }

  public void testDeleteRangeInsideAnnotation() {
    Blip blip = newBlip("\nFoo bar.", Arrays.asList(new Annotation(ANNOTATION_KEY, "bold", 1, 5)));
    // Delete "oo"
    blip.range(2, 4).delete();
    assertEquals("\nF bar.", blip.getContent());
    assertEquals(new Range(1, 3),  blip.getAnnotations().get(ANNOTATION_KEY).get(0).getRange());
  }

  public void testReplaceInsideAnnotation() {
    Blip blip = newBlip("\nFoo bar.", Arrays.asList(new Annotation(ANNOTATION_KEY, "bold", 1, 5)));
    // Replace "oo" with "ooo".
    blip.range(2, 4).replace("ooo");
    assertEquals("\nFooo bar.", blip.getContent());
    assertEquals(new Range(1, 6),  blip.getAnnotations().get(ANNOTATION_KEY).get(0).getRange());

    // Replace "ooo" with "o".
    blip.range(2, 5).replace("o");
    assertEquals("\nFo bar.", blip.getContent());
    assertEquals(new Range(1, 4),  blip.getAnnotations().get(ANNOTATION_KEY).get(0).getRange());
  }

  public void testReplaceSpanAnnotation() {
    Blip blip = newBlip("\nFoo bar.", Arrays.asList(new Annotation(ANNOTATION_KEY, "bold", 1, 4)));
    // Replace "oo bar." with "".
    blip.range(2, 9).replace("");
    assertEquals("\nF", blip.getContent());
    assertEquals(new Range(1, 2),  blip.getAnnotations().get(ANNOTATION_KEY).get(0).getRange());
  }

  public void testDeleteChildBlipId() {
    Blip blip = newBlip("\nFoo bar.", Collections.<Annotation>emptyList());
    assertEquals(1, blip.getChildBlipIds().size());
    blip.deleteChildBlipId(CHILD_BLIP_ID);
    assertEquals(0, blip.getChildBlipIds().size());
  }

  public void testSearchWithNoMatchShouldNotGenerateOperation() {
    Blip blip = newBlip("\nFoo bar.", Collections.<Annotation>emptyList());
    blip.all(":(").replace(":)");
    assertEquals(0, wavelet.getOperationQueue().getPendingOperations().size());
  }

  public void testDeleteAll() {
    Blip blip = newBlip("\nNew title\nNew content", Arrays.asList(
        new Annotation("style/fontWeight", "bold", new Range(10,11)),
        new Annotation("conv/title", "", new Range(0,10))));
    blip.all().delete();
    assertEquals("\n", blip.getContent());
  }

  public void testSerializeAndDeserialize() throws Exception {
    SortedMap<Integer, Element> elements = new TreeMap<Integer, Element>();
    elements.put(14, new Gadget("http://a/b.xml"));

    Blip expectedBlip = new Blip("blip1", Arrays.asList("blip2", "blip3"),
        "\nhello world!\n another line", Arrays.asList("robot@test.com", "user@test.com"),
        "user@test.com", 1000l, 123l, null, null, Arrays.asList(new Annotation("key", "val", 2, 3)),
        elements, new ArrayList<String>(), wavelet);

    Blip actualBlip = Blip.deserialize(wavelet.getOperationQueue(), wavelet,
        expectedBlip.serialize());

    assertEquals(expectedBlip.getWaveId(), actualBlip.getWaveId());
    assertEquals(expectedBlip.getWaveletId(), actualBlip.getWaveletId());
    assertEquals(expectedBlip.getBlipId(), actualBlip.getBlipId());
    assertEquals(expectedBlip.getContent(), actualBlip.getContent());
    assertEquals(expectedBlip.getCreator(), actualBlip.getCreator());
    assertEquals(expectedBlip.getLastModifiedTime(), actualBlip.getLastModifiedTime());
    assertEquals(expectedBlip.getParentBlipId(), actualBlip.getParentBlipId());
    assertEquals(expectedBlip.getVersion(), actualBlip.getVersion());
    assertEquals(expectedBlip.getContributors(), actualBlip.getContributors());
    assertEquals(expectedBlip.getChildBlipIds(), actualBlip.getChildBlipIds());
    assertEquals(expectedBlip.getElements().keySet(), actualBlip.getElements().keySet());
    assertEquals(expectedBlip.getAnnotations().size(), actualBlip.getAnnotations().size());
  }

  public void testAppendMarkup() throws Exception {
    Blip blip = newBlip("\nFoo", Collections.<Annotation>emptyList());
    blip.appendMarkup("foo");
    assertEquals(1, wavelet.getOperationQueue().getPendingOperations().size());
    assertEquals("\nFoofoo", blip.getContent());

    blip.appendMarkup("foo <b>bar</b>");
    assertEquals(2, wavelet.getOperationQueue().getPendingOperations().size());
    assertEquals("\nFoofoofoo bar", blip.getContent());

    blip.appendMarkup("foo<br>bar");
    assertEquals(3, wavelet.getOperationQueue().getPendingOperations().size());
    assertEquals("\nFoofoofoo barfoo\nbar", blip.getContent());

    blip.appendMarkup("foo<p indent=\"3\">bar</p>");
    assertEquals(4, wavelet.getOperationQueue().getPendingOperations().size());
    assertEquals("\nFoofoofoo barfoo\nbarfoo\nbar", blip.getContent());
  }

  public void testIteration() throws Exception {
    Blip blip = newBlip("\naaa 012 aaa 345 aaa 322", Collections.<Annotation>emptyList());

    Range[] expectedRanges = {new Range(1, 4), new Range(9, 12), new Range(17, 20)};
    BlipContentRefs blipRefs = blip.all("aaa");
    int index = 0;
    for (Range range : blipRefs) {
      assertEquals(expectedRanges[index++], range);
    }
    assertEquals(3, index);

    // Now let's make sure that we can iterate again.
    index = 0;
    for (Range range : blipRefs) {
      assertEquals(expectedRanges[index++], range);
    }
    assertEquals(3, index);

    // Assert iteration with blip refs that has no match.
    assertFalse(blip.all("invalid").iterator().hasNext());
  }

  public void testInlineBlip() throws Exception {
    Blip blip = newBlip("\n1234", Collections.<Annotation>emptyList());
    assertEquals(-1, blip.getInlineBlipOffset());

    Blip inlineBlip = blip.insertInlineBlip(3);
    assertTrue(blip.getChildBlipIds().contains(inlineBlip.getBlipId()));
    assertEquals(3, inlineBlip.getInlineBlipOffset());
    assertEquals("\n12 34", blip.getContent());
    assertEquals(ElementType.INLINE_BLIP, blip.getElements().get(3).getType());
  }

  private static void assertEquals(Annotation one, Annotation two) {
    assertEquals(one.getName(), two.getName());
    assertEquals(one.getValue(), two.getValue());
    assertEquals(one.getRange().getStart(), two.getRange().getStart());
    assertEquals(one.getRange().getEnd(), two.getRange().getEnd());
  }
}
