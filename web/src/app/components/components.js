var angular = require('angular');
var Home = require('./home/home');
var Loader = require('./loader/loader');
var Login = require('./login/login');
var Navbar = require('./navbar/navbar');
var Result = require('./result/result');

module.exports = angular.module('app.components', [
  Home.name,
  Loader.name,
  Login.name,
  Navbar.name,
  Result.name
]);