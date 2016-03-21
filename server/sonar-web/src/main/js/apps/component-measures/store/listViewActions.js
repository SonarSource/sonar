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
import { getComponentTree } from '../../../api/components';
import { enhanceWithSingleMeasure } from '../utils';
import { startFetching, stopFetching } from './statusActions';

export const UPDATE_STORE = 'drilldown/list/UPDATE_STORE';

function updateStore (state) {
  return { type: UPDATE_STORE, state };
}

function makeRequest (baseComponent, metric, options) {
  const asc = metric.direction === 1;
  const ps = 100;
  const finalOptions = { asc, ps };

  if (metric.key.indexOf('new_') === 0) {
    Object.assign(options, {
      s: 'metricPeriod,name',
      metricSort: metric.key,
      metricPeriodSort: 1
    });
  } else {
    Object.assign(options, {
      s: 'metric,name',
      metricSort: metric.key
    });
  }

  Object.assign(finalOptions, options);
  return getComponentTree('leaves', baseComponent.key, [metric.key], finalOptions);
}

function fetchLeaves (baseComponent, metric, pageIndex = 1) {
  const options = { p: pageIndex };

  return makeRequest(baseComponent, metric, options).then(r => {
    const nextComponents = enhanceWithSingleMeasure(r.components);

    return {
      components: nextComponents,
      pageIndex: r.paging.pageIndex,
      total: r.paging.total
    };
  });
}

/**
 * Fetch the first page of components for a given base component
 * @param baseComponent
 * @param metric
 */
export function fetchList (baseComponent, metric) {
  return (dispatch, getState) => {
    const { list } = getState();
    if (list.baseComponent === baseComponent && list.metric === metric) {
      return Promise.resolve();
    }

    dispatch(startFetching());
    return fetchLeaves(baseComponent, metric).then(r => {
      dispatch(updateStore({
        ...r,
        baseComponent,
        metric
      }));
      dispatch(stopFetching());
    });
  };
}

/**
 * Fetch next page of components
 * @param baseComponent
 * @param metric
 */
export function fetchMore (baseComponent, metric) {
  return (dispatch, getState) => {
    const { components, pageIndex } = getState().list;
    dispatch(startFetching());
    return fetchLeaves(baseComponent, metric, pageIndex + 1).then(r => {
      const diff = { ...r, components: [...components, ...r.components] };
      dispatch(updateStore(diff));
      dispatch(stopFetching());
    });
  };
}

/**
 * Select specified component from the list
 * @param component A component to select
 */
export function selectComponent (component) {
  return dispatch => {
    dispatch(updateStore({ selected: component }));
  };
}

/**
 * Select next element from the list of components
 */
export function selectNext () {
  return (dispatch, getState) => {
    const { components, selected } = getState().list;
    const selectedIndex = components.indexOf(selected);
    if (selectedIndex < components.length - 1) {
      const nextSelected = components[selectedIndex + 1];
      dispatch(selectComponent(nextSelected));
    }
  };
}

/**
 * Select previous element from the list of components
 */
export function selectPrevious () {
  return (dispatch, getState) => {
    const { components, selected } = getState().list;
    const selectedIndex = components.indexOf(selected);
    if (selectedIndex > 0) {
      const nextSelected = components[selectedIndex - 1];
      dispatch(selectComponent(nextSelected));
    }
  };
}
