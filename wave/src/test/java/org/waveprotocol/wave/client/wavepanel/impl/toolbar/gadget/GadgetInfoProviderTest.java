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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import junit.framework.TestCase;

import static org.mockito.Mockito.*;

import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProviderImpl;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.GadgetCategoryType;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.GadgetInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@link GadgetInfoProvider} class.
 *
 * @author wavegrove@gmail.com
 */
public class GadgetInfoProviderTest extends TestCase {

  GadgetInfoProviderImpl gadgetInfoProvider;
  ArrayList<GadgetInfo> gadgetInfoList;

  private static final GadgetInfo gadget1 = new GadgetInfo(
      "firstTestName",
      "A description",
      GadgetCategoryType.VOTING,
      GadgetCategoryType.OTHER,
      "gadgetUrl1",
      "gadgetAuthor1",
      "gadgetSubmittor1",
      "gadgetImage1");

  private static final GadgetInfo gadget2 = new GadgetInfo(
      "SecondTestname",
      "This is a test DESCRIPTION",
      GadgetCategoryType.PRODUCTIVITY,
      GadgetCategoryType.OTHER,
      "gadgetUrl2",
      "gadgetAuthor2",
      "gadgetSubmittor2",
      "gadgetImage2");

  private static final GadgetInfo gadget3 = new GadgetInfo(
      "thirdtestNAME",
      "Description number three",
      GadgetCategoryType.PRODUCTIVITY,
      GadgetCategoryType.TEAM,
      "gadgetUrl3", "gadgetAuthor3",
      "gadgetSubmittor3",
      "gadgetImage3");

  @Override
  protected void setUp() throws java.lang.Exception {
    List<GadgetInfo> gadgetList = new ArrayList<GadgetInfo>();
    gadgetList.add(gadget1);
    gadgetList.add(gadget2);
    gadgetList.add(gadget3);
    GadgetInfoParser parser = mock(GadgetInfoParser.class);
    when(parser.parseGadgetInfoJson(anyString())).thenReturn(gadgetList);

    gadgetInfoProvider = new GadgetInfoProviderImpl(parser);
    gadgetInfoProvider.addGadgetJson("");
    gadgetInfoList = new ArrayList<GadgetInfo>();
  }

  public void testUnfilteredList() {
    List<GadgetInfo> infoProviderList =
        gadgetInfoProvider.getGadgetInfoList("", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget1);
    gadgetInfoList.add(gadget2);
    gadgetInfoList.add(gadget3);

    assertSame("The unfiltered list got the wrong size", 3, infoProviderList.size());
    assertSameList(gadgetInfoList, infoProviderList);
  }

  public void testFilterFullName() {
    List<GadgetInfo> infoProviderList =
        gadgetInfoProvider.getGadgetInfoList("firstTestName", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget1);

    assertSame("The filter text 'firstTestName' resulted in wrong amount of filtered gadgets", 1,
        infoProviderList.size());
    assertSameList(gadgetInfoList, infoProviderList);
  }

  public void testFilterPartialName() {
    List<GadgetInfo> infoProviderList =
        gadgetInfoProvider.getGadgetInfoList("firstTest", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget1);

    assertSame("The filter text 'firstTest' resulted in wrong amount of filtered gadgets", 1,
        infoProviderList.size());
    assertSameList(gadgetInfoList, infoProviderList);
  }

