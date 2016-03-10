var express = require('express');
var app = express();
var server = require('http').Server(app);
var io = require('socket.io')(server);

// var port = process.env.PORT;
var port = 8081;
app.use(express.static('public'));

server.listen(port,process.env.IP, function () {
    console.log('Updated : Server listening at port '+ process.env.IP +" "+ port);
});

console.log("process.env.PORT "+process.env.PORT);

var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var math = require('mathjs');

var routes = require('./routes/index');
var users = require('./routes/users');

  
io.set('log level', 1);
io.sockets.on('connection', function(socket) {
    socket.emit('helo', {
        msg: 'Server'
    });
    console.log("incoming...");
});

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

// uncomment after placing your favicon in /public
//app.use(favicon(path.join(__dirname, 'public', 'favicon.ico')));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.use('/', routes);
app.use('/users', users);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});

// error handlers
// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: err
    });
  });
}

// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: {}
  });
});


module.exports = app;
