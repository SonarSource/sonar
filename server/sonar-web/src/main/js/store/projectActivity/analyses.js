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
// @flow
import type { Analysis, Action } from './duck';

type State = ?Array<Analysis>;

export default (state: State = null, action: Action): State => {
  if (action.type === 'RECEIVE_PROJECT_ACTIVITY') {
    // if first page, or state is empty
    if (action.paging.pageIndex === 1 || !state) {
      return action.analyses;
    } else {
      return [...state, ...action.analyses];
    }
  }

  if (action.type === 'ADD_PROJECT_ACTIVITY_EVENT') {
    if (!state) {
      return null;
    }
    return state.map(analysis => {
      // $FlowFixMe
      if (analysis.key === action.analysis) {
        // $FlowFixMe
        const events = [...analysis.events, action.event];
        return { ...analysis, events };
      } else {
        return analysis;
      }
    });
  }

  if (action.type === 'DELETE_PROJECT_ACTIVITY_EVENT') {
    if (!state) {
      return null;
    }
    return state.map(analysis => {
      // $FlowFixMe
      if (analysis.key === action.analysis) {
        // $FlowFixMe
        const events = analysis.events.filter(event => event.key !== action.event);
        return { ...analysis, events };
      } else {
        return analysis;
      }
    });
  }

  if (action.type === 'CHANGE_PROJECT_ACTIVITY_EVENT') {
    if (!state) {
      return null;
    }
    return state.map(analysis => {
      // $FlowFixMe
      if (analysis.key === action.analysis) {
        const events = analysis.events.map(event => (
            // $FlowFixMe
            event.key === action.event ? { ...event, ...action.changes } : event
        ));
        return { ...analysis, events };
      } else {
        return analysis;
      }
    });
  }

  if (action.type === 'DELETE_PROJECT_ACTIVITY_ANALYSIS') {
    if (!state) {
      return null;
    }
    // $FlowFixMe
    return state.filter(analysis => analysis.key !== action.analysis);
  }

  return state;
};
