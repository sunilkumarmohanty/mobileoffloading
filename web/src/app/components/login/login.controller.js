function LoginController($scope, $state, $timeout, AuthService) {
  'ngInject';
  var vm = this;

  vm.$onInit = function() {
    vm.loginError = '';
  }

  vm.login = function() {
    AuthService.login({
      username: document.getElementById('username').value,
      password: document.getElementById('password').value
    }).then(function(res) {
      AuthService.saveToken(res.data.token);
      vm.loginError = '';
      $state.go('home');
    }, function(err) {
      if (angular.isDefined(err.data.message)) {
        vm.loginError = err.data.message;
      } else {
        vm.loginError = 'Login failed, please try again.';
      }
    });
  };

  vm.facebookLogin = function() {
    var explicitLogin = function() {
      FB.login(function(response) {
        console.log('Response from Facebook.login:', response);
        if (response.status === 'connected') {
          AuthService.facebookLogin(response.authResponse.accessToken).then(
            function(res) {
              AuthService.saveToken(res.data.token);
              vm.loginError = '';
              $state.go('home');
            },
            function(err) {
              if (angular.isDefined(err.data.message)) {
                vm.loginError = err.data.message;
              } else {
                vm.loginError = 'Login failed, please try again.';
              }
            }
          );
        } else {
          vm.loginError = 'Login failed, please try again.';
          $scope.$apply();
        }
      });
    };

    FB.getLoginStatus(function(response) {
      console.log('Response from Facebook.getLoginStatus:', response);
      if (response.status === 'connected') {
        AuthService.facebookLogin(response.authResponse.accessToken).then(
          function(res) {
            AuthService.saveToken(res.data.token);
            vm.loginError = '';
            $state.go('home');
          },
          function(err) {
            console.log(err);
            if (angular.isDefined(err.data.message)) {
              vm.loginError = err.data.message;
            } else {
              vm.loginError = 'Login failed, please try again.';
            }
          }
        );
      } else {
        explicitLogin();
      }
    }, function(err) {
      console.log('err', err);
    });
  };
};

module.exports = LoginController;