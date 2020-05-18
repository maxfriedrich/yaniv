import { h } from 'preact';

export interface ActionsProps {
  throwDisabled: boolean;
  throwAction: () => void;
  yanivDisabled: boolean;
  yanivAction: () => void;
}

export const Actions = ({
  throwDisabled,
  throwAction,
  yanivDisabled,
  yanivAction
}: ActionsProps) => (
  <div>
    <button
      class="btn btn-primary mr-2"
      disabled={throwDisabled}
      onClick={throwAction}
    >
      Throw
    </button>
    <button
      class="btn btn-primary"
      disabled={yanivDisabled}
      onClick={yanivAction}
    >
      Yaniv
    </button>
  </div>
);
