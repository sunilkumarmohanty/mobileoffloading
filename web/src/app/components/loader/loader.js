var angular = require('angular');
var loaderComponent = require('./loader.component');
require('./loader.min.css');

var loaderModule = angular
  .module('loader', [])
  .component('loader', loaderComponent);

module.exports = loaderModule;