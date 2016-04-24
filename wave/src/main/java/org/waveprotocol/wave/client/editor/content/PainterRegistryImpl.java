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

package org.waveprotocol.wave.client.editor.content;

import com.google.common.annotations.VisibleForTesting;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;

import org.waveprotocol.wave.model.util.ChainedData;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.DataDomain;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Basic implementation of a PainterRegistry
 *
 * TODO(user): Changes to the registry are propagated to its extensions.
 * Investigate if this is really necessary, as there are costs involved and
 * cleaning up issues.
 *
 */
public final class PainterRegistryImpl implements PainterRegistry {
  private static class PaintData {
    private final Set<BoundaryFunction> boundaryFuncs = new HashSet<BoundaryFunction>();
    private final Set<PaintFunction> paintFuncs = new HashSet<PaintFunction>();
    private final StringSet keys = CollectionUtils.createStringSet();
  }

  private static final DataDomain<PaintData, PaintData> paintDataDomain =
      new DataDomain<PaintData, PaintData>() {
        @Override
        public void compose(PaintData target, PaintData changes, PaintData base) {
          CollectionUtils.STRING_SET_DOMAIN.compose(target.keys, changes.keys, base.keys);
          CollectionUtils.<PaintFunction>hashSetDomain().compose(
              target.paintFuncs, changes.paintFuncs, base.paintFuncs);
          CollectionUtils.<BoundaryFunction>hashSetDomain().compose(
              target.boundaryFuncs, changes.boundaryFuncs, base.boundaryFuncs);
        }

        @Override
        public PaintData empty() {
          return new PaintData();
        }

        @Override
        public PaintData readOnlyView(PaintData modifiable) {
          return modifiable;
        }
      };

  private final ChainedData<PaintData, PaintData> data;
  private final String paintTagName;
  private final String boundaryTagName;
  private final AnnotationPainter painter;

  public PainterRegistryImpl(String paintTagName, String boundaryTagName,
      AnnotationPainter painter) {
    Preconditions.checkNotNull(painter, "Null painter");
    this.data = new ChainedData<PaintData, PaintData>(paintDataDomain);
    this.painter = painter;
    this.paintTagName = paintTagName;
    this.boundaryTagName = boundaryTagName;
  }

  private PainterRegistryImpl(PainterRegistryImpl parent) {
    Preconditions.checkNotNull(parent, "Use the other constructor for the root");
    this.data = new ChainedData<PaintData, PaintData>(parent.data);
    paintTagName = parent.getPaintTagName();
    boundaryTagName = parent.getBoundaryTagName();
    painter = parent.getPainter();
  }

  @Override
  public PainterRegistry createExtension() {
    return new PainterRegistryImpl(this);
  }

  @Override
  public String getPaintTagName() {
    return paintTagName;
  }

  @Override
  public String getBoundaryTagName() {
    return boundaryTagName;
  }

  @Override
  public ReadableStringSet getKeys() {
    return data.inspect().keys;
  }

  @Override
  public Set<PaintFunction> getPaintFunctions() {
    return data.inspect().paintFuncs;
  }

  @Override
  public Set<AnnotationPainter.BoundaryFunction> getBoundaryFunctions() {
    return data.inspect().boundaryFuncs;
  }

  @Override
  public AnnotationPainter getPainter() {
    return painter;
  }

  @Override
  public void registerBoundaryFunction(ReadableStringSet dependentKeys,
      BoundaryFunction function) {
    PaintData modifying = data.modify();
    modifying.keys.addAll(dependentKeys);
    modifying.boundaryFuncs.add(function);
  }

  @Override
  public void registerPaintFunction(ReadableStringSet dependentKeys, PaintFunction function) {
    PaintData modifying = data.modify();
    modifying.keys.addAll(dependentKeys);
    modifying.paintFuncs.add(function);
  }

  @Override
  public void unregisterBoundaryFunction(ReadableStringSet dependentKeys,
      BoundaryFunction function) {
    data.modify().keys.removeAll(dependentKeys);
    // Need proper reference counting implementation. For now, just don't remove the function.
    // This will break if someone registers more than one function for a given key, or if
    // they want to remove the function once no more keys are associated with it.
  }

  @Override
  public void unregisterPaintFunction(ReadableStringSet dependentKeys, PaintFunction function) {
    data.modify().keys.removeAll(dependentKeys);
    // Need proper reference counting implementation. For now, just don't remove the function.
    // This will break if someone registers more than one function for a given key, or if
    // they want to remove the function once no more keys are associated with it.
  }

  @VisibleForTesting
  public double debugGetVersion() {
    return data.debugGetVersion();
  }

  @VisibleForTesting
  public double debugGetKnownParentVersion() {
    return data.debugGetKnownParentVersion();
  }
}
