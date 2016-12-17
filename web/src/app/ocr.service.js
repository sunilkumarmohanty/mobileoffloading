var Tesseract = require('tesseract.js');

function OcrService($http, AuthService) {
  'ngInject';

  var scanLocal = function(image) {
    return Tesseract.recognize(image, 'eng');
  };

  var scanRemote = function(images) {
    var data = new FormData();
    for (var i = 0; i < images.length; i++) {
      data.append('images', images[i]);
    }
    return $http.post(
      '/api/scan',
      data,
      {
        transformRequest: angular.identity,
        headers: {
          'Content-Type': undefined,
        }
      }
    );
  };

  var getHistory = function() {
    return $http.get('/api/history');
  };

  return {
    getHistory: getHistory,
    scanLocal: scanLocal,
    scanRemote: scanRemote
  };
}

module.exports = OcrService;