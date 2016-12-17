function AppController($scope, $state, $timeout, OcrService) {
  'ngInject';
  var vm = this;

  vm.$onInit = function() {
    vm.loading = false;
    vm.results = null;
    vm.resultsType = null; // normal | bench
    vm.loaderText = null;
    vm.loaderProgress = null;
    vm.waitingRetake = false;

    $scope.$watch('vm.waitingRetake', function() {
      if (vm.waitingRetake) {
        $state.go('home');
      }
    });
  };

  vm.onScanResult = function(results) {
    $timeout(function() {
      vm.loading = false;
    });
    vm.results = results;
    vm.resultsType = 'normal';
    $state.go('result');
  };

  vm.onBenchmarkResults = function(results) {
    vm.resultsType = 'bench';
    vm.results = results;
    $state.go('result');
  };

  vm.setProgress = function(text, progress) {
    vm.progressText = text;
    vm.progressProgress = progress;
    $scope.$apply();
  };
};

module.exports = AppController;
