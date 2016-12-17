var template = require('./app.html');
var controller = require('./app.controller.js');

var AppComponent = {
  template: template,
  controller: controller,
  controllerAs: 'vm'
};

module.exports = AppComponent;