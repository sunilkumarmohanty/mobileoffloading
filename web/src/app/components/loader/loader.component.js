var template = require('./loader.html');

var loaderComponent = {
  bindings: {
    text: '<?',
    progress: '<?'
  },
  template: template
};

module.exports = loaderComponent;