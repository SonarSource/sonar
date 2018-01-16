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
import SimpleModal from '../../../components/controls/SimpleModal';
import { translate } from '../../../helpers/l10n';

interface Props {
  children: (
    props: { onClick: (event: React.SyntheticEvent<HTMLButtonElement>) => void }
  ) => React.ReactNode;
  confirmButtonText: string;
  isDestructive?: boolean;
  modalBody: React.ReactNode;
  modalHeader: string;
  onConfirm: () => void | Promise<void>;
}

interface State {
  modal: boolean;
}

// TODO move this component to components/ and use everywhere!
export default class ConfirmButton extends React.PureComponent<Props, State> {
  state: State = { modal: false };

  handleButtonClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ modal: true });
  };

  handleSubmit = () => {
    this.setState({ modal: false });
    this.props.onConfirm();
  };

  handleCloseModal = () => {
    this.setState({ modal: false });
  };

  render() {
    const { confirmButtonText, isDestructive, modalBody, modalHeader } = this.props;

    return (
      <>
        {this.props.children({ onClick: this.handleButtonClick })}
        {this.state.modal && (
          <SimpleModal
            header={modalHeader}
            onClose={this.handleCloseModal}
            onSubmit={this.handleSubmit}>
            {({ onCloseClick, onSubmitClick, submitting }) => (
              <>
                <header className="modal-head">
                  <h2>{modalHeader}</h2>
                </header>

                <div className="modal-body">{modalBody}</div>

                <footer className="modal-foot">
                  {submitting && <i className="spinner spacer-right" />}
                  <button
                    className={isDestructive ? 'button-red' : undefined}
                    disabled={submitting}
                    onClick={onSubmitClick}>
                    {confirmButtonText}
                  </button>
                  <a href="#" onClick={onCloseClick}>
                    {translate('cancel')}
                  </a>
                </footer>
              </>
            )}
          </SimpleModal>
        )}
      </>
    );
  }
}
