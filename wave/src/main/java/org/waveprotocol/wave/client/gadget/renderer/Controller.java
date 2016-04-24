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

package org.waveprotocol.wave.client.gadget.renderer;

import static org.waveprotocol.wave.client.gadget.GadgetLog.log;
import static org.waveprotocol.wave.client.gadget.GadgetLog.logError;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Implements callback logic for the Gadget RPC relay. Calls the GWT RPC
 * listener methods from the context of JavaScript gadget library.
 *
 * The general logic is similar to one in Google
 * javascript.apps.gadgets.container.Controller.
 *
 */
public class Controller {

  /**
   * Overlay type for JavaScript array of mixed element types.
   * TODO: Move this type into GWT's own library.
   */
  public static final class JsArrayMixed extends JavaScriptObject {
    protected JsArrayMixed() {
    }

    public static native JsArrayMixed create() /*-{
      return [];
    }-*/;

    public native void put(int i, JavaScriptObject value) /*-{
      this[i] = value;
    }-*/;

    public native void put(int i, double value) /*-{
      this[i] = value;
    }-*/;

    public native void put(int i, boolean value) /*-{
      this[i] = value;
    }-*/;

    public native void put(int i, String value) /*-{
      this[i] = value;
    }-*/;

    public native JavaScriptObject getObject(int i) /*-{
      return this[i] == null ? null : Object(this[i]);
    }-*/;

    public native double getNumber(int i) /*-{
      return Number(this[i]);
    }-*/;

    public native boolean getBoolean(int i) /*-{
      return Boolean(this[i]);
    }-*/;

    public native String getString(int i) /*-{
      return String(this[i]);
    }-*/;

    public native int length() /*-{
      return this.length;
    }-*/;
  }

  /**
   * Service callback interface.
   */
  private static interface ServiceCallback {
    /**
     * Relays service call to a Gadget listener implementation.
     *
     * @param listener Gadget RPC listener object to receive the call.
     * @param arguments RPC call arguments to pass to listener object.
     */
    void callService(GadgetRpcListener listener, JsArrayMixed arguments)
        throws InvalidArgumentException;
  }

  /**
   * Exposes a way to call JS callback functions.
   */
  private static final class JavaScriptFunction extends JavaScriptObject {
    /**
     * Bans external construction of this class.
     */
    protected JavaScriptFunction() {}

    /**
     * Calls the object as JS function with given arguments.
     *
     * @param args function arguments.
     */
    public native void call(JsArrayMixed args) /*-{
      this(args);
    }-*/;
  }

  /**
   * Invalid Argument Exception used in service callback.
   */
  private static class InvalidArgumentException extends Exception {
    public InvalidArgumentException(String message) {
      super(message);
    }
  }

