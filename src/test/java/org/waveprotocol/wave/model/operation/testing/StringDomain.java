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

package org.waveprotocol.wave.model.operation.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.waveprotocol.wave.model.operation.Domain;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Component;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Delete;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Insert;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Skip;

public class StringDomain implements Domain<StringDomain.Data, StringDomain.StringOp> {

  public static final class Data {
    private final StringBuilder value = new StringBuilder();

    @Override
    public String toString() {
      return value.toString();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Data other = (Data) obj;
      return value.toString().equals(other.value.toString());
    }
  }

  public static class StringOp {
    final List<Component> components;

    public StringOp(List<Component> components) {
      this.components = new ArrayList<Component>(components);
    }

    public StringOp(Component[] ... componentGroups) {
      this.components = new ArrayList<Component>();
      for (Component[] group : componentGroups) {
        components.addAll(Arrays.asList(group));
      }
    }

    void apply(Data d) throws OperationException {
      int location = 0;
      for (Component c : components) {
        location += c.apply(location, d);
      }
    }

    static abstract class Component {
      abstract int apply(int location, Data d) throws OperationException;
    }

    static class Insert extends Component {
      final char inserted;

      public Insert(char inserted) {
        this.inserted = inserted;
      }

      @Override
      int apply(int location, Data d) throws OperationException {
        d.value.insert(location, inserted);
        return 1;
      }

      @Override
      public String toString() {
        return inserted + "";
      }
    }

    static class Delete extends Component {
      final char deleted;

      public Delete(char deleted) {
        this.deleted = deleted;
      }

      @Override
      int apply(int location, Data d) throws OperationException {
        if (location >= d.value.length()) {
          throw new OperationException("Delete at end");
        }
        d.value.deleteCharAt(location);
        return 0;
      }

      @Override
      public String toString() {
        return ((char) (deleted - 'A' + 'a')) + "";
      }
    }

    static class Skip extends Component {
      static final Skip INSTANCE = new Skip();

      @Override
      int apply(int location, Data d) throws OperationException {
        if (location >= d.value.length()) {
          throw new OperationException("Skip past end");
        }
        return 1;
      }

      @Override
      public String toString() {
        return "-";
      }
    }

    @Override
    public String toString() {
      StringBuilder b = new StringBuilder();
      for (Component c : components) {
        b.append(c.toString());
      }
      return b.toString();
    }
  }

  private StringOp.Insert[] i(String text) {
    StringOp.Insert[] group = new StringOp.Insert[text.length()];
    for (int i = 0; i < text.length(); i++) {
      group[i] = new StringOp.Insert(text.charAt(i));
    }
    return group;
  }

  private StringOp.Delete[] d(String text) {
    StringOp.Delete[] group = new StringOp.Delete[text.length()];
    for (int i = 0; i < text.length(); i++) {
      group[i] = new StringOp.Delete(text.charAt(i));
    }
    return group;
  }

  private StringOp.Skip[] s(int distance) {
    StringOp.Skip[] group = new StringOp.Skip[distance];
    for (int i = 0; i < distance; i++) {
      group[i] = StringOp.Skip.INSTANCE;
    }
    return group;
  }

  @Override
  public void apply(StringOp op, Data state) throws OperationException {
    op.apply(state);
  }

  @Override
  public StringOp asOperation(Data state) {
    return new StringOp(i(state.value.toString()));
  }

  @Override
  public StringOp compose(StringOp f, StringOp g) throws OperationException {
    Iterator<Component> after = f.components.iterator();
    Iterator<Component> before = g.components.iterator();

    //System.err.println("XXXX");

    List<Component> list = new ArrayList<Component>();
    Component beforeComponent = null;
    Component afterComponent = null;
    boolean advanceBefore = true;
    boolean advanceAfter = true;
    while ((!advanceBefore || before.hasNext()) && (!advanceAfter || after.hasNext())) {
      if (advanceBefore) beforeComponent = before.next();
      if (advanceAfter) afterComponent = after.next();
      advanceBefore = false;
      advanceAfter = false;
      if (beforeComponent instanceof Skip) {
        if (afterComponent instanceof Skip) {
          //System.err.println("SS " + beforeComponent + " " + afterComponent);
          list.add(Skip.INSTANCE);
          advanceBefore = true;
          advanceAfter = true;
        } else if (afterComponent instanceof Insert) {
          //System.err.println("SI " + beforeComponent + " " + afterComponent);
          list.add(afterComponent);
          advanceAfter = true;
        } else {
          //System.err.println("SD " + beforeComponent + " " + afterComponent);
          assert afterComponent instanceof Delete;
          list.add(afterComponent);
          advanceBefore = true;
          advanceAfter = true;
        }
      } else if (beforeComponent instanceof Insert) {
        if (afterComponent instanceof Skip) {
          //System.err.println("IS " + beforeComponent + " " + afterComponent);
          list.add(beforeComponent);
          advanceBefore = true;
          advanceAfter = true;
        } else if (afterComponent instanceof Insert) {
          //System.err.println("II " + beforeComponent + " " + afterComponent);
          list.add(afterComponent);
          advanceAfter = true;
        } else {
          //System.err.println("ID " + beforeComponent + " " + afterComponent);
          assert afterComponent instanceof Delete;
          advanceBefore = true;
          advanceAfter = true;
        }
      } else {
        assert beforeComponent instanceof Delete;
        if (afterComponent instanceof Skip) {
          //System.err.println("DS " + beforeComponent + " " + afterComponent);
          list.add(beforeComponent);
          advanceBefore = true;
        } else if (afterComponent instanceof Insert) {
          //System.err.println("DI " + beforeComponent + " " + afterComponent);
          list.add(afterComponent);
          advanceAfter = true;
        } else {
          assert afterComponent instanceof Delete;
          //System.err.println("DD " + beforeComponent + " " + afterComponent);
          list.add(beforeComponent);
          advanceBefore = true;
        }
      }
    }

    if (!advanceBefore && beforeComponent != null) {
      list.add(beforeComponent);
    }
    while (before.hasNext()) {
      list.add(before.next());
      //System.err.println("Finally before: " + list.get(list.size() - 1));
    }

    if (!advanceAfter && afterComponent != null) {
      list.add(afterComponent);
    }
    while (after.hasNext()) {
      list.add(after.next());
      //System.err.println("Finally after: " + list.get(list.size() - 1));
    }

    StringOp op = new StringOp(list);

    //System.err.println("Composed " + f + " of " + g + " = " + op);
    return op;
  }

