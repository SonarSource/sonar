/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.projectanalyses;

import java.util.List;
import javax.annotation.Generated;

/**
 * Delete a project analysis.<br>Requires one of the following permissions:<ul>  <li>'Administer System'</li>  <li>'Administer' rights on the project of the specified analysis</li></ul>
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_analyses/delete">Further information about this action online (including a response example)</a>
 * @since 6.3
 */
@Generated("sonar-ws-generator")
public class DeleteRequest {

  private String analysis;

  /**
   * Analysis key
   *
   * This is a mandatory parameter.
   * Example value: "AU-TpxcA-iU5OvuD2FL1"
   */
  public DeleteRequest setAnalysis(String analysis) {
    this.analysis = analysis;
    return this;
  }

  public String getAnalysis() {
    return analysis;
  }
}
