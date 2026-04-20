import '@testing-library/jest-dom'

window.HTMLElement.prototype.scrollIntoView = () => {};

global.IntersectionObserver = class {
  constructor() {}
  observe() {}
  unobserve() {}
  disconnect() {}
};
