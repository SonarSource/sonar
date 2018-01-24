/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { sortBy } from 'lodash';
import * as classNames from 'classnames';
import { Query, FacetKey } from '../query';
import { Profile } from '../../../api/quality-profiles';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import { translate } from '../../../helpers/l10n';

interface Props {
  activation: boolean | undefined;
  compareToProfile: string | undefined;
  languages: string[];
  onChange: (changes: Partial<Query>) => void;
  onToggle: (facet: FacetKey) => void;
  open: boolean;
  referencedProfiles: { [profile: string]: Profile };
  value: string | undefined;
}

export default class ProfileFacet extends React.PureComponent<Props> {
  handleItemClick = (selected: string) => {
    const newValue = this.props.value === selected ? '' : selected;
    this.props.onChange({
      activation: this.props.activation === undefined ? true : this.props.activation,
      compareToProfile: undefined,
      profile: newValue
    });
  };

  handleHeaderClick = () => {
    this.props.onToggle('profile');
  };

  handleClear = () => {
    this.props.onChange({
      activation: undefined,
      activationSeverities: [],
      compareToProfile: undefined,
      inheritance: undefined,
      profile: undefined
    });
  };

  handleActiveClick = (event: React.SyntheticEvent<HTMLElement>) => {
    this.stopPropagation(event);
    this.props.onChange({ activation: true, compareToProfile: undefined });
  };

  handleInactiveClick = (event: React.SyntheticEvent<HTMLElement>) => {
    this.stopPropagation(event);
    this.props.onChange({ activation: false, compareToProfile: undefined });
  };

  stopPropagation = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
  };

  getTextValue = () => {
    const { referencedProfiles, value } = this.props;
    if (value) {
      const profile = referencedProfiles[value];
      const name = (profile && `${profile.name} ${profile.languageName}`) || value;
      return [name];
    } else {
      return [];
    }
  };

  renderName = (profile: Profile) => {
    return (
      <>
        {profile.name}
        <span className="note little-spacer-left">{profile.languageName}</span>
      </>
    );
  };

  renderActivation = (profile: Profile) => {
    const isCompare = profile.key === this.props.compareToProfile;
    const activation = isCompare ? true : this.props.activation;
    return (
      <>
        <span
          aria-checked={activation}
          className={classNames('js-active', 'facet-toggle', 'facet-toggle-green', {
            'facet-toggle-active': activation
          })}
          onClick={isCompare ? this.stopPropagation : this.handleActiveClick}
          role="radio"
          tabIndex={-1}>
          active
        </span>
        <span
          aria-checked={!activation}
          className={classNames('js-inactive', 'facet-toggle', 'facet-toggle-red', {
            'facet-toggle-active': !activation
          })}
          onClick={isCompare ? this.stopPropagation : this.handleInactiveClick}
          role="radio"
          tabIndex={-1}>
          inactive
        </span>
      </>
    );
  };

  renderItem = (profile: Profile) => {
    const active = [this.props.value, this.props.compareToProfile].includes(profile.key);

    return (
      <FacetItem
        active={active}
        className={this.props.compareToProfile === profile.key ? 'compare' : undefined}
        key={profile.key}
        name={this.renderName(profile)}
        onClick={this.handleItemClick}
        stat={this.renderActivation(profile)}
        value={profile.key}
      />
    );
  };

  render() {
    const { languages, referencedProfiles } = this.props;
    let profiles = Object.values(referencedProfiles);
    if (languages.length > 0) {
      profiles = profiles.filter(profile => languages.includes(profile.language));
    }
    profiles = sortBy(
      profiles,
      profile => profile.name.toLowerCase(),
      profile => profile.languageName
    );

    return (
      <FacetBox>
        <FacetHeader
          name={translate('coding_rules.facet.qprofile')}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.getTextValue()}
        />

        {this.props.open && <FacetItemsList>{profiles.map(this.renderItem)}</FacetItemsList>}
      </FacetBox>
    );
  }
}