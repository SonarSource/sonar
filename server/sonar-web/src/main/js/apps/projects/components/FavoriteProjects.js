/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import Helmet from 'react-helmet';
import PageHeaderContainer from './PageHeaderContainer';
import ProjectsListContainer from './ProjectsListContainer';
import ProjectsListHeaderContainer from './ProjectsListHeaderContainer';
import GlobalMessagesContainer from '../../../app/components/GlobalMessagesContainer';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

export default class FavoriteProjects extends React.Component {
  static propTypes = {
    user: React.PropTypes.object,
    fetchFavoriteProjects: React.PropTypes.func.isRequired
  };

  componentDidMount () {
    document.querySelector('html').classList.add('dashboard-page');
    this.props.fetchFavoriteProjects();
  }

  componentWillUnmount () {
    document.querySelector('html').classList.remove('dashboard-page');
  }

  render () {
    if (!this.props.user) {
      return null;
    }

    return (
        <div id="projects-page" className="page page-limited">
          <Helmet title={translate('projects.page')} titleTemplate="SonarQube - %s"/>

          <GlobalMessagesContainer/>

          <div className="page-with-sidebar page-with-left-sidebar">
            <div className="page-main">
              <div className="projects-list-container">
                <ProjectsListHeaderContainer/>
                <ProjectsListContainer/>
              </div>
            </div>
            <aside className="page-sidebar-fixed projects-sidebar">
              <PageHeaderContainer/>
              <div className="search-navigator-facets-list">
                <div className="projects-facets-header">
                  <h3>Filters</h3>
                </div>
                <p className="note text-center">Filters are not available.</p>
              </div>
            </aside>
          </div>
        </div>
    );
  }
}
