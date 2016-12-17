var angular = require('angular');
var uiRouter = require('angular-ui-router').default;
var ngFileSaver = require('angular-file-saver');
require('angular-img-http-src');
var angularJwt = require('angular-jwt');
var Components = require('./components/components');
var AppComponent = require('./app.component');
var AuthService = require('./auth.service');
var OcrService = require('./ocr.service');
require('./main.min.css');

angular
  .module('app', [uiRouter, Components.name, ngFileSaver, 'angular.img', angularJwt])
  .component('app', AppComponent)
  .service('AuthService', AuthService)
  .service('OcrService', OcrService)
  .config(function($compileProvider, $httpProvider, $stateProvider, $urlRouterProvider, jwtOptionsProvider) {
    $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|file|blob):/);

    jwtOptionsProvider.config({
      whiteListedDomains: [
        'https://cdn.rawgit.com/naptha/'
      ],
      tokenGetter: ['AuthService', function(AuthService) {
        return AuthService.getToken();
      }]
    });
    $httpProvider.interceptors.push('jwtInterceptor');

    $urlRouterProvider.otherwise('/login');
    $stateProvider
      .state('login', {
        url: '/login',
        component: 'login',
        data: {
          title: 'OCR scan'
        }
      })
      .state('home', {
        url: '/home',
        component: 'home',
        data: {
          title: 'OCR scan',
          data: { requiresAuth: true }
        }
      })
      .state('result', {
        url: '/result',
        component: 'result',
        data: {
          title: 'Scan result',
          data: { requiresAuth: true }
        }
      });
  })
  .filter('msToS', ['$filter', function($filter) {
    return function(ms) {
      var s = '';
      s += (ms/1000).toFixed(3);
      s += 's';
      return s;
    };
  }])
  // adapted from: https://gist.github.com/thomseddon/3511330
  .filter('bytes', function() {
  	return function(bytes, precision) {
      if (bytes === 0) { return '0 B' };
  		if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
  		if (typeof precision === 'undefined') precision = 1;
  		var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
  			number = Math.floor(Math.log(bytes) / Math.log(1024));
  		return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
  	};
  })
  .filter('imgSrc', function() {
    return function(src) {
      if (src.indexOf('blob') > -1) {
        return src;
      } else {
        return '/api/' + src;
      }
    };
  })
  .filter('short', function() {
    return function(input, len) {
      if (input.length < len+1) {
        return input;
      } else {
        return '' + input.substring(0, len) + '...';
      }
    };
  })
  .run(function($rootScope, $state, $window, AuthService) {
    $rootScope.$on('$routeChangeStart', function(ev, next, current) {
      console.log('routeChangeStart');
      if (!current.state.name === 'login' && !AuthService.isLoggedIn()) {
        $state.go('login');
      }
    });

    // Facebook API
    $window.fbAsyncInit = function() {
      FB.init({
        appId: '379043535778305',
        channelUrl: 'facebookchannel.html',
        cookie: false,
        xfbml: true,
        version: 'v2.4'
      });
    };

    (function(d){
      var js,
      id = 'facebook-jssdk',
      ref = d.getElementsByTagName('script')[0];
      if (d.getElementById(id)) { return; }
      js = d.createElement('script');
      js.id = id;
      js.async = true;
      js.src = '//connect.facebook.net/en_US/sdk.js';
      ref.parentNode.insertBefore(js, ref);
    }(document));
  });