  public void testFilterCaseInsensitiveName() {
    List<GadgetInfo> infoProviderList1 =
        gadgetInfoProvider.getGadgetInfoList("name", GadgetCategoryType.ALL.getType());
    List<GadgetInfo> infoProviderList2 =
        gadgetInfoProvider.getGadgetInfoList("Name", GadgetCategoryType.ALL.getType());
    List<GadgetInfo> infoProviderList3 =
        gadgetInfoProvider.getGadgetInfoList("NAME", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget1);
    gadgetInfoList.add(gadget2);
    gadgetInfoList.add(gadget3);

    assertSame("The filter text 'name' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList1.size());
    assertSame("The filter text 'Name' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList2.size());
    assertSame("The filter text 'NAME' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList3.size());

    assertSameList(gadgetInfoList, infoProviderList1);
    assertSameList(gadgetInfoList, infoProviderList2);
    assertSameList(gadgetInfoList, infoProviderList3);
  }

  public void testFilterPartialDescription() {
    List<GadgetInfo> infoProviderList =
        gadgetInfoProvider.getGadgetInfoList("This is a", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget2);

    assertSame("The filter text 'This is a' resulted in wrong amount of filtered gadgets", 1,
        infoProviderList.size());
    assertSameList(gadgetInfoList, infoProviderList);
  }

  public void testFilterCaseInsensitiveDescription() {
    List<GadgetInfo> infoProviderList1 =
        gadgetInfoProvider.getGadgetInfoList("description", GadgetCategoryType.ALL.getType());
    List<GadgetInfo> infoProviderList2 =
        gadgetInfoProvider.getGadgetInfoList("Description", GadgetCategoryType.ALL.getType());
    List<GadgetInfo> infoProviderList3 =
        gadgetInfoProvider.getGadgetInfoList("DESCRIPTION", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget1);
    gadgetInfoList.add(gadget2);
    gadgetInfoList.add(gadget3);

    assertSame("The filter text 'description' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList1.size());
    assertSame("The filter text 'Description' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList2.size());
    assertSame("The filter text 'DESCRIPTION' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList3.size());

    assertSameList(gadgetInfoList, infoProviderList1);
    assertSameList(gadgetInfoList, infoProviderList2);
    assertSameList(gadgetInfoList, infoProviderList3);
  }

  public void testFilterNameAndDescription() {
    List<GadgetInfo> infoProviderList =
        gadgetInfoProvider.getGadgetInfoList("test", GadgetCategoryType.ALL.getType());
    gadgetInfoList.add(gadget1);
    gadgetInfoList.add(gadget2);
    gadgetInfoList.add(gadget3);

    assertSame("The filter text 'test' resulted in wrong amount of filtered gadgets", 3,
        infoProviderList.size());
    assertSameList(gadgetInfoList, infoProviderList);
  }

  public void testFilterPrimaryCategory() {
    List<GadgetInfo> infoProviderList1 =
        gadgetInfoProvider.getGadgetInfoList("", GadgetCategoryType.PRODUCTIVITY.getType());
    List<GadgetInfo> infoProviderList2 =
        gadgetInfoProvider.getGadgetInfoList("", GadgetCategoryType.VOTING.getType());

    assertSame("The filter category 'PRODUCTIVITY' resulted in wrong amount of filtered gadgets",
        2, infoProviderList1.size());
    assertSame("The filter category 'VOTING' resulted in wrong amount of filtered gadgets", 1,
        infoProviderList2.size());

    assertSameList(gadgetInfoList, infoProviderList1);
    assertSameList(gadgetInfoList, infoProviderList2);
  }

  public void testFilterSecondaryCategory() {
    List<GadgetInfo> infoProviderList1 =
        gadgetInfoProvider.getGadgetInfoList("", GadgetCategoryType.OTHER.getType());
    List<GadgetInfo> infoProviderList2 =
        gadgetInfoProvider.getGadgetInfoList("", GadgetCategoryType.TEAM.getType());

    assertSame("The filter category 'OTHER' resulted in wrong amount of filtered gadgets", 2,
        infoProviderList1.size());
    assertSame("The filter category 'TEAM' resulted in wrong amount of filtered gadgets", 1,
        infoProviderList2.size());

    assertSameList(gadgetInfoList, infoProviderList1);
    assertSameList(gadgetInfoList, infoProviderList2);
  }

  public void testFilterNone() {
    List<GadgetInfo> infoProviderList =
        gadgetInfoProvider.getGadgetInfoList("abcdefghij", GadgetCategoryType.ALL.getType());

    assertSame("The filter text 'abcdefghij' resulted in wrong amount of filtered gadgets", 0,
        infoProviderList.size());
  }

  private void assertSameList(List<GadgetInfo> expected, List<GadgetInfo> actual) {
    for (int i = 0; i < expected.size(); i++) {
      GadgetInfo gadget1 = expected.get(i);
      GadgetInfo gadget2 = actual.get(i);
      if (!isEqual(gadget1, gadget2)) {
        fail("The filtered gadgets are not the same: " + gadget1.getName() + ", "
            + gadget2.getName());
      }
    }
  }

  private boolean isEqual(GadgetInfo gadget1, GadgetInfo gadget2) {
    if (gadget1.getName().equals(gadget2.getName())
        || gadget1.getDescription().equals(gadget2.getDescription())
        || gadget1.getGadgetUrl().equals(gadget2.getGadgetUrl())
        || gadget1.getImageUrl().equals(gadget2.getImageUrl())
        || gadget1.getPrimaryCategory().getType().equals(gadget2.getPrimaryCategory().getType())
        || gadget1.getSecondaryCategory().getType().equals(gadget2.getSecondaryCategory().getType())
        || gadget1.getSubmittedBy().equals(gadget2.getSubmittedBy())
        || gadget1.getAuthor().equals(gadget2.getAuthor())) {
      return true;
    }
    return false;
  }
}
