// NOTE: partially adapted from
// https://www.sitepoint.com/user-authentication-mean-stack/

var passport = require('passport');
var LocalStrategy = require('passport-local').Strategy;
var FacebookTokenStrategy = require('passport-facebook-token');
var mongoose = require('mongoose');
var User = mongoose.model('User');

passport.use(new LocalStrategy({
    usernameField: 'username'
  },
  function(username, password, done) {
    User.findOne({ username: username }, function (err, user) {
      if (err) {
        return done(err);
      }
      if (!user) {
        return done(null, false, {
          message: 'User not found'
        });
      }
      if (!user.checkPassword(password)) {
        return done(null, false, {
          message: 'Wrong username or password'
        });
      }
      return done(null, user);
    });
  }
));

passport.use(new FacebookTokenStrategy({
  clientID: '379043535778305',
  clientSecret: '1b6323bb9b8584c656395a17b1be410a',
}, function(accessToken, refreshToken, profile, done) {
  User.count({
    username: profile.id,
    facebook: true
  }, function(err, count) {
    if (count > 0) {
      User.findOne({
        username: profile.id,
        facebook: true
      }, function (err, user) {
        if (err) { return done(err); }
        return done(null, user);
      });
    } else {
      var user = new User();
      user.username = profile.id;
      user.facebook = true;
      user.save(function(err) {
        console.log('Registered a new Facebook user:', profile.id);
        return done(err, user);
      });
    }
  });
}));
