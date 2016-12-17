var AuthService = function($http, $window) {
  'ngInject';

  var saveToken = function(token) {
    $window.localStorage['mcctoken'] = token;
  };

  var getToken = function() {
    return $window.localStorage['mcctoken'];
  };

  var register = function(user) {
    return $http.post('/api/register', user);
  };

  var login = function(user) {
    return $http.post('/api/login', user);
  };

  var isLoggedIn = function() {
    var token = getToken();
    if (typeof token !== 'undefined' && token !== 'undefined' && token) {
      try {
        var payload = token.split('.')[1];
        payload = $window.atob(payload);
        payload = JSON.parse(payload);
        return payload.exp > Date.now() / 1000;
      } catch (e) {
        console.log('Invalid token.');
        return false;
      }
    } else {
      return false;
    }
  };

  var logout = function() {
    $window.localStorage.removeItem('mcctoken');
  };

  var facebookLogin = function(token) {
    return $http.post('/api/login/facebook', {
      access_token: token
    });
  };

  return {
    getToken: getToken,
    facebookLogin: facebookLogin,
    register: register,
    login: login,
    isLoggedIn: isLoggedIn,
    logout: logout,
    saveToken: saveToken
  };
};

module.exports = AuthService;