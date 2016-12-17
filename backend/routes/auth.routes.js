var passport = require('passport');
var mongoose = require('mongoose');
var User = mongoose.model('User');

var register = function(req, res) {
  var user = new User();
  user.username = req.body.username;
  user.setPassword(req.body.password);
  user.save(function(err) {
    var token = user.genToken();
    console.log('Registered:', user.username);
    res.status(200).json({'token': token});
  });
};

var login = function(req, res) {
  passport.authenticate('local', function(err, user, info) {
    if (err) {
      res.status(404).json(err);
      return;
    }
    if (user) {
      var token = user.genToken();
      console.log('Authorized login:', user.username);
      res.status(200).json({'token': token});
    } else {
      console.log('Unauthorized login:', info);
      res.status(401).json(info);
    }
  })(req, res);
};

var loginFacebook = function(req, res, next) {
  passport.authenticate('facebook-token', function(err, user, info) {
    if (err) {
      res.status(404).json(err);
      return;
    }
    if (user) {
      var token = user.genToken();
      console.log('Authorized login from Facebook:', user.username);
      res.status(200).json({'token': token});
    } else {
      console.log('Unauthorized login from Facebook:', info);
      res.status(401).json(info);
    }
  })(req, res, next);
};

module.exports = function(app) {
  app.post('/api/register', register);
  app.post('/api/login', login);
  app.post('/api/login/facebook', loginFacebook);
};