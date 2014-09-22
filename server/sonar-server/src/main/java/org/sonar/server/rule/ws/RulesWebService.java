/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.ws;

import org.sonar.api.server.ws.WebService;

public class RulesWebService implements WebService {

  private final SearchAction search;
  private final ShowAction show;
  private final TagsAction tags;
  private final CreateAction create;
  private final AppAction app;
  private final UpdateAction update;
  private final DeleteAction delete;

  public RulesWebService(SearchAction search, ShowAction show, TagsAction tags, CreateAction create,
                         AppAction app, UpdateAction update, DeleteAction delete) {
    this.search = search;
    this.show = show;
    this.tags = tags;
    this.create = create;
    this.app = app;
    this.update = update;
    this.delete = delete;
  }

  @Override
  public void define(Context context) {
    NewController controller = context
      .createController("api/rules")
      .setDescription("Coding rules");

    search.define(controller);
    show.define(controller);
    tags.define(controller);
    app.define(controller);
    update.define(controller);
    create.define(controller);
    delete.define(controller);

    controller.done();
  }
}
