import { h } from 'preact';

interface FlashComponentPropsType {
  text?: string;
  dismissAction?: () => void;
}

export const FlashTransitionTimeout = 5000;

export const Flash = ({text, dismissAction}: FlashComponentPropsType) => (
  <div>
    {text != null ? (<div class="fixed-top bg-danger text-light py-2 text-center mb-0" role="alert" onClick={dismissAction}>{text}</div>) : <div />}
  </div>
  )