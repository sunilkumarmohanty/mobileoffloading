var template = require('./login.html');
var controller = require('./login.controller');

var loginComponent = {
  template: template,
  controller: controller,
  controllerAs: 'vm'
};

module.exports = loginComponent;