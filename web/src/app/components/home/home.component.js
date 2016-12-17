var template = require('./home.html');
var controller = require('./home.controller');

var homeComponent = {
  template: template,
  controller: controller,
  controllerAs: 'vm',
  require: {
    appCtrl: '^app'
  }
};

module.exports = homeComponent;