var template = require('./navbar.html');
var controller = require('./navbar.controller');

var navbarComponent = {
  template: template,
  controller: controller,
  controllerAs: 'vm',
  bindings: {
    back: '<'
  }
};

module.exports = navbarComponent;