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

package org.waveprotocol.wave.client.editor.extract;

import static org.waveprotocol.wave.client.editor.testing.FakeUser.bksp;
import static org.waveprotocol.wave.client.editor.testing.FakeUser.del;
import static org.waveprotocol.wave.client.editor.testing.FakeUser.move;
import static org.waveprotocol.wave.client.editor.testing.FakeUser.split;
import static org.waveprotocol.wave.client.editor.testing.FakeUser.type;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.junit.client.GWTTestCase;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRawDocument;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.HtmlPoint;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.testing.FakeUser;
import org.waveprotocol.wave.client.editor.testing.MockTypingSink;
import org.waveprotocol.wave.client.editor.testing.TestEditors;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.TimerService;

import org.waveprotocol.wave.model.document.util.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the logic of the typing extractor.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class TypingExtractorGwtTest extends GWTTestCase {

  /** {@inheritDoc} */
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.extract.Tests";
  }

  static final String bigstr = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

  // editor
  private TypingExtractor t;
  private NodeManager m;
  private ContentDocument dom;
  private ContentRawDocument c;
  private ContentElement root;
  private ContentTextNode t1;

  // simplest mock for timer service, only work for user that only call
  // timerService.schedule
  private final List<Task> tasks = new ArrayList<Task>();
  private final TimerService timerService = new TimerService() {

    @Override
    public void cancel(Schedulable job) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isScheduled(Schedulable job) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double currentTimeMillis() {
      return 0;
    }

    @Override
    public int elapsedMillis() {
      return 0;
    }

    @Override
    public void schedule(Task task) {
      tasks.add(task);
    }

    @Override
    public void schedule(IncrementalTask process) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void scheduleDelayed(Task task, int minimumTime) {
      tasks.add(task);
    }

    @Override
    public void scheduleDelayed(IncrementalTask process, int minimumTime) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void scheduleRepeating(IncrementalTask process, int minimumTime, int interval) {
      throw new UnsupportedOperationException();
    }
  };

  // interface
  private final MockTypingSink typingSink = new MockTypingSink();
  private FakeUser user;


  public void testSplittingCausesNoProblems() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 0), "abc");
    runSimpleTest(
        move(hpoint(t1, 0)),
        type("abc"),
        split()
    );

    ContentTextNode t2 = c.createTextNode("hellothereandmore", root, null);
    typingSink.expectDelete(cpoint(t2, 3), 4);
    typingSink.expectInsert(cpoint(t2, 3), "abc");
    runSimpleTest(
        move(hpoint(t2,3)),
        type("abc"),
        split(),
        del(4)
    );


    ContentTextNode t3 = c.createTextNode("fishandchips", root, null);
    typingSink.expectDelete(cpoint(t3, 0), 3);
    runSimpleTest(
        move(hpoint(t3,3)),
        split(),
        bksp(3)
    );

  }

  public void testTypingFromEmpty() throws HtmlMissing, HtmlInserted {
    init();

    typingSink.expectInsert(cpoint(root, null), "abc");
    runSimpleTest(
        move(hpoint(root, null)),
        type("abc")
    );
  }

  public void testTypingTwiceFromEmpty() throws HtmlMissing, HtmlInserted {
    init();

    typingSink.expectInsert(cpoint(root, null), "abc");
    runSimpleTest(
        move(hpoint(root, null)),
        type("ab"),
        type("c")
    );
  }

  public void testTypingLotsFromEmpty() throws HtmlMissing, HtmlInserted {
    init();

    typingSink.expectInsert(cpoint(root, null), "abc");
    runSimpleTest(
        move(hpoint(root, null)),
        type("ad"),
        bksp(1),
        type("bc")
    );
  }

  public void testNoTypingDoesNothing() throws HtmlMissing, HtmlInserted {
    init("something");
    runSimpleTest();
  }

  public void testCancellingOutDoesNothing() throws HtmlMissing, HtmlInserted {
    init("something");

    runSimpleTest(
        move(hpoint(t1, 0)),
        type("abc"),
        bksp(3)
    );

    ContentElement b = c.createElement("b", root, null);
    runSimpleTest(
        move(hpoint(b, null)),
        type("abc"),
        bksp(3)
    );

  }

