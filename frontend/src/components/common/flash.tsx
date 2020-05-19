import { h } from 'preact';

interface FlashComponentPropsType {
  text?: string;
  dismissAction?: () => void;
}

export const FlashTransitionTimeout = 3500;

const removeQuotes = (text: string) =>
  text.startsWith('"') && text.endsWith('"') ? text.slice(1, -1) : text;

export const Flash = ({ text, dismissAction }: FlashComponentPropsType) => (
  <div>
    {text != null ? (
      <div
        class="fixed-top bg-secondary text-light p-2 text-center mb-0"
        role="alert"
        onClick={dismissAction}
      >
        {removeQuotes(text)}
      </div>
    ) : (
      <div />
    )}
  </div>
);
