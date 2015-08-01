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

package org.waveprotocol.wave.model.experimental.schema;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.AttributeNotAllowed;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.InvalidAttributeValue;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.MissingRequiredAttribute;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.RemovingRequiredAttribute;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.Type;
import org.waveprotocol.wave.model.experimental.schema.DocInitializationParser.ParseException;
import org.waveprotocol.wave.model.experimental.schema.SchemaPattern.Prologue;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for SchemaPattern.
 *
 */

public class SchemaFactoryTest extends TestCase {

  private static final Attributes SAMPLE_ATTRIBUTES =
      new AttributesImpl(
          "name1", "value1",
          "name2", "value2");

  private static final AttributesUpdate SAMPLE_ATTRIBUTES_UPDATE =
      new AttributesUpdateImpl(
          "name1", "oldValue1", "newValue1",
          "name2", null, "newValue2",
          "name3", "oldValue3", null);

  /**
   * Tests a schema featuring elements and prologues.
   */
  public void testGoodSchema1() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good1.schema");
    assertEquals(0, test.prologue().size());
    checkValidation_ANA(test.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(test.validateAttributesUpdate(SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, test.validateCharacters("abcd"));
    assertNull(test.child("bad"));
    SchemaPattern test_prologueTest = test.child("prologueTest");
    Prologue test_prologueTest_prologue = test_prologueTest.prologue();
    assertEquals(3, test_prologueTest_prologue.size());
    assertEquals("element1", test_prologueTest_prologue.get(0).elementType());
    assertEquals("element2", test_prologueTest_prologue.get(1).elementType());
    assertEquals("element3", test_prologueTest_prologue.get(2).elementType());
    SchemaPattern test_prologueTest_prologue0 = test_prologueTest_prologue.get(0).pattern();
    assertEquals(0, test_prologueTest_prologue0.prologue().size());
    checkValidation_ANA(test_prologueTest_prologue0.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(test_prologueTest_prologue0.validateAttributesUpdate(
        SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, test_prologueTest_prologue0.validateCharacters("abcd"));
    assertNull(test_prologueTest_prologue0.child("bad"));
    SchemaPattern test_prologueTest_prologue1 = test_prologueTest_prologue.get(1).pattern();
    checkEmptyPattern(test_prologueTest_prologue1);
    SchemaPattern test_prologueTest_prologue2 = test_prologueTest_prologue.get(2).pattern();
    Prologue test_prologueTest_prologue2_prologue = test_prologueTest_prologue2.prologue();
    assertEquals(2, test_prologueTest_prologue2_prologue.size());
    assertEquals("element7", test_prologueTest_prologue2_prologue.get(0).elementType());
    assertEquals("element8", test_prologueTest_prologue2_prologue.get(1).elementType());
    checkValidation_ANA(test_prologueTest_prologue2.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(test_prologueTest_prologue2.validateAttributesUpdate(
        SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, test_prologueTest_prologue2.validateCharacters("abcd"));
    assertNull(test_prologueTest_prologue2.child("bad"));
    checkEmptyPattern(test_prologueTest_prologue0.child("element4"));
    checkEmptyPattern(test_prologueTest_prologue2_prologue.get(0).pattern());
    checkEmptyPattern(test_prologueTest_prologue2_prologue.get(1).pattern());
    checkEmptyPattern(test_prologueTest_prologue2.child("element5"));
    checkEmptyPattern(test_prologueTest_prologue2.child("element6"));
  }

  /**
   * Tests a schema featuring attributes.
   */
  public void testGoodSchema2() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good2.schema");

    /*
     * Test a pattern containing a single non-required attribute whose only
     * allowed value is a fixed string.
     */
    SchemaPattern test_element1 = test.child("element1");
    checkValidation_ANA(test_element1.validateAttributes(new AttributesImpl(
        "bad", "value1")),
        "bad");
    checkValidation_IAV(test_element1.validateAttributes(new AttributesImpl(
        "attribute1", "bad")),
        "attribute1", "bad");
    checkValidation_V(test_element1.validateAttributes(new AttributesImpl(
        "attribute1", "value1")));
    checkValidation_V(test_element1.validateAttributes(AttributesImpl.EMPTY_MAP));
    checkValidation_ANA(test_element1.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "bad", "bad")),
        "bad");
    checkValidation_ANA(test_element1.validateAttributesUpdate(new AttributesUpdateImpl(
        "bad", "value1", "value1")),
        "bad");
    checkValidation_IAV(test_element1.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "bad", "bad")),
        "attribute1", "bad");
    checkValidation_V(test_element1.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1")));
    checkValidation_V(test_element1.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", null, "value1")));
    checkValidation_V(test_element1.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", null)));
    checkValidation_V(test_element1.validateAttributesUpdate(AttributesUpdateImpl.EMPTY_MAP));
    checkValidation_ANA(test_element1.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1",
        "bad", "bad", "bad")),
        "bad");

    /*
     * Test a pattern containing a single non-required attribute with an
     * infinite number of possible values matching a regular expression.
     */
    SchemaPattern test_element2 = test.child("element2");
    checkValidation_ANA(test_element2.validateAttributes(new AttributesImpl(
        "bad", "abbbc")),
        "bad");
    checkValidation_IAV(test_element2.validateAttributes(new AttributesImpl(
        "attribute2", "bad")),
        "attribute2", "bad");
    checkValidation_V(test_element2.validateAttributes(new AttributesImpl(
        "attribute2", "abbbc")));
    checkValidation_ANA(test_element2.validateAttributesUpdate(new AttributesUpdateImpl(
        "bad", "abbbc", "abbbc")),
        "bad");
    checkValidation_IAV(test_element2.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute2", "bad", "bad")),
        "attribute2", "bad");
    checkValidation_V(test_element2.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute2", "abbbc", "abbbc")));
    checkValidation_V(test_element2.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute2", null, "abbbc")));
    checkValidation_V(test_element2.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute2", "abbbc", null)));

    /*
     * Test a pattern containing a mixture of required and non-required attributes.
     */
    SchemaPattern test_element3 = test.child("element3");
    checkValidation_MRA(test_element3.validateAttributes(new AttributesImpl(
        "bad", "value1")),
        "attribute3");
    checkValidation_ANA(test_element3.validateAttributes(new AttributesImpl(
        "attribute3", "value3",
        "bad", "bad")),
        "bad");
    checkValidation_V(test_element3.validateAttributes(new AttributesImpl(
        "attribute3", "value3")));
    checkValidation_V(test_element3.validateAttributes(new AttributesImpl(
        "attribute3", "value3",
        "attribute4", "value4",
        "attribute5", "value5")));
    checkValidation_IAV(test_element3.validateAttributes(new AttributesImpl(
        "attribute3", "bad",
        "attribute4", "value4",
        "attribute5", "value5")),
        "attribute3", "bad");
    checkValidation_IAV(test_element3.validateAttributes(new AttributesImpl(
        "attribute3", "value3",
        "attribute4", "bad",
        "attribute5", "value5")),
        "attribute4", "bad");
    checkValidation_MRA(test_element3.validateAttributes(new AttributesImpl(
        "attribute4", "value4",
        "attribute5", "value5")),
        "attribute3");
    checkValidation_MRA(test_element3.validateAttributes(AttributesImpl.EMPTY_MAP),
        "attribute3");
    checkValidation_ANA(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "bad", "value1", "value1")),
        "bad");
    checkValidation_ANA(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute3", "value3", "value3",
        "bad", "bad", "bad")),
        "bad");
    checkValidation_V(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute3", "value3", "value3")));
    checkValidation_RRA(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute3", "value3", null)),
        "attribute3");
    checkValidation_V(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute3", "value3", "value3",
        "attribute4", "value4", "value4",
        "attribute5", "value5", "value5")));
    checkValidation_IAV(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute3", "bad", "bad",
        "attribute4", "value4", "value4",
        "attribute5", "value5", "value5")),
        "attribute3", "bad");
    checkValidation_IAV(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute3", "value3", "value3",
        "attribute4", "bad", "bad",
        "attribute5", "value5", "value5")),
        "attribute4", "bad");
    checkValidation_V(test_element3.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute4", "value4", "value4",
        "attribute5", "value5", "value5")));
    checkValidation_V(test_element3.validateAttributesUpdate(AttributesUpdateImpl.EMPTY_MAP));

    /*
     * Test a pattern containing a single required attribute attribute.
     */
    SchemaPattern test_element4 = test.child("element4");
    checkValidation_IAV(test_element4.validateAttributes(new AttributesImpl(
        "attribute6", "xyz")),
        "attribute6", "xyz");
    checkValidation_IAV(test_element4.validateAttributes(new AttributesImpl(
        "attribute6", "axyz")),
        "attribute6", "axyz");
    checkValidation_IAV(test_element4.validateAttributes(new AttributesImpl(
        "attribute6", "xyzb")),
        "attribute6", "xyzb");
    checkValidation_V(test_element4.validateAttributes(new AttributesImpl(
        "attribute6", "axyzb")));
    checkValidation_IAV(test_element4.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute6", "xyz", "xyz")),
        "attribute6", "xyz");
    checkValidation_IAV(test_element4.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute6", "axyz", "axyz")),
        "attribute6", "axyz");
    checkValidation_IAV(test_element4.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute6", "xyzb", "xyzb")),
        "attribute6", "xyzb");
    checkValidation_V(test_element4.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute6", "axyzb", "axyzb")));

    /*
     * Test that the "casting" methods of the result objects returned by the
     * validation behave correctly.
     */
    AttributeValidationResult example_V = test_element3.validateAttributes(
        new AttributesImpl(
            "attribute3", "value3"));
    AttributeValidationResult example_ANA = test_element3.validateAttributes(
        new AttributesImpl(
            "attribute3", "value3",
            "bad", "bad"));
    AttributeValidationResult example_IAV = test_element3.validateAttributes(
        new AttributesImpl(
            "attribute3", "bad"));
    AttributeValidationResult example_MRA = test_element3.validateAttributes(
        AttributesImpl.EMPTY_MAP);
    AttributeValidationResult example_RRA = test_element3.validateAttributesUpdate(
        new AttributesUpdateImpl(
            "attribute3", "value3", null));
    assertNull(example_V.asAttributeNotAllowed());
    assertNull(example_V.asInvalidAttributeValue());
    assertNull(example_V.asMissingRequiredAttribute());
    assertNull(example_V.asRemovingRequiredAttribute());
    assertNotNull(example_ANA.asAttributeNotAllowed());
    assertNull(example_ANA.asInvalidAttributeValue());
    assertNull(example_ANA.asMissingRequiredAttribute());
    assertNull(example_ANA.asRemovingRequiredAttribute());
    assertNull(example_IAV.asAttributeNotAllowed());
    assertNotNull(example_IAV.asInvalidAttributeValue());
    assertNull(example_IAV.asMissingRequiredAttribute());
    assertNull(example_IAV.asRemovingRequiredAttribute());
    assertNull(example_MRA.asAttributeNotAllowed());
    assertNull(example_MRA.asInvalidAttributeValue());
    assertNotNull(example_MRA.asMissingRequiredAttribute());
    assertNull(example_MRA.asRemovingRequiredAttribute());
    assertNull(example_RRA.asAttributeNotAllowed());
    assertNull(example_RRA.asInvalidAttributeValue());
    assertNull(example_RRA.asMissingRequiredAttribute());
    assertNotNull(example_RRA.asRemovingRequiredAttribute());
  }

  /**
   * Tests a schema featuring character data.
   */
  public void testGoodSchema3() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good3.schema");
    SchemaPattern test_element1 = test.child("element1");
    assertEquals(-1, test_element1.validateCharacters("eeeeeeee"));
    assertEquals(-1, test_element1.validateCharacters("ffffffff"));
    assertEquals(-1, test_element1.validateCharacters("efghefgh"));
    assertEquals(0, test_element1.validateCharacters("aaaaaaaa"));
    assertEquals(0, test_element1.validateCharacters("bbbbbbbb"));
    assertEquals(4, test_element1.validateCharacters("efgha"));
    SchemaPattern test_element2 = test.child("element2");
    assertEquals(-1, test_element2.validateCharacters("aaaaaaaa"));
    assertEquals(-1, test_element2.validateCharacters("bbbbbbbb"));
    assertEquals(-1, test_element2.validateCharacters("abcdabcd"));
    assertEquals(0, test_element2.validateCharacters("eeeeeeee"));
    assertEquals(0, test_element2.validateCharacters("ffffffff"));
    assertEquals(4, test_element2.validateCharacters("abcde"));
  }

  /**
   * Tests a schema featuring a top-level prologue.
   */
  public void testGoodSchema4() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good4.schema");
    Prologue test_prologue = test.prologue();
    assertEquals(3, test_prologue.size());
    assertEquals("element1", test_prologue.get(0).elementType());
    assertEquals("element2", test_prologue.get(1).elementType());
    assertEquals("element3", test_prologue.get(2).elementType());
    checkValidation_ANA(test.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(test.validateAttributesUpdate(SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, test.validateCharacters("abcd"));
    assertNull(test.child("bad"));
    SchemaPattern test_prologue0 = test_prologue.get(0).pattern();
    assertEquals(0, test_prologue0.prologue().size());
    checkValidation_ANA(test_prologue0.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(test_prologue0.validateAttributesUpdate(SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, test_prologue0.validateCharacters("abcd"));
    assertNull(test_prologue0.child("bad"));
    SchemaPattern test_prologue1 = test_prologue.get(1).pattern();
    checkEmptyPattern(test_prologue1);
    SchemaPattern test_prologue2 = test_prologue.get(2).pattern();
    Prologue test_prologue2_prologue = test_prologue2.prologue();
    assertEquals(2, test_prologue2_prologue.size());
    assertEquals("element7", test_prologue2_prologue.get(0).elementType());
    assertEquals("element8", test_prologue2_prologue.get(1).elementType());
    checkValidation_ANA(test_prologue2.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(test_prologue2.validateAttributesUpdate(SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, test_prologue2.validateCharacters("abcd"));
    assertNull(test_prologue2.child("bad"));
    checkEmptyPattern(test_prologue0.child("element4"));
    checkEmptyPattern(test_prologue2_prologue.get(0).pattern());
    checkEmptyPattern(test_prologue2_prologue.get(1).pattern());
    checkEmptyPattern(test_prologue2.child("element5"));
    checkEmptyPattern(test_prologue2.child("element6"));
  }

  /**
   * Tests a schema featuring top-level optional attributes.
   */
  public void testGoodSchema5() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good5.schema");
    checkValidation_ANA(test.validateAttributes(new AttributesImpl(
        "bad", "value1")),
        "bad");
    checkValidation_ANA(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "bad", "bad")),
        "bad");
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1")));
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "abbbc",
        "attribute3", "axyzb")));
    checkValidation_IAV(test.validateAttributes(new AttributesImpl(
        "attribute1", "bad",
        "attribute2", "abbbc",
        "attribute3", "axyzb")),
        "attribute1", "bad");
    checkValidation_V(test.validateAttributes(AttributesImpl.EMPTY_MAP));
    checkValidation_ANA(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "bad", "value1", "value1")),
        "bad");
    checkValidation_ANA(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1",
        "bad", "bad", "bad")),
        "bad");
    checkValidation_V(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1")));
    checkValidation_V(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", null)));
    checkValidation_V(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1",
        "attribute2", "abbbc", "abbbc",
        "attribute3", "axyzb", "axyzb")));
    checkValidation_IAV(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "bad", "bad",
        "attribute2", "abbbc", "abbbc",
        "attribute3", "axyzb", "axyzb")),
        "attribute1", "bad");
    checkValidation_V(test.validateAttributesUpdate(AttributesUpdateImpl.EMPTY_MAP));
  }

  /**
   * Tests a schema featuring top-level attributes, including a required attribute.
   */
  public void testGoodSchema6() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good6.schema");
    checkValidation_MRA(test.validateAttributes(new AttributesImpl(
        "bad", "value1")),
        "attribute1");
    checkValidation_ANA(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "bad", "bad")),
        "bad");
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1")));
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "abbbc",
        "attribute3", "axyzb")));
    checkValidation_IAV(test.validateAttributes(new AttributesImpl(
        "attribute1", "bad",
        "attribute2", "abbbc",
        "attribute3", "axyzb")),
        "attribute1", "bad");
    checkValidation_IAV(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "bad",
        "attribute3", "axyzb")),
        "attribute2", "bad");
    checkValidation_MRA(test.validateAttributes(new AttributesImpl(
        "attribute2", "abbbc",
        "attribute3", "axyzb")),
        "attribute1");
    checkValidation_MRA(test.validateAttributes(AttributesImpl.EMPTY_MAP),
        "attribute1");
    checkValidation_ANA(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "bad", "value1", "value1")),
        "bad");
    checkValidation_ANA(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1",
        "bad", "bad", "bad")),
        "bad");
    checkValidation_V(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1")));
    checkValidation_RRA(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", null)),
        "attribute1");
    checkValidation_V(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1",
        "attribute2", "abbbc", "abbbc",
        "attribute3", "axyzb", "axyzb")));
    checkValidation_IAV(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "bad", "bad",
        "attribute2", "abbbc", "abbbc",
        "attribute3", "axyzb", "axyzb")),
        "attribute1", "bad");
    checkValidation_IAV(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute1", "value1", "value1",
        "attribute2", "bad", "bad",
        "attribute3", "axyzb", "axyzb")),
        "attribute2", "bad");
    checkValidation_V(test.validateAttributesUpdate(new AttributesUpdateImpl(
        "attribute2", "abbbc", "abbbc",
        "attribute3", "axyzb", "axyzb")));
    checkValidation_V(test.validateAttributesUpdate(AttributesUpdateImpl.EMPTY_MAP));
  }

  /**
   * Tests a schema featuring top-level character data with blacklisting of characters.
   */
  public void testGoodSchema7() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good7.schema");
    assertEquals(-1, test.validateCharacters("eeeeeeee"));
    assertEquals(-1, test.validateCharacters("ffffffff"));
    assertEquals(-1, test.validateCharacters("efghefgh"));
    assertEquals(0, test.validateCharacters("aaaaaaaa"));
    assertEquals(0, test.validateCharacters("bbbbbbbb"));
    assertEquals(4, test.validateCharacters("efgha"));
  }

  /**
   * Tests a schema featuring top-level character data with whitelisting of characters.
   */
  public void testGoodSchema8() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good8.schema");
    assertEquals(-1, test.validateCharacters("aaaaaaaa"));
    assertEquals(-1, test.validateCharacters("bbbbbbbb"));
    assertEquals(-1, test.validateCharacters("abcdabcd"));
    assertEquals(0, test.validateCharacters("eeeeeeee"));
    assertEquals(0, test.validateCharacters("ffffffff"));
    assertEquals(4, test.validateCharacters("abcde"));
  }

  /**
   * Tests a schema featuring references.
   */
  public void testGoodSchema9() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good9.schema");
    checkValidation_V(test.validateAttributes(new AttributesImpl("attribute1", "value1")));
    checkValidation_V(test.validateAttributes(new AttributesImpl("attribute2", "value2")));
    checkValidation_ANA(test.validateAttributes(new AttributesImpl("bad", "bad")), "bad");
    SchemaPattern test_element1 = test.child("element1");
    assertNull(test_element1.child("bad"));
    assertNotNull(test_element1.child("element4"));
    SchemaPattern test_element2 = test.child("element2");
    assertNull(test_element2.child("bad"));
    assertNotNull(test_element2.child("element4"));
    SchemaPattern test_element3 = test.child("element3");
    assertNull(test_element3.child("element4"));
  }

  /**
   * Tests a schema featuring a nested reference.
   */
  public void testGoodSchema10() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good10.schema");
    SchemaPattern test_element0 = test.child("element0");
    checkValidation_V(test_element0.validateAttributes(new AttributesImpl("attribute1", "value1")));
    checkValidation_V(test_element0.validateAttributes(new AttributesImpl("attribute2", "value2")));
    checkValidation_ANA(test_element0.validateAttributes(new AttributesImpl("bad", "bad")), "bad");
    SchemaPattern test_element0_element1 = test_element0.child("element1");
    assertNull(test_element0_element1.child("bad"));
    assertNotNull(test_element0_element1.child("element4"));
    SchemaPattern test_element0_element2 = test_element0.child("element2");
    assertNull(test_element0_element2.child("bad"));
    assertNotNull(test_element0_element2.child("element4"));
    SchemaPattern test_element0_element3 = test_element0.child("element3");
    assertNull(test_element0_element3.child("element4"));
  }

  /**
   * Tests a schema featuring references involving required attributes.
   */
  public void testGoodSchema11() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good11.schema");
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "value2",
        "attribute3", "value3",
        "attribute4", "value4")));
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute3", "value3",
        "attribute4", "value4")));
    checkValidation_V(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "value2",
        "attribute3", "value3")));
    checkValidation_MRA(test.validateAttributes(new AttributesImpl(
        "attribute2", "value2",
        "attribute3", "value3",
        "attribute4", "value4")),
        "attribute1");
    checkValidation_MRA(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "value2",
        "attribute4", "value4")),
        "attribute3");
    checkValidation_ANA(test.validateAttributes(new AttributesImpl(
        "attribute1", "value1",
        "attribute2", "value2",
        "attribute3", "value3",
        "attribute4", "value4",
        "bad", "bad")),
        "bad");
  }

  /**
   * Tests a schema featuring references with double indirection.
   */
  public void testGoodSchema12() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good12.schema");
    checkValidation_V(test.validateAttributes(new AttributesImpl("attribute1", "value1")));
    checkValidation_V(test.validateAttributes(new AttributesImpl("attribute2", "value2")));
    checkValidation_V(test.validateAttributes(new AttributesImpl("attribute3", "value3")));
    checkValidation_ANA(test.validateAttributes(new AttributesImpl("bad", "bad")), "bad");
    SchemaPattern test_element1 = test.child("element1");
    assertNull(test_element1.child("bad"));
    assertNotNull(test_element1.child("element4"));
    SchemaPattern test_element2 = test.child("element2");
    assertNull(test_element2.child("bad"));
    assertNotNull(test_element2.child("element5"));
    SchemaPattern test_element3 = test.child("element3");
    assertNull(test_element2.child("bad"));
    assertNotNull(test_element3.child("element6"));
  }

  /**
   * Tests a schema featuring a recursive reference.
   */
  public void testGoodSchema13() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good13.schema");
    for (int i = 0; i < 16; ++i) {
      checkValidation_V(test.validateAttributes(new AttributesImpl("attribute1", "value1")));
      checkValidation_ANA(test.validateAttributes(new AttributesImpl("bad", "bad")), "bad");
      test = test.child("element1");
    }
  }

  /**
   * Tests a schema featuring recursive references.
   */
  public void testGoodSchema14() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern pattern = readSchema("good14.schema");
    String[] elements = { "element1", "element2", "element3" };
    String[] attributes = { "attribute1", "attribute2", "attribute3" };
    String[] values = { "value1", "value2", "value3" };
    for (int i = 0; i < 16; ++i) {
      int k0 = i % 3;
      int k1 = (i + 1) % 3;
      int k2 = (i + 2) % 3;
      checkValidation_V(pattern.validateAttributes(new AttributesImpl(attributes[k0], values[k0])));
      checkValidation_ANA(
          pattern.validateAttributes(new AttributesImpl(attributes[k1], values[k1])),
          attributes[k1]);
      checkValidation_ANA(
          pattern.validateAttributes(new AttributesImpl(attributes[k2], values[k2])),
          attributes[k2]);
      pattern = pattern.child(elements[i % 3]);
    }
  }

  /**
   * Tests a schema featuring the interleaving of prologues and referenced
   * patterns containing prologues.
   */
  public void testGoodSchema15() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good15.schema");
    Prologue prologue = test.prologue();
    assertEquals(8, test.prologue().size());
    assertEquals("element1", prologue.get(0).elementType());
    assertEquals("element2", prologue.get(1).elementType());
    assertEquals("element5", prologue.get(2).elementType());
    assertEquals("element6", prologue.get(3).elementType());
    assertEquals("element3", prologue.get(4).elementType());
    assertEquals("element4", prologue.get(5).elementType());
    assertEquals("element5", prologue.get(6).elementType());
    assertEquals("element6", prologue.get(7).elementType());
    assertNotNull(prologue.get(0).pattern().child("element7"));
    assertNotNull(prologue.get(1).pattern().child("element8"));
    assertNotNull(prologue.get(2).pattern().child("element11"));
    assertNotNull(prologue.get(3).pattern().child("element12"));
    assertNotNull(prologue.get(4).pattern().child("element9"));
    assertNotNull(prologue.get(5).pattern().child("element10"));
    assertNotNull(prologue.get(6).pattern().child("element11"));
    assertNotNull(prologue.get(7).pattern().child("element12"));
  }

  /**
   * Tests a schema featuring various regular expressions for attribute values.
   */
  public void testGoodSchema16() throws InvalidSchemaException, IOException, ParseException {
    SchemaPattern test = readSchema("good16.schema");
    SchemaPattern pattern = test.child("element1");
    checkValidation_V(pattern.validateAttributes(new AttributesImpl(
        "attribute1", "hello")));
    checkValidation_V(pattern.validateAttributes(new AttributesImpl(
        "attribute1", "world")));
    checkValidation_IAV(pattern.validateAttributes(new AttributesImpl(
        "attribute1", "bad")),
        "attribute1", "bad");
    checkValidation_V(pattern.validateAttributes(new AttributesImpl(
        "attribute2", "abcdgh")));
    checkValidation_V(pattern.validateAttributes(new AttributesImpl(
        "attribute2", "abefgh")));
    checkValidation_IAV(pattern.validateAttributes(new AttributesImpl(
        "attribute2", "bad")),
        "attribute2", "bad");
    checkValidation_V(pattern.validateAttributes(new AttributesImpl(
        "attribute3", "abccccccccccef")));
    checkValidation_V(pattern.validateAttributes(new AttributesImpl(
        "attribute3", "abcdef")));
    checkValidation_IAV(pattern.validateAttributes(new AttributesImpl(
        "attribute3", "abccdef")),
        "attribute3", "abccdef");
  }

  /**
   * Tests a bad schema with no root specified.
   */
  public void testBadSchema1() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad1.schema");
  }

  /**
   * Tests a bad schema with an undefined root.
   */
  public void testBadSchema2() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad2.schema");
  }

  /**
   * Tests a bad schema with a duplicate element.
   */
  public void testBadSchema3() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad3.schema");
  }

  /**
   * Tests a bad schema with a duplicate attribute.
   */
  public void testBadSchema4() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad4.schema");
  }

  /**
   * Tests a bad schema with a duplicate definition.
   */
  public void testBadSchema5() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad5.schema");
  }

  /**
   * Tests a bad schema with an element clash through a reference.
   */
  public void testBadSchema6() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad6.schema");
  }

  /**
   * Tests a bad schema with an attribute clash through a reference.
   */
  public void testBadSchema7() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad7.schema");
  }

  /**
   * Tests a bad schema with an infinite recursion.
   */
  public void testBadSchema8() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad8.schema");
  }

  /**
   * Tests a bad schema with an infinite recursion through indirect means.
   */
  public void testBadSchema9() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad9.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression beginning with '*'.
   */
  public void testBadSchema10() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad10.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression beginning with '?'.
   */
  public void testBadSchema11() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad11.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression beginning with ')'.
   */
  public void testBadSchema12() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad12.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression with an unexpected '*'.
   */
  public void testBadSchema13() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad13.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression with an unexpected '?'.
   */
  public void testBadSchema14() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad14.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression ending with '\\'.
   */
  public void testBadSchema15() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad15.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression with a single '('.
   */
  public void testBadSchema16() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad16.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression with a single ')'.
   */
  public void testBadSchema17() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad17.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression with an unmatched '('.
   */
  public void testBadSchema18() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad18.schema");
  }

  /**
   * Tests a bad schema with a bad regular expression with an unmatched ')'.
   */
  public void testBadSchema19() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad19.schema");
  }

  /**
   * Tests a bad schema with a top-level reference to an undefined definition.
   */
  public void testBadSchema120() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad20.schema");
  }

  /**
   * Tests a bad schema with a reference, not at the top level, to an undefined
   * definition.
   */
  public void testBadSchema21() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad21.schema");
  }

  /**
   * Tests a bad schema containing an invalid element type.
   */
  public void testBadSchema22() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad22.schema");
  }

  /**
   * Tests a bad schema containing an unknown attribute.
   */
  public void testBadSchema23() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad23.schema");
  }

  /**
   * Tests a bad schema with a missing name attribute for a definition element.
   */
  public void testBadSchema24() throws IOException, ParseException {
    checkInvalidSchemaExceptionIsThrown("bad24.schema");
  }

  // TODO(user): We can test for more things similar to testBadSchema23 and
  // testBadSchema24.

  private static void checkEmptyPattern(SchemaPattern pattern) {
    assertEquals(0, pattern.prologue().size());
    checkValidation_ANA(pattern.validateAttributes(SAMPLE_ATTRIBUTES), "name1");
    checkValidation_ANA(pattern.validateAttributesUpdate(SAMPLE_ATTRIBUTES_UPDATE), "name1");
    assertEquals(0, pattern.validateCharacters("abcd"));
    assertNull(pattern.child("bad"));
  }

  /**
   * Checks that the validation result is VALID.
   */
  private static void checkValidation_V(AttributeValidationResult result) {
    assertEquals(Type.VALID, result.getType());
  }

  /**
   * Checks that the validation result is ATTRIBUTE_NOT_ALLOWED.
   */
  private static void checkValidation_ANA(AttributeValidationResult result, String name) {
    assertEquals(Type.ATTRIBUTE_NOT_ALLOWED, result.getType());
    AttributeNotAllowed ana = result.asAttributeNotAllowed();
    assertEquals(name, ana.getName());
  }

  /**
   * Checks that the validation result is INVALID_ATTRIBUTE_VALUE.
   */
  private static void checkValidation_IAV(AttributeValidationResult result,
      String name, String value) {
    assertEquals(Type.INVALID_ATTRIBUTE_VALUE, result.getType());
    InvalidAttributeValue iav = result.asInvalidAttributeValue();
    assertEquals(name, iav.getName());
    assertEquals(value, iav.getValue());
  }

  /**
   * Checks that the validation result is MISSING_REQUIRED_ATTRIBUTE.
   */
  private static void checkValidation_MRA(AttributeValidationResult result, String name) {
    assertEquals(Type.MISSING_REQUIRED_ATTRIBUTE, result.getType());
    MissingRequiredAttribute mra = result.asMissingRequiredAttribute();
    assertEquals(name, mra.getName());
  }

  /**
   * Checks that the validation result is REMOVING_REQUIRED_ATTRIBUTE.
   */
  private static void checkValidation_RRA(AttributeValidationResult result, String name) {
    assertEquals(Type.REMOVING_REQUIRED_ATTRIBUTE, result.getType());
    RemovingRequiredAttribute rra = result.asRemovingRequiredAttribute();
    assertEquals(name, rra.getName());
  }

  private static SchemaPattern readSchema(String filename)
      throws InvalidSchemaException, IOException, ParseException {
    InputStream stream = SchemaFactoryTest.class.getResourceAsStream(filename);
    return SchemaFactory.createSchemaPattern(DocInitializationParser.parseNonCharacterData(stream));
  }

  private static void checkInvalidSchemaExceptionIsThrown(String filename)
      throws IOException, ParseException {
    InputStream stream = SchemaFactoryTest.class.getResourceAsStream(filename);
    try {
      SchemaPattern pattern =
          SchemaFactory.createSchemaPattern(DocInitializationParser.parseNonCharacterData(stream));
      fail("The expected InvalidSchemaException was not thrown");
    } catch (InvalidSchemaException e) {
    }
  }

}
