var auth = require('../conf/auth.conf');
var crypto = require('crypto');
var fs = require('fs');
var Grid = require('gridfs-stream');
var mime = require('mime');
var mongoose = require('mongoose');
var multer = require('multer');
var sharp = require('sharp');
var Tesseract = require('tesseract.js');

// file extension workaround from
// https://github.com/expressjs/multer/issues/170#issuecomment-123362345
var storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, './backend/uploads/');
  },
  filename: function (req, file, cb) {
    crypto.pseudoRandomBytes(16, function (err, raw) {
      cb(null, raw.toString('hex') + Date.now() + '.' + mime.extension(file.mimetype));
    });
  }
});
var upload = multer({ storage: storage });

Grid.mongo = mongoose.mongo;
var gfs = new Grid(mongoose.connection.db);

var User = mongoose.model('User');
var Scan = mongoose.model('Scan');

var handleError = function(err, res) {
  console.error(err);
  res.status(500).json({
    'message': err
  });
};

var getFile = function(req, res, next) {
  var readstream = gfs.createReadStream({
    filename: req.params.filename
  });
  readstream.on('error', function(err) {
    res.send('Image not found');
  });
  readstream.pipe(res);
};

var deleteFile = function(req, res, next) {
  gfs.exist({
    filename: req.params.filename
  }, function(err, file) {
    if (err) {
      return res.send('An error occured');
    }
    if (file) {
      gfs.remove({
        filename: req.params.filename
      }, function(err) {
        if (err) {
          return res.send('An error occured');
        }
        res.send('Image deleted');
      });
    } else {
      res.send('Image not found');
    }
  });
};

var prepareImages = function(req, res, next) {
  var names = [];
  var done = 0;

  // convert jpeg to png
  req.files.forEach(function(file, idx) {
    var filetype = file.mimetype.substring(file.mimetype.indexOf('/') + 1, file.mimetype.length);
    if (['jpg', 'jpeg'].indexOf(filetype) > -1) {
      var lastPeriodAt = file.filename.lastIndexOf('.');
      var pathPrefix = './backend/uploads/';
      var oldPath = pathPrefix + file.filename;
      var newFileName = file.filename.substring(0, lastPeriodAt) + '.png';
      var newPath = pathPrefix + newFileName;

      var convertSync = function(fromPath, toPath) {
        var sync = true;
        var error = null;

        sharp(fromPath).resize(1000).png().toFile(toPath, function(err, info) {
          if (err) {
            error = err;
            console.log(err);
          } else {
            console.log('Converted from jpeg to png');
            req.files[idx].filename = newFileName;
            sync = false;
          }
        });

        while(sync) { require('deasync').sleep(100); }
        if (error !== null) {
          res.status(500).send("Failed to convert from jpeg to png:", err);
        }
      };

      convertSync(oldPath, newPath);
    }
  });

  // store to the backend
  console.log('Starting write to GridFS');
  req.files.forEach(function(file, idx) {
    names.push(file.filename);
    var writestream = gfs.createWriteStream({
      filename: 'o_' + file.filename
    });
    var writestream_thumbnail = gfs.createWriteStream({
      filename: 't_' + file.filename
    });
    var path = './backend/uploads/' + file.filename;
    var transform_pic = sharp(path);
    transform_pic.metadata()
      .then(function(metadata) {
        return transform_pic.resize(200);
      })
      .then(function() {
        var readableStream = fs.createReadStream(path);
        readableStream.on('end', function() {
          var stream = readableStream
            .pipe(transform_pic)
            .pipe(writestream_thumbnail);
          stream.on('finish', function() {
            res.locals['filenames'] = names;
            done++;
            if (done === req.files.length) { next(); }
          });
        }).on('err', function() {
          res.status(500).send('Error uploading images, please try again. Error:', err);
        }).pipe(writestream);
      });
  });
};

var newScan = function(req, res, next) {
  var onScanReady = function(scanResults) {
    console.log('Scan ready');
    res.locals.filenames.forEach(function(filename, idx) {
      var path = './backend/uploads/' + filename;
      fs.unlinkSync(path);
    });

    User.findOne({
      'username': req.tokenPayload.username
    }, function(err, user) {
      if (err) {
        handleError(err, res)
      };
      var scan = new Scan({
        text: '',
        originals: [],
        thumbnails: [],
        user: user._id
      });
      for (var i = 0; i < results.length; i++) {
        scan.text += results[i].text + ' ';
        scan.originals.push(results[i].original);
        scan.thumbnails.push(results[i].thumbnail);
      }
      scan.save(function(err, scan) {
        if (err) {
          handleError(err, res)
        };
        user.scans.push(scan._id);
        user.save(function(err, user) {
          if (err) {
            handleError(err, res)
          };
          var data = results.slice();
          for (var i = 0; i < data.length; i++) {
            data[i]['createdAt'] = scan.createdAt;
          }
          res.status(200).json(data);
        })
      });
    });
  };

  // scan all, then call onScanReady
  var filenames = res.locals.filenames;
  var startTimes = [];
  var results = [];
  var started = 0;
  var done = 0;
  console.log('Starting Tesseract scan');
  filenames.forEach(function(filename, idx) {
    console.log('started', idx);
    startTimes[idx] = new Date().getTime();
    var path = './backend/uploads/' + filename;
    var tesseractSync = function(path) {
      var sync = true;
      var data = null;
      Tesseract.recognize(path)
      .then(function(result) {
        var timeTaken = new Date().getTime() - startTimes[idx];
        console.log('ended', idx);
        console.log('processing time:', timeTaken);
        data = {
          text: result.text,
          original: 'o_' + filenames[idx],
          thumbnail: 't_' + filenames[idx],
          processingTime: timeTaken
        };
        sync = false;
      });
      while(sync) { require('deasync').sleep(100); }
      return data;
    };

    results.push(tesseractSync(path));
    done++;
    if (done === filenames.length) { onScanReady(results); }
  });
};

var getHistory = function(req, res, next) {
  User.findOne({
      'username': req.tokenPayload.username
    })
    .populate('scans').exec(function(err, user) {
      if (err) {
        handleError(err, res)
      };
      res.status(200).json(user.scans);
    });
};

module.exports = function(app) {
  app.post('/api/scan', auth, upload.array('images'), prepareImages, newScan);
  app.get('/api/history', auth, getHistory);
  app.get('/api/:filename', auth, getFile);
  app.get('/api/delete/:filename', auth, deleteFile);
};