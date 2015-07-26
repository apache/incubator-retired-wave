// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

/**
 * @fileoverview
 * Tames and exposes an api wave doodads
 *   - exposes domita
 *   - exposes a restricted alert
 *
 * @author Jasvir Nagra (jasvir@google.com)
 * @author Ihab Awad (ihab@google.com)
 */
var caja___ = (function () {
  var cajaDomPrefix = 'c';
  var cajaDomSuffix = 'g___';
  var cajaTamedVirtualDocCount = 0;

  function initialize(domitaVdocElement, wave, doodadModuleText) {
    var imports = ___.copy(___.sharedImports);
    imports.wave = wave;

    imports.outers = imports;

    var uriCallback = {
      rewrite: function (uri, mimeType) {
        if (!/^https?:\/\//i.test(uri)) { return null; }
        if (/^image[/]/.test(mimeType)) { return uri; }
        // TODO(ihab): Substitute WFE's own service here
        return ('http://caja.appspot.com/cajole?url='
            + encodeURIComponent(uri)
            + '&mimeType=' + encodeURIComponent(mimeType));
      }
    };

    var className = cajaDomPrefix + cajaTamedVirtualDocCount++ + cajaDomSuffix;

    domitaVdocElement.innerHTML = '';
    domitaVdocElement.setAttribute('class',
        domitaVdocElement.getAttribute('class') + ' ' + className);

    imports.htmlEmitter___ = new HtmlEmitter(domitaVdocElement);
    attachDocumentStub(
        '-' + className,
        uriCallback, imports, domitaVdocElement);

    wave.setTameDomNodeMaker(imports.tameNode___);
    imports.$v = valijaMaker.CALL___(imports.outers);
    ___.getNewModuleHandler().setImports(imports);
  }

  function tameFrozenFunc(f) {
    return ___.frozenFunc(function() {
      return f.apply(undefined, arguments);
    });
  }

  function tameFrozenRecord(o) {
    var result = {};
    for (var k in o) {
      if (/___$/.test(k)) { continue; }
      result[k] = o[k];
    }
    return ___.freeze(result);
  }

  function tameFrozenArray(a) {
    var result = [];
    for (var i = 0; i < a.length; i++) {
      result[i] = a[i];
    }
    return ___.freeze(result);
  }

  return {
    initialize: initialize,
    tameFrozenFunc: tameFrozenFunc,
    tameFrozenRecord: tameFrozenRecord,
    tameFrozenArray: tameFrozenArray
  };
})();
