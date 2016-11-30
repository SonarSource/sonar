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
import { connect } from 'react-redux';
import GlobalNavBranding from './global-nav-branding';
import GlobalNavMenu from './global-nav-menu';
import GlobalNavUser from './global-nav-user';
import GlobalNavSearch from './global-nav-search';
import ShortcutsHelpView from './shortcuts-help-view';
import { getCurrentUser } from '../../../store/rootReducer';

const GlobalNav = React.createClass({
  componentDidMount() {
    window.addEventListener('keypress', this.onKeyPress);
  },

  componentWillUnmount() {
    window.removeEventListener('keypress', this.onKeyPress);
  },

  onKeyPress(e) {
    const tagName = e.target.tagName;
    const code = e.keyCode || e.which;
    const isInput = tagName === 'INPUT' || tagName === 'SELECT' || tagName === 'TEXTAREA';
    const isTriggerKey = code === 63;
    const isModalOpen = document.querySelector('html').classList.contains('modal-open');
    if (!isInput && !isModalOpen && isTriggerKey) {
      this.openHelp();
    }
  },

  openHelp(e) {
    if (e) {
      e.preventDefault();
    }
    new ShortcutsHelpView().render();
  },

  render() {
    return (
        <nav className="navbar navbar-global page-container" id="global-navigation">
          <div className="container">
            <GlobalNavBranding {...this.props}/>

            <GlobalNavMenu {...this.props}/>

            <ul className="nav navbar-nav navbar-right">
              <GlobalNavUser {...this.props}/>
              <GlobalNavSearch {...this.props}/>
              <li>
                <a onClick={this.openHelp} href="#">
                  <i className="icon-help navbar-icon"/>
                </a>
              </li>
            </ul>
          </div>
        </nav>
    );
  }
});

const mapStateToProps = state => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(GlobalNav);
