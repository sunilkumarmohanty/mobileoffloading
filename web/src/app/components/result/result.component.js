var template = require('./result.html');
var controller = require('./result.controller');

var resultComponent = {
  template: template,
  controller: controller,
  controllerAs: 'vm',
  require: {
    appCtrl: '^app'
  }
};

module.exports = resultComponent;