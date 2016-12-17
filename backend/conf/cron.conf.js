var mongoose = require('mongoose');
var Grid = require('gridfs-stream');
Grid.mongo = mongoose.mongo;
var gfs = new Grid(mongoose.connection.db);
var Scan = mongoose.model('Scan');
var User = mongoose.model('User');

var cleanOldOriginals = function() {
  var deleteFile = function(filename) {
    gfs.exist({
      filename: filename
    }, function(err, file) {
      if (err) { console.log('Error deleting file:', err); }
      if (file) {
        gfs.remove({
          filename: filename
        }, function(err) {
          if (err) { console.log('Error deleting file:', err); }
        });
      }
    });
  };

  var cleanScan = function(scan) {
    console.log('Cleaning scan', scan._id);
    for (var i = 0; i < scan.originals.length; i++) {
      deleteFile(scan.originals[i]);
    }
    scan.originals = [];
    scan.save();
  };

  Scan.count({}, function(err, count) {
    if (count > 0) {
      Scan.find({}, function (err, scans) {
        if (err) { return done(err); }
        for (var i = 0; i < scans.length; i++) {
          var scan = scans[i];
          var createdAt = new Date(scan.createdAt);
          var msSinceCreation = Date.now() - scan.createdAt;
          var sevenDays = 1000*60*60*24;
          if (msSinceCreation > sevenDays) {
            cleanScan(scan);
          }
        }
      });
    } else {
      console.log('Nothing to clean up.');
    }
  });
};

var CronJob = require('cron').CronJob;
var job = new CronJob({
  cronTime: '00 00 00 * * *',
  onTick: function() {
    console.log('Running cleanup job...');
    cleanOldOriginals();
  },
  start: true,
  timeZone: 'Europe/Helsinki'
});