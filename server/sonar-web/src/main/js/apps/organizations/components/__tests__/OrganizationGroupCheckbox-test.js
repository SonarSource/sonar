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
import React from 'react';
import { shallow } from 'enzyme';
import OrganizationGroupCheckbox from '../OrganizationGroupCheckbox';

const group = {
  id: '7',
  name: 'professionals',
  description: '',
  membersCount: 12,
  default: false
};

it('should render unchecked', () => {
  const wrapper = shallow(
    <OrganizationGroupCheckbox group={group} checked={false} onCheck={jest.fn()} />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should be able to toggle check', () => {
  const onCheck = jest.fn((group, checked) => wrapper.setProps({ checked }));
  const wrapper = shallow(
    <OrganizationGroupCheckbox group={group} checked={true} onCheck={onCheck} />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.instance().toggleCheck();
  expect(onCheck.mock.calls).toMatchSnapshot();
  expect(wrapper).toMatchSnapshot();
});

it('should disabled default groups', () => {
  const onCheck = jest.fn((group, checked) => wrapper.setProps({ checked }));
  const wrapper = shallow(
    <OrganizationGroupCheckbox
      group={{ ...group, default: true }}
      checked={true}
      onCheck={onCheck}
    />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.instance().toggleCheck();
  expect(onCheck.mock.calls.length).toBe(0);
});
