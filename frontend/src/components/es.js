import { h, Component } from 'preact';

export class EventSourceTest extends Component {
  constructor() {
    super();
    this.state = [];
  }
  componentDidMount = () => {
    this.source = new EventSource(`/rest/test-stream`);
    this.source.onmessage = event => {
      this.setState(event);
    };
    this.source.onerror = error => {
      this.setState(error);
    };
  };
  render() {
    return <div>{JSON.stringify(this.state)}</div>;
  }
}
