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
import * as React from 'react';
import { shallow } from 'enzyme';
import EditionBoxes from '../EditionBoxes';
import { EditionStatus } from '../../../api/marketplace';

const DEFAULT_STATUS: EditionStatus = {
  currentEditionKey: 'developer',
  nextEditionKey: '',
  installationStatus: 'NONE'
};

const DEFAULT_EDITIONS = [
  {
    key: 'developer',
    name: 'Developer Edition',
    textDescription: 'foo',
    downloadUrl: 'download_url',
    homeUrl: 'more_url',
    requestUrl: 'license_url'
  },
  {
    key: 'comunity',
    name: 'Comunity Edition',
    textDescription: 'bar',
    downloadUrl: 'download_url',
    homeUrl: 'more_url',
    requestUrl: 'license_url'
  }
];

it('should display the edition boxes correctly', () => {
  const wrapper = getWrapper({ editions: DEFAULT_EDITIONS, loading: true });
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ loading: false });
  expect(wrapper).toMatchSnapshot();
});

it('should display an error message', () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
});

it('should open the license form', () => {
  const wrapper = getWrapper({ editions: DEFAULT_EDITIONS });
  (wrapper.instance() as EditionBoxes).handleOpenLicenseForm(DEFAULT_EDITIONS[0]);
  expect(wrapper.find('LicenseEditionForm').exists()).toBeTruthy();
});

function getWrapper(props = {}) {
  return shallow(
    <EditionBoxes
      canInstall={true}
      canUninstall={true}
      loading={false}
      editionStatus={DEFAULT_STATUS}
      updateCenterActive={true}
      updateEditionStatus={jest.fn()}
      {...props}
    />
  );
}
