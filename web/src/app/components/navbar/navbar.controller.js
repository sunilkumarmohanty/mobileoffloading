function NavbarController($state, AuthService) {
  'ngInject';
  var vm = this;

  vm.isLoggedIn = AuthService.isLoggedIn;
  vm.logout = function() {
    if (confirm('Are you sure you want to log out?')) {
      AuthService.logout();
      $state.go('login');
    }
  };

  vm.$onInit = function() {
    vm.title = $state.current.data.title;
  };
  vm.goBack = function() {
    $state.go(vm.back);
  };
};

module.exports = NavbarController;