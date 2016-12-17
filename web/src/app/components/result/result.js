var angular = require('angular');
var resultComponent = require('./result.component');

var resultModule = angular
  .module('result', [])
  .component('result', resultComponent);

module.exports = resultModule;