//  public void testComplicatedMunging() throws HtmlMissing, HtmlInserted {
//    init("something");
//
//    runSimpleTest(
//        move(hpoint(t1, 3)),
//        type("abc"),
//        bksp(3),
//        type("abcdef"),
//        split()
//    );
//
//    ContentElement b = c.createElement("b");
//    c.insertBefore(root, b, null);
//    runSimpleTest(
//        move(hpoint(b, null)),
//        type("abc"),
//        bksp(3)
//    );
//
//  }

  public void testDeleteCharAtStart() throws HtmlMissing, HtmlInserted {
    String str = "something";
    init(str);

    typingSink.expectDelete(cpoint(t1, 0), 1);
    runSimpleTest(
        move(hpoint(t1, 0)),
        del()
        );
  }

  public void testDeleteCharInMiddle() throws HtmlMissing, HtmlInserted {
    String str = "something";
    init(str);

    typingSink.expectDelete(cpoint(t1, 4), 1);
    runSimpleTest(
        move(hpoint(t1, 4)),
        del()
        );
  }

  public void testDeleteCharAtEnd() throws HtmlMissing, HtmlInserted {
    String str = "something";
    init(str);

    typingSink.expectDelete(cpoint(t1, str.length() - 1), 1);
    runSimpleTest(
        move(hpoint(t1, str.length() - 1)),
        del()
        );
  }


  public void testInsertCharInMiddle() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 3), "x");
    runSimpleTest(
        move(hpoint(t1, 3)),
        type("x")
        );
  }

  public void testInsertCharAtStart() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 0), "x");
    runSimpleTest(
        move(hpoint(t1, 0)),
        type("x")
        );
  }

  public void testInsertCharAtEnd() throws HtmlMissing, HtmlInserted {
    String str = "something";
    init(str);

    typingSink.expectInsert(cpoint(t1, str.length()), "x");
    runSimpleTest(
        move(hpoint(t1, str.length())),
        type("x")
        );
  }

  public void testInsert2CharInMiddle() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 3), "xx");
    runSimpleTest(
        move(hpoint(t1, 3)),
        type("xx")
        );
  }

  public void testInsert2CharAtStart() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 0), "xx");
    runSimpleTest(
        move(hpoint(t1, 0)),
        type("xx")
        );
  }

  public void testInsert2CharAtEnd() throws HtmlMissing, HtmlInserted {
    String str = "something";
    init(str);

    typingSink.expectInsert(cpoint(t1, str.length()), "xx");
    runSimpleTest(
        move(hpoint(t1, str.length())),
        type("xx")
        );
  }

  public void testInsertLotsInMiddle() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 3), bigstr);
    runSimpleTest(
        move(hpoint(t1, 3)),
        type(bigstr)
        );
  }

  public void testInsertLotsAtStart() throws HtmlMissing, HtmlInserted {
    init("something");

    typingSink.expectInsert(cpoint(t1, 0), bigstr);
    runSimpleTest(
        move(hpoint(t1, 0)),
        type(bigstr)
        );
  }

  public void testInsertLotsAtEnd() throws HtmlMissing, HtmlInserted {
    String str = "something";
    init(str);

    typingSink.expectInsert(cpoint(t1, str.length()), bigstr);
    runSimpleTest(
        move(hpoint(t1, str.length())),
        type(bigstr)
        );
  }


//  public void testInsertCharAtEnd() throws HtmlMissing, HtmlInserted {
//    String str = "something";
//    init(str);
//
//    typingSink.expectInsert(cpoint(t1, str.length()), "x");
//    runSimpleTest(
//        move(hpoint(t1, str.length())),
//        type("x")
//        );
//  }

  protected void runSimpleTest(Object... actions) throws HtmlMissing, HtmlInserted {
    user.run(t, actions);
    endTypingSequence();
  }

  protected Point<Node> hpoint(Text node, int offset) {
    return Point.inText((Node)node, offset);
  }

  protected Point<Node> hpoint(ContentTextNode node, int offset) throws HtmlMissing {
    HtmlPoint hpointOutput = new HtmlPoint(null, 0);
    node.findNodeletWithOffset(offset, hpointOutput);
    return Point.inText(hpointOutput.getNode(), hpointOutput.getOffset());
  }

  protected Point<ContentNode> cpoint(ContentTextNode node, int offset) {
    return Point.inText((ContentNode)node, offset);
  }

  protected Point<Node> hpoint(ContentElement container, ContentNode nodeAfter) {
    return Point.inElement(
        container.getImplNodelet(), nodeAfter == null ? null : nodeAfter.getImplNodelet());
  }

  protected Point<ContentNode> cpoint(ContentElement container, ContentNode nodeAfter) {
    return Point.inElement(container, nodeAfter);
  }

  protected void init(String text) {
    init();
    t1 = c.createTextNode("something", root, null);
  }

//
//  // convenience method
//  protected void somethingHappened(Text nodelet) throws HtmlMissing, HtmlInserted {
//    t.somethingHappened(nodelet, dom.getSelectionStart(), dom.getSelectionEnd());
//  }

  protected void init() {
    dom = TestEditors.createTestDocument();
    c = dom.debugGetRawDocument();
    root = c.getDocumentElement();
    m = dom.getContext().rendering().getNodeManager();
    user = new FakeUser(dom.getContext().rendering().getFullHtmlView());
    t = new TypingExtractor(typingSink, m, timerService,
        dom.getContext().rendering().getFilteredHtmlView(),
        dom.getContext().rendering().getRenderedContentView(), null, user);
  }

  protected void endTypingSequence() {
    for (Task t : tasks) {
      t.execute();
    }
    tasks.clear();
    typingSink.expectFinished();
  }
}
