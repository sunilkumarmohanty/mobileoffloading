var mathjs = require('mathjs');

function ResultController($state, $window, FileSaver, Blob, AuthService) {
  'ngInject';
  var vm = this;

  vm.$onInit = function() {
    if (!AuthService.isLoggedIn()) {
      $state.go('login');
      return;
    }

    if (!vm.appCtrl.results) {
      $state.go('home');
    } else {
      vm.mode = vm.appCtrl.resultsType;
      vm.timestamp = Math.round(new Date().getTime() / 1000);
      vm.text = '';
      vm.localText = '';
      vm.remoteText = '';
      vm.filelist = [];
      vm.fileUrls = [];
      vm.results = vm.appCtrl.results;
      vm.stats = {
        local: {
          time: { avg: null, stdev: null, max: null, maxInd: null, min: null, minInd: null }
        }, remote: {
          time: { avg: null, stdev: null, max: null, maxInd: null, min: null, minInd: null },
          transfer: { avg: null, stdev: null, max: null, maxInd: null, min: null, minInd: null, sum: null }
        }
      };

      // handle local or remote results
      if (vm.mode !== 'bench') {
        for (var i = 0; i < vm.results.length; i++) {
          if (angular.isDefined(vm.results[i].createdAt)) {
            vm.timestamp = vm.results[i].createdAt;
          }
          if (angular.isDefined(vm.results[i].tesseractResult)) {
            vm.text += vm.results[i].tesseractResult.text;
            if (i < vm.results.length - 1) { vm.text += '\n'; }
          }
          if (angular.isDefined(vm.results[i].file)) {
            vm.filelist.push(vm.results[i].file);
          } else {
            vm.fileUrls.push(vm.results[i].fileUrl);
          }
        }
        if (vm.filelist.length > 0) {
          for (var i = 0; i < vm.filelist.length; i++) {
            var url = $window.URL.createObjectURL(vm.filelist[i]);
            vm.fileUrls.push(url);
          }
        }

      // handle benchmark results
      } else {
        // local
        for (var i = 0; i < vm.results.local.results.length; i++) {
          vm.localText += vm.results.local.results[i].tesseractResult.text;
          if (i < vm.results.local.length - 1) {
            vm.localText += '\n';
          }
          vm.filelist.push(vm.results.local.results[i].file);
        }
        if (vm.filelist.length > 0) {
          for (var i = 0; i < vm.filelist.length; i++) {
            var url = $window.URL.createObjectURL(vm.filelist[i]);
            vm.fileUrls.push(url);
          }
        }
        // local stats
        var times = vm.results.local.results.map(function(x) {return x.processingTime});
        vm.stats.local.time.max = mathjs.max(times);
        vm.stats.local.time.min = mathjs.min(times);
        vm.stats.local.time.avg = mathjs.mean(times);
        vm.stats.local.time.std = mathjs.std(times);
        vm.stats.local.time.maxInd = times.indexOf(vm.stats.local.time.max);
        vm.stats.local.time.minInd = times.indexOf(vm.stats.local.time.min);

        // remote
        for (var i = 0; i < vm.results.remote.results.length; i++) {
          vm.remoteText += vm.results.remote.results[i].text;
          if (i < vm.results.local.results.length - 1) {
            vm.remoteText += '\n';
          }
        }
        // remote stats
        times = vm.results.remote.results.map(function(x) {return x.processingTime}); // note spelling
        vm.stats.remote.time.max = mathjs.max(times);
        vm.stats.remote.time.min = mathjs.min(times);
        vm.stats.remote.time.avg = mathjs.mean(times);
        vm.stats.remote.time.std = mathjs.std(times);
        vm.stats.remote.time.maxInd = times.indexOf(vm.stats.remote.time.max);
        vm.stats.remote.time.minInd = times.indexOf(vm.stats.remote.time.min);

        // note that image sizes come from local results
        var sizes = vm.results.local.results.map(function(x) {return x.file.size});
        vm.stats.remote.transfer.max = mathjs.max(sizes);
        vm.stats.remote.transfer.min = mathjs.min(sizes);
        vm.stats.remote.transfer.avg = mathjs.mean(sizes);
        vm.stats.remote.transfer.std = mathjs.std(sizes);
        vm.stats.remote.transfer.sum = mathjs.sum(sizes)
        vm.stats.remote.transfer.maxInd = sizes.indexOf(vm.stats.remote.transfer.max);
        vm.stats.remote.transfer.minInd = sizes.indexOf(vm.stats.remote.transfer.min);
      }
      vm.appCtrl.loading = false;
    }
  };

  vm.prepareRetake = function() {
    vm.appCtrl.waitingRetake = true;
  };

  vm.saveAsFile = function(text, filename) {
    var blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    FileSaver.saveAs(blob, filename);
  };

  vm.saveCurrentResultAsFile = function() {
    vm.saveAsFile(vm.text, 'scan_result.txt')
  };

  vm.clearFilelist = function() {
    if (vm.filelist) {
      for (var i = 0; i < vm.filelist.length; i++) {
        $window.URL.revokeObjectURL(vm.filelist[i]);
      }
      vm.filelist = null;
    }
  };

  vm.$onDestroy = function() {
    vm.clearFilelist();
  };
};

module.exports = ResultController;