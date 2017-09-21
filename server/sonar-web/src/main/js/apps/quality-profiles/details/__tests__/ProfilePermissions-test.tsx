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
jest.mock('../../../../api/quality-profiles', () => ({
  searchUsers: jest.fn(() => Promise.resolve([]))
}));

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import ProfilePermissions from '../ProfilePermissions';
import { click } from '../../../../helpers/testUtils';

const searchUsers = require('../../../../api/quality-profiles').searchUsers as jest.Mock<any>;

const profile = { name: 'Sonar way', language: 'js' };

beforeEach(() => {
  searchUsers.mockClear();
});

it('renders', () => {
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ loading: false, users: [{ login: 'luke', name: 'Luke Skywalker' }] });
  expect(wrapper).toMatchSnapshot();
});

it('opens add users form', () => {
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  (wrapper.instance() as ProfilePermissions).mounted = true;
  wrapper.setState({ loading: false, users: [{ login: 'luke', name: 'Luke Skywalker' }] });
  expect(wrapper.find('ProfilePermissionsAddUserForm').exists()).toBeFalsy();

  click(wrapper.find('button'));
  expect(wrapper.find('ProfilePermissionsAddUserForm').exists()).toBeTruthy();

  wrapper.find('ProfilePermissionsAddUserForm').prop<Function>('onClose')();
  expect(wrapper.find('ProfilePermissionsAddUserForm').exists()).toBeFalsy();
});

it('removes user', () => {
  const wrapper = shallow(<ProfilePermissions profile={profile} />);
  (wrapper.instance() as ProfilePermissions).mounted = true;

  const joda = { login: 'joda', name: 'Joda' };
  wrapper.setState({ loading: false, users: [{ login: 'luke', name: 'Luke Skywalker' }, joda] });
  expect(wrapper.find('ProfilePermissionsUser')).toHaveLength(2);

  wrapper
    .find('ProfilePermissionsUser')
    .first()
    .prop<Function>('onDelete')(joda);
  wrapper.update();
  expect(wrapper.find('ProfilePermissionsUser')).toHaveLength(1);
});

it('fetches users on mount', () => {
  mount(<ProfilePermissions organization="org" profile={profile} />);
  expect(searchUsers).toBeCalledWith({
    language: 'js',
    organization: 'org',
    profile: 'Sonar way',
    selected: true
  });
});
