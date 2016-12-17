var mongoose = require('mongoose');

mongoose.Promise = global.Promise; // fixes deprecated warning
// mongoose.connect('mongodb://localhost/mcc_local_db');
mongoose.connect('mongodb://mongo-1,mongo-2,mongo-3/mcc_local_db');


var scanSchema = require('./scan.schema');
var userSchema = require('./user.schema');

mongoose.model('Scan', scanSchema);
mongoose.model('User', userSchema);