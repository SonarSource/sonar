/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.sticker.ws;

import java.util.List;
import org.sonar.api.server.ws.WebService;

public class StickersWs implements WebService {

  static final String SVG_MEDIA_TYPE = "image/svg+xml";

  private final List<StickersWsAction> actions;

  public StickersWs(List<StickersWsAction> actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/stickers");
    controller.setDescription("Generate badges for quality gates or measures");
    controller.setSince("7.1");
    actions.forEach(action -> action.define(controller));
    controller.done();
  }

}
