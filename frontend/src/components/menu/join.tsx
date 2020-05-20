import { h } from 'preact';

export interface JoinProps {
  name: string;
  updateName: (event) => void;
  buttonTitle: string;
  action: () => void;
}

export const Join = ({ name, updateName, buttonTitle, action }: JoinProps) => (
  <div class="card my-2">
    <div class="card-body">
      <form>
        <div class="form-group">
          <input
            type="text"
            class="form-control"
            id="name"
            placeholder="Enter name"
            value={name}
            onChange={updateName}
          />
        </div>
        <button
          class="btn btn-primary float-right"
          disabled={!name || name.trim().length === 0}
          onClick={action}
        >
          {buttonTitle}
        </button>
      </form>
    </div>
  </div>
);
