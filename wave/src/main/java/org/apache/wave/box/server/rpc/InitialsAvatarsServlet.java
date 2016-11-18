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

package org.apache.wave.box.server.rpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.common.io.Resources;
import org.waveprotocol.wave.util.logging.Log;

import javax.imageio.ImageIO;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A servlet for fetching the users Initials Avatar.
 */
@Singleton
public final class InitialsAvatarsServlet extends HttpServlet {
  private static final Log LOG = Log.get(InitialsAvatarsServlet.class);
  private BufferedImage DEFAULT;

  @Inject
  public InitialsAvatarsServlet() throws IOException {
    DEFAULT = ImageIO.read(Resources.getResource("org/apache/wave/box/server/rpc/InitialsAvatarDefault.jpg"));
  }

  /**
   * Create an http response to the fetch query. Main entrypoint for this class.
   */
  @Override
  @VisibleForTesting
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
    response.setContentType("image/jpg");
    ImageIO.write(DEFAULT, "JPG", response.getOutputStream());
  }
}
