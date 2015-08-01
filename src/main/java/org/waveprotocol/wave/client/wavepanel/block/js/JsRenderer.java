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


package org.waveprotocol.wave.client.wavepanel.block.js;

import org.waveprotocol.wave.client.wavepanel.block.BlockStructure;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure.Node;
import org.waveprotocol.wave.client.wavepanel.block.pojo.PojoRenderer;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;

/**
 * Renders a block tree as Javascript. Evaluating that javascript rebuilds an
 * identical block tree as JSOs.
 *
 * THIS RENDERER ASSUMES THAT BLOCK IDS ARE SAFE (i.e., [a-zA-Z_]*). IF THAT
 * ASSUMPTION IS INVALID, THEN THE GENERATED JS IS A SECURITY RISK.
 *
 */
public final class JsRenderer {

  /**
   * Renders the block structure of a collection of conversations as javascript.
   */
  public static String render(ViewIdMapper viewIdMapper, ConversationView model) {
    BlockStructure v = PojoRenderer.render(viewIdMapper, model);
    final IdentityMap<Node, String> nodes = CollectionUtils.createIdentityMap();
    final StringBuffer decl = new StringBuffer();
    final StringBuffer hookup = new StringBuffer();
    decl.append("var ");
    buildNames(nodes, 0, v.getRoot());
    nodes.each(new IdentityMap.ProcV<Node, String>() {
      @Override
      public void apply(Node node, String name) {
        render(decl, hookup, nodes, node);
      }
    });
    decl.append("n;");
    decl.append(hookup);
    return decl.toString();
  }

  private static int buildNames(IdentityMap<Node, String> names, int c, Node n) {
    names.put(n, "n" + c++);
    for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
      c = buildNames(names, c, child);
    }
    return c;
  }

  private static void render(StringBuffer declarations, StringBuffer hookup,
      IdentityMap<Node, String> names, Node node) {
    String var = names.get(node);
    declarations.append(var);
    declarations.append("={");
    declareAttribute(declarations, "i", "" + node.getId());
    declareAttribute(declarations, "t", "" + node.getType().ordinal());
    declareReference(hookup, var, "f", names.get(node.getFirstChild()));
    declareReference(hookup, var, "l", names.get(node.getLastChild()));
    declareReference(hookup, var, "p", names.get(node.getPreviousSibling()));
    declareReference(hookup, var, "n", names.get(node.getNextSibling()));
    declareReference(hookup, var, "c", names.get(node.getParent()));
    hookup.append("\n");
    declarations.append("},\n");
  }

  private static void declareAttribute(StringBuffer decl, String name, String value) {
    decl.append(name);
    decl.append(":\"");
    decl.append(value);
    decl.append(":\";");
  }

  private static void declareReference(StringBuffer hookup, String var, String name, String value) {
    if (value == null) {
      return;
    }
    hookup.append(var);
    hookup.append(".");
    hookup.append(name);
    hookup.append("=");
    hookup.append(value);
    hookup.append(";");
  }
}
