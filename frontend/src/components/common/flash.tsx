import { h } from 'preact';

interface FlashComponentPropsType {
  text?: string;
  dismissAction?: () => void;
}

export const FlashTransitionTimeout = 5000;

const removeQuotes = (text: string) => text.charAt(0) === '"' && text.slice(-1) === '"' ? text.slice(1, -1) : text;

export const Flash = ({text, dismissAction}: FlashComponentPropsType) => (
  <div>
    {text != null ? (<div class="fixed-top bg-danger text-light p-2 text-center mb-0" role="alert" onClick={dismissAction}>{removeQuotes(text)}</div>) : <div />}
  </div>
  )