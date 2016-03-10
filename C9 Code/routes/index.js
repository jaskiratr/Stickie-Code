var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Stickie' });
});

/* GET Hello World page. */
router.get('/team1', function(req, res) {
    res.render('teamName', { title: 'team1' });
});

/* GET Hello World page. */
router.get('/team2', function(req, res) {
    res.render('teamName', { title: 'team2' });
});

module.exports = router;
