var angular = require('angular');
var homeComponent = require('./home.component');

var homeModule = angular
  .module('home', [])
  .component('home', homeComponent);

module.exports = homeModule;