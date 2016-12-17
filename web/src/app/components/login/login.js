var angular = require('angular');
var loginComponent = require('./login.component');

var loginModule = angular
  .module('login', [])
  .component('login', loginComponent);

module.exports = loginModule;