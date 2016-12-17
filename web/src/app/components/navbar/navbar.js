var angular = require('angular');
var navbarComponent = require('./navbar.component');

var navbarModule = angular
  .module('navbar', [])
  .component('navbar', navbarComponent);

module.exports = navbarModule;