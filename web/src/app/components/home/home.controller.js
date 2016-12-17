function HomeController($scope, $state, $timeout, $window, OcrService, AuthService) {
  'ngInject';
  vm = this;
  vm.go = $state.go;

  vm.$onInit = function() {
    if (!AuthService.isLoggedIn()) {
      $state.go('login');
      return;
    }

    vm.mode = 'remote';
    vm.selectedInputId = null;
    vm.filelist = null;
    vm.fileUrls = [];
    vm.progress = null;
    vm.history = [];
    // reset progress text
    vm.appCtrl.progressText = '';
    vm.appCtrl.progressProgress = -1;

    vm.loadingHistory = true;
    OcrService.getHistory().then(function(res) {
      vm.history = res.data;
      vm.loadingHistory = false;
    }, function() {
      vm.loadingHistory = false;
    });

    var inputIds = ['select-input', 'capture-input'];
    for (var i = 0; i < inputIds.length; i++) {
      var el = document.getElementById(inputIds[i]);
      el.addEventListener('click', function(ev) {
        vm.selectedInputId = ev.target.id;
      });
      el.addEventListener('change', function(ev) {
        vm.clearFilelist();
        vm.filelist = ev.target.files;
        if (vm.filelist) {
          for (var i = 0; i < vm.filelist.length; i++) {
            var url = $window.URL.createObjectURL(vm.filelist[i]);
            vm.fileUrls.push(url);
          }
          $scope.$apply();
        }
      });
    }

    // trigger retake
    if (vm.appCtrl.waitingRetake) {
      vm.appCtrl.waitingRetake = false;
      $timeout(function () {
        document.getElementById('select-input').click();
      }, 50);
    }
  };

  vm.selectHistoryItem = function(item) {
    results = [];
    var urls = item.originals.length > 0 ? item.originals : item.thumbnails;
    for (var i = 0; i < urls.length; i++) {
      results.push({
        fileUrl: urls[i]
      });
    }
    if (results.length > 0) {
      results[0]['tesseractResult'] = { text: item.text };
      results[0]['createdAt'] = item.createdAt;
    }
    vm.appCtrl.onScanResult(results);
  };

  vm.clearFilelist = function() {
    if (vm.filelist) {
      for (var i = 0; i < vm.filelist.length; i++) {
        $window.URL.revokeObjectURL(vm.filelist[i]);
      }
      vm.filelist = null;
      vm.fileUrls = [];
    }
  };

  vm.clearInputs = function() {
    document.getElementById('select-input').form.reset();
    document.getElementById('capture-input').form.reset();
  };

  vm.launchScanLocal = function() {
    var done = 0;
    var results = [];
    for (var i = 0; i < vm.filelist.length; i++) {
      results[i] = {
        tesseractResult: null,
        file: vm.filelist[i]
      };
      OcrService.scanLocal(vm.filelist[i])
        .progress(function(p) {
          if (vm.filelist.length === 1) {
            // console.log(p);
            p.progress *= 100;
            vm.setLocalProgress(p);
          }
        })
        .then(function(res) {
          results[done].tesseractResult = res;
          done += 1;
          if (done === vm.filelist.length) {
            vm.appCtrl.onScanResult(results);
          }
        });
    }
  };

  vm.launchScanRemote = function() {
    vm.appCtrl.progressText = 'Performing remote scan...\n(This might take a while if you used a very large image)';
    OcrService.scanRemote(vm.filelist).then(function(res) {
      var results = res.data.slice();
      for (var i = 0; i < results.length; i++) {
        if (angular.isDefined(results[i].original) && results[i].original !== "") {
          results[i]['fileUrl'] = results[i].original;
        } else {
          results[i]['fileUrl'] = results[i].thumbnail;
        }
        results[i]['tesseractResult'] = { text: results[i].text };
      }
      vm.appCtrl.onScanResult(results);
    }, function(err) {
      console.log('Remote scan failed:', err);
      vm.appCtrl.loading = false;
      var msg = 'Remote scan failed, please try again.';
      if (angular.isDefined(err.statusText) && err.statusText !== '') {
        msg += 'Error: ' + err.statusText;
      }
      alert(msg);
    });
  };

  vm.launchScanBenchmark = function() {
    vm.appCtrl.progressText = 'Performing benchmark scan...\n(This might take a while if you used a very large image)';

    var results = {
      local: {
        startTime: null,
        endTime: null,
        processingTime: null,
        results: []
      },
      remote: {
        startTime: null,
        endTime: null,
        processingTime: null,
        results: null
      }
    };

    var localDone = 0; // number of done
    var remoteDone = false;

    var bothDone = function() {
      return localDone === vm.filelist.length && remoteDone;
    };

    var whenDone = function() {
      results.local.processingTime = results.local.endTime - results.local.startTime;
      results.remote.processingTime = results.remote.endTime - results.remote.startTime;
      vm.appCtrl.onBenchmarkResults(results);
    };

    results.local.startTime = performance.now();
    for (var i = 0; i < vm.filelist.length; i++) {
      results.local.results[i] = {
        tesseractResult: null,
        startTime: null,
        endTime: null,
        file: vm.filelist[i]
      };
      results.local.results[i].startTime = performance.now();
      OcrService.scanLocal(vm.filelist[i])
        .then(function(res) {
          results.local.results[localDone].endTime = performance.now();
          results.local.results[localDone]['processingTime'] = results.local.results[localDone].endTime - results.local.results[localDone].startTime;
          results.local.results[localDone].tesseractResult = res;
          localDone += 1;
          if (localDone === vm.filelist.length) {
            results.local.endTime = performance.now();
          }
          if (bothDone()) {
            whenDone();
          }
        });
    }

    results.remote.startTime = performance.now();
    OcrService.scanRemote(vm.filelist)
      .then(function(res) {
        results.remote.results = res.data;
        results.remote.endTime = performance.now();
        remoteDone = true;
        if (bothDone()) {
          whenDone();
        }
      }, function(err) {
        console.log('Remote scan failed:', err);
        vm.appCtrl.loading = false;
        var msg = 'Remote scan failed, please try again.';
        if (angular.isDefined(err.statusText) && err.statusText !== '') {
          msg += 'Error: ' + err.statusText;
        }
        alert(msg);
      });
  };

  vm.launchScan = function() {
    vm.appCtrl.loading = true;
    if (vm.mode === 'local') {
      vm.launchScanLocal();
    } else if (vm.mode === 'remote') {
      vm.launchScanRemote();
    } else if (vm.mode === 'bench') {
      vm.launchScanBenchmark();
    }
  };

  vm.setLocalProgress = function(progress) {
    var tesseractStages = [
      'loading tesseract core',
      'initializing tesseract',
      'downloading eng.traineddata.gz',
      'unzipping eng.traineddata.gz',
      'loading eng.traineddata',
      'initializing api',
      'recognizing text'
    ];

    // stages: 0 = not doing anything, 1 = preparing, 2 = recognizing
    var newP = { stage: 0, status: null, stageProgress: 0, oldSubStageProgress: 0 };
    var oldP = vm.progress || newP;

    if (tesseractStages.indexOf(progress.status) > 4) {
      newP.stage = 2;
      newP.status = 'Recognizing text... ';
    } else if (tesseractStages.indexOf(progress.status) > -1) {
      newP.stage = 1;
      newP.status = 'Preparing Tesseract... ';
    }

    if (newP.stage) {
      var numOfSubStages;
      if (newP.stage === 1) { numOfSubStages = 5 };
      if (newP.stage === 2) { numOfSubStages = 2 };
      if (newP.stage === oldP.stage) {
        var diff = progress.progress - oldP.oldSubStageProgress;
        if (diff > 0) {
          newP.stageProgress = oldP.stageProgress + diff / numOfSubStages;
        } else if (diff > 0) {
          newP.stageProgress = oldP.stageProgress + progress.progress / numOfSubStages;
        } else {
          newP.stageProgress = oldP.stageProgress;
        }
      }
    }

    newP.oldSubStageProgress = progress.progress;
    vm.progress = newP;

    vm.appCtrl.setProgress(vm.progress.status, vm.progress.stageProgress);
  };
};

module.exports = HomeController;