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
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';

import ItemValue from '../item-value';

describe('System', function () {

  describe('Item Value', function () {
    it('should render string', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value="/some/path/as/an/example"/>);
      const content = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(result, 'code'));
      expect(content.textContent).toBe('/some/path/as/an/example');
    });

    it('should render `true`', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value={true}/>);
      TestUtils.findRenderedDOMComponentWithClass(result, 'icon-check');
    });

    it('should render `false`', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value={false}/>);
      TestUtils.findRenderedDOMComponentWithClass(result, 'icon-delete');
    });

    it('should render object', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value={{ name: 'Java', version: '3.2' }}/>);
      TestUtils.findRenderedDOMComponentWithTag(result, 'table');
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'tr').length).toBe(2);
    });

    it('should render `true` inside object', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value={{ name: 'Java', isCool: true }}/>);
      TestUtils.findRenderedDOMComponentWithTag(result, 'table');
      TestUtils.findRenderedDOMComponentWithClass(result, 'icon-check');
    });

    it('should render object inside object', () => {
      const result = TestUtils.renderIntoDocument(
          <ItemValue value={{ users: { docs: 1, shards: 5 }, tests: { docs: 68, shards: 5 } }}/>);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'table').length).toBe(3);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'tr').length).toBe(6);
    });
  });

  describe('Log Level', function () {
    it('should render select box', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value="INFO" name="Logs Level"/>);
      TestUtils.findRenderedDOMComponentWithTag(result, 'select');
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'option').length).toBe(3);
    });

    it('should set initial value', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value="DEBUG" name="Logs Level"/>);
      const select = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(result, 'select'));
      expect(select.value).toBe('DEBUG');
    });

    it('should render warning', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value="DEBUG" name="Logs Level"/>);
      TestUtils.findRenderedDOMComponentWithClass(result, 'alert');
    });

    it('should not render warning', () => {
      const result = TestUtils.renderIntoDocument(<ItemValue value="INFO" name="Logs Level"/>);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'alert').length).toBe(0);
    });
  });

});
