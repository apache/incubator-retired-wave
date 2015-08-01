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

package org.waveprotocol.wave.model.document.operation.impl;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;


public class DocOpUtilTest extends TestCase {


  public static final DocOp TEST_DOC1 = new DocOpBuffer() {
      {
        // Check the 3 things we need to escape
        AnnotationBoundaryMapImpl link1 = AnnotationBoundaryMapImpl.builder()
            .initializationValues("link", "12?\"\\3").build();
        AnnotationBoundaryMapImpl link2 = AnnotationBoundaryMapImpl.builder()
            .initializationValues("link", "1").build();
        AnnotationBoundaryMapImpl ann = AnnotationBoundaryMapImpl.builder()
            .initializationValues("x", "3", "y", "3").build();
        AnnotationBoundaryMapImpl linkNull = AnnotationBoundaryMapImpl.builder()
            .initializationValues("link", null).build();
        AnnotationBoundaryMapImpl change = AnnotationBoundaryMapImpl.builder()
            .initializationValues("xa", null, "xb", "5", "xc", "6", "z", "4", "zz", "7")
            .initializationEnd("x").build();
        AnnotationBoundaryMapImpl finish = AnnotationBoundaryMapImpl.builder()
            .initializationEnd("link", "xa", "xb", "xc", "y", "z", "zz").build();
        elementStart("p", new AttributesImpl());
        characters("hi ");
        characters("there");
        elementStart("q", new AttributesImpl("a", "1"));
        // Check things we need to escape
        characters("<some>markup&");
        elementEnd();

        // Check things we need to escape
        elementStart("r", new AttributesImpl("a", "2", "b", "\\\"'"));
        elementEnd();

        elementStart("q", Attributes.EMPTY_MAP);
        annotationBoundary(link1);
        elementEnd();
        elementStart("q", Attributes.EMPTY_MAP);
        annotationBoundary(AnnotationBoundaryMapImpl.builder().build());
        elementEnd();
        elementStart("q", Attributes.EMPTY_MAP);
        annotationBoundary(link1);
        elementEnd();

        elementStart("r", Attributes.EMPTY_MAP);
        annotationBoundary(link1);
        elementEnd();
        elementStart("r", Attributes.EMPTY_MAP);
        annotationBoundary(link2);
        elementEnd();

        annotationBoundary(ann);
        characters("abc");
        annotationBoundary(linkNull);
        characters("def");
        annotationBoundary(change);
        characters("ghi");

        annotationBoundary(finish);

        elementEnd();
      }
    }.finish();

  public static final String TEST_DOC1_XML =
      "<p>hi there" +
      "<q a=\"1\">&lt;some&gt;markup&amp;</q>" +
      "<r a=\"2\" b=\"\\&quot;'\"/>" +
      "<q><?a \"link\"=\"12\\q\\\"\\\\3\"?></q>" +
      "<q/>" +
      "<q/>" +
      "<r/>" +
      "<r><?a \"link\"=\"1\"?></r>" +
      "<?a \"x\"=\"3\" \"y\"=\"3\"?>abc" +
      "<?a \"link\"?>def" +
      "<?a \"x\" \"xb\"=\"5\" \"xc\"=\"6\" \"z\"=\"4\" \"zz\"=\"7\"?>ghi" +
      "<?a \"xb\" \"xc\" \"y\" \"z\" \"zz\"?>" +
      "</p>";

  public void testXmlStringRepresentationIsMinimalAndNormalised() {
    assertEquals(
        TEST_DOC1_XML,
        DocOpUtil.toXmlString(DocOpUtil.asInitialization(TEST_DOC1))
    );
  }
}
