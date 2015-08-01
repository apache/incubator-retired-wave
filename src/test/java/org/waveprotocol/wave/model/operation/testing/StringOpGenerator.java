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
import java.util.List;
import java.util.Random;

import org.waveprotocol.wave.model.operation.testing.StringDomain.Data;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Component;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Delete;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Insert;
import org.waveprotocol.wave.model.operation.testing.StringDomain.StringOp.Skip;

public class StringOpGenerator implements RandomOpGenerator<Data, StringOp> {

  final int maxComponents = 20;

  @Override
  public StringOp randomOperation(Data state, Random random) {

    int numComponents = random.nextInt(maxComponents);

    List<Component> components = new ArrayList<Component>();

    String str = state.toString();
    int remainingLength = str.length();
    while (numComponents > 0 && remainingLength >= 0) {
      if (remainingLength == 0) {
        components.add(new Insert((char) (random.nextInt(26) + 'A')));
      } else {
        switch (random.nextInt(3)) {
        case 0:
          components.add(new Insert((char) (random.nextInt(26) + 'A')));
          break;
        case 1:
          components.add(new Delete(str.charAt(str.length() - remainingLength)));
          remainingLength--;
          break;
        default:
          components.add(Skip.INSTANCE);
          remainingLength--;
          break;
        }
      }
      numComponents--;
    }

    StringOp op = new StringOp(components);

    return op;
  }

}