  /** Enumeration class for supported Gadget services. */
  private enum Service {
    /**
     * Informs the gadget container that the gadget is wave-aware and requests
     * the container to send wave-specific initializaton. This RPC call is going
     * to be initiated by the wave library in the gadget. The wave gadget
     * library will eventually be loaded as a feature requested in the Gadget
     * xml specification. The library will know "a priori" whether the container
     * is wave-capable by checking the "wave" parameter passed in the URL.
     */
    WAVE_ENABLE("wave_enable", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.waveEnable(getArgumentAsString(0, arguments));
      }
    }),

    /**
     * Symmetrical container-to-gadget and gadget-to-container RPC. In this
     * case, the RPC is received by the container from the gadgets to inform
     * about the gadget-initiated state change. The state parameter is in the
     * form of delta that contains only the key-value pairs for the values that
     * should be updated.
     */
    WAVE_GADGET_STATE("wave_gadget_state", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.waveGadgetStateUpdate(getArgument(0, arguments));
      }
    }),

    /**
     * Similar to WAVE_GADGET_STATE, but for private per-user state.
     */
    WAVE_PRIVATE_GADGET_STATE("wave_private_gadget_state", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.wavePrivateGadgetStateUpdate(getArgument(0, arguments));
      }
    }),

    /** Sets gadget container title service. */
    SET_TITLE("set_title", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.setTitle(getArgumentAsString(0, arguments));
      }
    }),

    /** Sets user preference. */
    SET_PREF("set_pref", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        if ((arguments == null) || (arguments.length() < 1)) {
          throw new InvalidArgumentException("Invalid number of arguments.");
        }
        // Note: arguments[0] is a deprecated token parameter, no longer used.
        String[] keyValue = new String[arguments.length() - 1];
        for (int i = 1; i < arguments.length(); ++i) {
          keyValue[i - 1] = getArgumentAsString(i, arguments);
        }
        listener.setPrefs(keyValue);
      }
    }),

    /**
     * Resizes gadget container (vertical dimension only).
     *
     * NOTE(user): This is a standard gadget RPC that only affects the height.
     * The width is updated by a separate non-standard RPC.
     */
    RESIZE_IFRAME("resize_iframe", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.setIframeHeight(getArgumentAsString(0, arguments));
      }
    }),

    /** Sets gadget container width. */
    SET_IFRAME_WIDTH("setIframeWidth", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.setIframeWidth(getArgumentAsString(0, arguments));
      }
    }),

    /**
     * Instructs the browser to navigate to a different view.
     *
     * TODO(user): This is normally used to change gadget views. Consider
     * reimplementing or eliminating this RPC.
     */
    REQUEST_NAVIGATE_TO("requestNavigateTo", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.requestNavigateTo(getArgumentAsString(0, arguments));
      }
    }),

    /** Instructs the browser to navigate to a given fragment. */
    NAVIGATE_TO_FRAGMENT("navigateToFragment", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.requestNavigateTo(getArgumentAsString(0, arguments));
      }
    }),

    /** Updates the persistent state of Podium gadget. */
    UPDATE_PODIUM_STATE("updateState", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.updatePodiumState(getArgumentAsString(0, arguments));
      }
    }),

    /** Logs a message from the gadget. */
    LOG_MESSAGE("wave_log", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.logMessage(getArgumentAsString(0, arguments));
      }
    }),

    /** Sets a gadget snippet displayed in the wave digest. */
    SET_SNIPPET("set_snippet", new ServiceCallback() {
      public void callService(GadgetRpcListener listener, JsArrayMixed arguments)
          throws InvalidArgumentException {
        listener.setSnippet(getArgumentAsString(0, arguments));
      }
    });

    /** The name for the service at JavaScript/wire level. */
    private final String name;

    /** Callback object to relay the calls. */
    private final ServiceCallback callback;

    /**
     * Constructs enum object.
     *
     * @param name the RPC name used at JavaScript/wire level.
     * @param callback callback object to relay the call to RPC listener.
     */
    private Service(String name, ServiceCallback callback) {
      this.name = name;
      this.callback = callback;
    }

    /**
     * Relays service call to a Gadget listener implementation.
     *
     * @param listener Gadget RPC listener object to receive the call.
     * @param serviceCallback JS function to receive the call results or null if
     *        undefined.
     * @param arguments RPC call arguments to pass to listener object.
     */
    public void callService(GadgetRpcListener listener, JavaScriptFunction serviceCallback,
        JsArrayMixed arguments) {
      try {
        callback.callService(listener, arguments);
      } catch (Exception e) {
        // Catch as a non-specific exception to capture unexpected parameter issues.
        if (TO_LOG) {
          logError("Unable to call service " + e.getMessage());
        }
      }
    }

    /**
     * Returns the name of the service used in the RPC relay.
     *
     * @return the name of the service.
     */
    public String getName() {
      return name;
    }

    /**
     * Helper method to fetch an argument by its index from a given argument
     * list.
     *
     * @param index index of the argument to get.
     * @param arguments the argument list.
     * @return the requested argument as a JavaScriptObject.
     * @throws InvalidArgumentException if the list does not have requested
     *         argument.
     */
    private static JavaScriptObject getArgument(int index, JsArrayMixed arguments)
        throws InvalidArgumentException {
      if ((arguments != null) && (arguments.length() > index)) {
        return arguments.getObject(index);
      } else {
        throw new InvalidArgumentException("Missing argument at " + index);
      }
    }

    /**
     * Helper method to fetch an argument by its index from a given argument
     * list. The argument is returned as a string.
     *
     * @param index index of the argument to get.
     * @param arguments the argument list.
     * @return the requested argument as a string.
     * @throws InvalidArgumentException if the list does not have requested
     *         argument.
     */
    private static String getArgumentAsString(int index, JsArrayMixed arguments)
       throws InvalidArgumentException {
      if ((arguments != null) && (arguments.length() > index)) {
        return arguments.getString(index);
      } else {
        throw new InvalidArgumentException("Missing argument at " + index);
      }
    }
  }

  private static final boolean TO_LOG = LogLevel.showDebug();

  // TODO(user): Use CollectionUtils.createStringMap() instead, less bug prone
  // and easier to write unit tests for this class

  /** Callback map. */
  private final StringMap<GadgetRpcListener> callbackMap = CollectionUtils.createStringMap();

  /** Service map. */
  private final StringMap<Service> serviceMap = CollectionUtils.createStringMap();

  /** JavaScript gadgets RPC library object. */
  @SuppressWarnings("unused") // Used in the JavaScript methods.
  private JavaScriptObject hub;

  /** Default Constructor used to create the singleton instance. */
  private Controller() {
    this(getStandardGadgetRpcLibrary());
  }

  /**
   * Generic constructor that can be used for testing.
   *
   * @param library the gadget RPC library object.
   */
  // @VisibleForTesting
  protected Controller(JavaScriptObject library) {
    setGadgetRpcLibrary(library);
    for (Service service : Service.values()) {
      registerService(service.getName());
      serviceMap.put(service.getName(), service);
    }
  }

  /** Singleton instance of this class. */
  // @VisibleForTesting
  protected static Controller instance;

  /**
   * Returns the singleton instance of Controller.
   *
   * @return Controller singleton instance.
   */
  public static Controller getInstance() {
    if (instance == null) {
      instance = new Controller();
    }
    return instance;
  }

  /**
   * Generic callback relay that is called from the JS callback function.
   * Receives the RPC arguments to be processed in gwt.
   *
   * @param service the name of the RPC service called from the gadget.
   * @param gadgetId the name of the gadget that initiated the RPC call.
   * @param serviceCallback JS callback function to return service result or
   *        null if not needed.
   * @param arguments RPC call arguments.
   */
  @SuppressWarnings("unused") // Used in registerService native method.
  private void callback(String service, String gadgetId, JavaScriptFunction serviceCallback,
      JsArrayMixed arguments) {
    StringBuilder builder = null;
    if (TO_LOG) {
      builder = new StringBuilder();
      builder.append(service + " from " + gadgetId);
      if (arguments != null) {
        for (int index = 0; index <  arguments.length(); ++index) {
          builder.append(" arg" + (index + 1) + ":'" + arguments.getString(index) + "'");
        }
      }
      log(builder.toString());
    }
    if (callbackMap.containsKey(gadgetId) && serviceMap.containsKey(service)) {
      serviceMap.get(service).callService(callbackMap.get(gadgetId), serviceCallback, arguments);
    }
  }

  /**
   * Returns the standard gadget RPC library object.
   *
   * @return the standard gadget RPC library object.
   */
  private static native JavaScriptObject getStandardGadgetRpcLibrary() /*-{
    return $wnd.gadgets.rpc;
  }-*/;

  /**
   * Sets the gadget RPC library to be used by the controller.
   *
   * @param library the gadget RPC library to set.
   */
  // @VisibleForTesting
  protected void setGadgetRpcLibrary(JavaScriptObject library) {
    hub = library;
  }

  /**
   * Creates a JS callback function for the given RPC service and registers it
   * with the RPC hub.
   *
   * @param service the name of the service to register.
   */
  private native void registerService(String service) /*-{
    var hub = this.@org.waveprotocol.wave.client.gadget.renderer.Controller::hub;
    if (hub) {
      hub.register(service, function() {
        // This function runs in the JS context that contains values for
        // service name, gadget ID, callback, and args in s, f, c, and a.
        var service = this['s'];
        var gadgetId = this['f'];
        var callback = this['c'] || null;
        var args = this['a'];
        @org.waveprotocol.wave.client.gadget.renderer.Controller::getInstance()().
        @org.waveprotocol.wave.client.gadget.renderer.Controller::callback(Ljava/lang/String;Ljava/lang/String;Lorg/waveprotocol/wave/client/gadget/renderer/Controller$JavaScriptFunction;Lorg/waveprotocol/wave/client/gadget/renderer/Controller$JsArrayMixed;)
        (service, gadgetId, callback, args);
      });
    }
  }-*/;

  /**
   * Calls remote service for the given frame id.
   *
   * @param targetId ID of the frame to send the call to.
   * @param serviceName the name of the service to call.
   * @param arguments the service call arguments.
   */
  public native void call(String targetId, String serviceName, JsArrayMixed arguments) /*-{
    var hub = this.@org.waveprotocol.wave.client.gadget.renderer.Controller::hub;
    hub.call(targetId, serviceName, null, arguments);
  }-*/;

  /**
   * Sets relay URL to be used to send RPCs to the gadget.
   *
   * @param targetId ID of the frame to send RPCs to.
   * @param url the URL that contains the relay code for the frame.
   */
  public native void setRelayUrl(String targetId, String url) /*-{
    var hub = this.@org.waveprotocol.wave.client.gadget.renderer.Controller::hub;
    hub.setRelayUrl(targetId, url, false);
  }-*/;

  /**
   * Sets RPC token to be used to verify RPCs to the gadget.
   *
   * @param targetId ID of the frame to send RPCs to.
   * @param rpcToken The rpcToken specified in the iframe URL of this gadget.
   */
  public native void setRpcToken(String targetId, String rpcToken) /*-{
    var hub = this.@org.waveprotocol.wave.client.gadget.renderer.Controller::hub;
    hub.setAuthToken(targetId, rpcToken);
  }-*/;

  /**
   * Registers a Gadget RPC listener object to receive RPC calls addressed to
   * the given gadget ID.
   *
   * @param gadgetId gadget ID.
   * @param listener Gadget RPC listener object to receive RPC calls.
   */
  public void registerGadgetListener(String gadgetId, GadgetRpcListener listener) {
    callbackMap.put(gadgetId, listener);
  }

  /**
   * Override some of the configuration options.
   *
   * TODO(user): Investigate whether or not we really need this, as it
   * should come from our syndicator options.
   */
  public static native void initializeContainerConfiguration() /*-{
    if ($wnd.gadgets && $wnd.gadgets.config) {
      $wnd.gadgets.config.init(
        {"rpc" : {"useLegacyProtocol" : false,
                  "parentRelayUrl" : "/gadgets/files/container/rpc_relay.html"}
        });
    }
  }-*/;
}