  @Override
  public OperationPair<StringOp> transform(StringOp clientOp, StringOp serverOp)
      throws TransformException {
    Iterator<Component> client = clientOp.components.iterator();
    Iterator<Component> server = serverOp.components.iterator();

    //System.err.println("=======================");

    List<Component> clientList = new ArrayList<Component>();
    List<Component> serverList = new ArrayList<Component>();
    Component clientComponent = null;
    Component serverComponent = null;
    boolean advanceClient = true;
    boolean advanceServer = true;
    while ((!advanceServer || server.hasNext()) && (!advanceClient || client.hasNext())) {
      if (advanceClient) clientComponent = client.next();
      if (advanceServer) serverComponent = server.next();
      advanceClient = false;
      advanceServer = false;
      if (serverComponent instanceof Insert) {
        //System.err.println("I? " + serverComponent + " " + clientComponent);
        serverList.add(serverComponent);
        clientList.add(Skip.INSTANCE);
        advanceServer = true;
      } else if (serverComponent instanceof Skip) {
        if (clientComponent instanceof Skip) {
          //System.err.println("SS " + serverComponent + " " + clientComponent);
          serverList.add(Skip.INSTANCE);
          clientList.add(Skip.INSTANCE);
          advanceServer = true;
          advanceClient = true;
        } else if (clientComponent instanceof Insert) {
          //System.err.println("SI " + serverComponent + " " + clientComponent);
          serverList.add(Skip.INSTANCE);
          clientList.add(clientComponent);
          advanceClient = true;
        } else {
          //System.err.println("SD " + serverComponent + " " + clientComponent);
          assert clientComponent instanceof Delete;
          clientList.add(clientComponent);
          advanceClient = true;
          advanceServer = true;
        }
      } else {
        assert serverComponent instanceof Delete;
        if (clientComponent instanceof Skip) {
          //System.err.println("DS " + serverComponent + " " + clientComponent);
          serverList.add(serverComponent);
          advanceServer = true;
          advanceClient = true;
        } else if (clientComponent instanceof Insert) {
          //System.err.println("DI " + serverComponent + " " + clientComponent);
          serverList.add(Skip.INSTANCE);
          clientList.add(clientComponent);
          advanceClient = true;
        } else {
          //System.err.println("DD " + serverComponent + " " + clientComponent);
          assert clientComponent instanceof Delete;
          advanceClient = true;
          advanceServer = true;
        }
      }
    }

    if (!advanceClient && clientComponent != null) {
      clientList.add(clientComponent);
      //System.err.println("Client:  " + clientList.get(clientList.size() - 1));
    }
    while (client.hasNext()) {
      clientList.add(client.next());
      //System.err.println("Client:  " + clientList.get(clientList.size() - 1));
    }

    if (!advanceServer && serverComponent != null) {
      serverList.add(serverComponent);
      //System.err.println("Server:  " + serverList.get(serverList.size() - 1));
    }
    while (server.hasNext()) {
      serverList.add(server.next());
      //System.err.println("Server:  " + serverList.get(serverList.size() - 1));
    }

    StringOp clientOp2 = new StringOp(clientList);
    StringOp serverOp2 = new StringOp(serverList);

    return new OperationPair<StringOp>(clientOp2, serverOp2);
  }

  @Override
  public boolean equivalent(Data state1, Data state2) {
    return state1.equals(state2);
  }

  @Override
  public Data initialState() {
    return new Data();
  }

  @Override
  public StringOp invert(StringOp operation) {
    // TODO Auto-generated method stub
    //return null;
    throw new UnsupportedOperationException("invert");
  }
}
