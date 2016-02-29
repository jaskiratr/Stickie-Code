//URL
//https://stickie-jaskiratr.c9users.io

// module dependencies
var http = require("http");
var sio = require("socket.io");
var math = require('mathjs');
// create http server
var server = http.createServer().listen(process.env.PORT, process.env.IP),
// create socket server
io = sio.listen(server);
// set socket.io debugging
io.set('log level', 1);

var jsonfile = require('jsonfile');
var util = require('util');
var fs = require('fs');
var fsPlayback = require('fs');

// var osc = require("osc");
var imgCount = 0;
var rxColor;
var zeroing = false;
var phoneA = "null";
var phoneB = "null";
var displayA = "null";
var displayB = "null";
var displayRX, displayTX, phoneRX, phoneTX;
var playback = false;
var startTime = math.floor(Date.now() / 1000);
var elapsedTime = 0;
var bufferImage = null;
var bufferImagePath = null;
var notePostedFromLog = false;

function avgColor(r, g, b) {
    this.r = r;
    this.g = g;
    this.b = b;
};

io.sockets.on('connection', function(socket) {
    socket.emit('helo', {
        msg: 'Server'
    });

    // Identify the phones & TVs
    socket.on("device_id", function(data) {
        switch (data) {
            case "phoneA":
                phoneA = socket.id;
                console.log("phoneA Connected");
                io.to(phoneA).emit("Hello", "phoneA");
                break;
            case "phoneB":
                phoneB = socket.id;
                console.log("phoneB Connected");
                io.to(phoneA).emit("Hello", "phoneA");
                break;
            case "displayA":
                displayA = socket.id;
                console.log("displayA Connected");
                io.to(displayA).emit("Hello", "displayA");
                break;
            case "displayB":
                displayB = socket.id;
                console.log("displayB Connected");
                io.to(displayB).emit("Hello", "displayB");
                break;
        }
    });
    socket.on("TestColor", function(data) {
        io.sockets.emit('TestColor_noteAdd', data);
        // console.log("gyro data");
        console.log(data);
    });

    socket.on("image", function(data) {
        var phoneTX = socket.id;
        var imgData = data.replace(/^data:image\/\w+;base64,/, "");
        bufferImage = imgData;

        io.to(displayA).emit("bufferImage", imgData);
        io.to(displayB).emit("bufferImage", imgData);

        if (playback == false) {
            //Write to file
            var filename = __dirname + "/images/out_" + imgCount + ".png";
            fs.writeFile(filename, imgData, 'base64', function(err) {
                console.log("error: " + err);
                io.sockets.emit('image_path', "out_" + imgCount + ".png");
                // console.log(filename);
                imgCount++;
                bufferImagePath = filename;
            });
        }

        zeroing = true;
        // console.log("phoneTX "+ phoneTX);
        // console.log("phoneA "+ phoneA);
        if (phoneTX == phoneA) {
            displayRX = displayA;
            console.log(" Request initiated by Phone A");
            console.log("DisplayA" + displayA);
        };
        if (phoneTX == phoneB) {
            displayRX = displayB;
            console.log(" Request initiated by Phone B");
            console.log("DisplayB" + displayB);
        };
        // io.sockets.emit('createGrid', 'origin'); // Laptop 1 only
        console.log("displayRX ----" + displayRX);
        console.log("DisplayA ----" + displayA);
        io.to(displayRX).emit('createGrid', 'origin'); ////
        console.log(" Create GRID on ORIGIN");
    });
    socket.on("gridCreated", function(data) {
        console.log("SERVER: Grid created");
        var displayTX = socket.id;
        if (data == 'stop') {
            zeroing = false;
        };
        if (zeroing == true) {
            if (data == 'true') {
                setTimeout(function() {
                    // Delayed for exposure adjustment.
                    // io.sockets.emit("findColor", "true");
                    if (displayTX == displayA) {
                        phoneRX = phoneA;
                        io.to(phoneRX).emit('findColor', 'findColor');
                    };
                    if (displayTX == displayB) {
                        phoneRX = phoneB;
                        io.to(phoneRX).emit("findColor", "findColor");
                    };
                    // console.log("displayTX ----"+displayTX);
                    // console.log("DisplayA ----"+displayA);
                    // console.log("phoneRX ----"+phoneRX);
                    // console.log("phoneA -----"+phoneA);
                    // socket.to(phoneRX).emit("findColor", "true"); // DispOut only
                }, 200);
            }
        };
    });
    socket.on("color", function(data) {
        console.log("COLOR Recieved from Phone");
        console.log(typeof(data));
        console.log(data);
        
        var phoneTX = socket.id;
        var displayRX = "null";
        rxColor = new avgColor(data.red, data.green, data.blue);
        var pos = calculatePosition();
        console.log("Phone Position " + pos);

        // Send to specific display

        if (phoneTX == phoneA) {
            displayRX = displayA
        }
        if (phoneTX == phoneB) {
            displayRX = displayB
        }
        io.to(displayRX).emit('createGrid', pos);
        // io.sockets.emit('createGrid', pos);
    });
    socket.on("imagePosition", function(imagePosition) {
        var displayTX = socket.id;
        if (displayTX == displayA) {
            io.to(displayB).emit("imagePosition", imagePosition);
        }
        if (displayTX == displayB) {
            io.to(displayA).emit("imagePosition", imagePosition);
        }

        elapsedTime = math.floor(Date.now() / 1000) - startTime;
        // Write to Json
        addToLog(elapsedTime, displayTX, bufferImagePath, imagePosition);
    });

});




setInterval(function() {
    if (playback == true) {
        readFromLog();
    };
}, 1000);

var lastImage = -1;

function readFromLog() {

    // console.log("reading log");
    var playbackLog = require('./recording/playback.json');
    for (var i = 0; i < playbackLog.sequence.length; i++) {
        if ((math.floor(Date.now() / 1000) - startTime) > playbackLog.sequence[i].elapsedTime && lastImage != i && i > lastImage) {
            //load image from ...
            lastImage = i;
            console.log("reading file");
            fsPlayback.readFile(playbackLog.sequence[i].imagePath, function(err, data) {
                // var base64Image = data.toString('base64');
                var base64Image = new Buffer(data, 'base64');
                base64Image = base64Image.toString('base64');;
                base64Image = base64Image.replace(/^data:image\/\w+;base64,/, "");
                server.sockets.socket(displayB).emit("bufferImage", base64Image);
                server.sockets.socket(displayA).emit("bufferImage", base64Image);
            });
            imagePosition = playbackLog.sequence[i].imagePosition;
            setTimeout(function() {
                server.sockets.socket(displayB).emit("imagePosition", imagePosition);
                server.sockets.socket(displayA).emit("imagePosition", imagePosition);
            }, 3000);
            // playback = false;
        }
    }
}

function addToLog(elapsedTime, displayTX, bufferImagePath, imagePosition) {
    console.log("ADDING TO LOG");
    var sender = null;
    if (displayTX == displayA) {
        sender = "displayA"
    }
    if (displayTX == displayB) {
        sender = "displayB"
    }

    //Find current time
    //Posted by
    //Posx
    //PosY
    //imagePath
    var file = __dirname + "/recording/playback.json";
    var playbackLog = require('./recording/playback.json');
    var obj = {
        "count": imgCount,
        "elapsedTime": elapsedTime,
        "sender": sender,
        "imagePath": bufferImagePath,
        "imagePosition": imagePosition
    };
    playbackLog.sequence.push(obj);

    jsonfile.writeFile(file, playbackLog, function(err) {
        if (err) {
            console.error(err)
        }
        // SEQUENCE TO START READING AND POSTING        
    });
}

function calculatePosition() {
    // rxColor = new avgColor (3,10,255); // CHANGE THIS----
    var position;
    var calcColor = "nullColor";
    var delta = 0;
    var minDelta = 10000;
    var parsedJSON = require('./colors.json');
    console.log("parsedJSON.colorList.length" + parsedJSON.colorList.length);
    for (var i = 0; i < parsedJSON.colorList.length; i++) {
        // console.log(parsedJSON.colorList[i]);
        console.log("rxColor" + JSON.stringify(rxColor));
        console.log("rxColor[1]" + rxColor[1]);
        console.log("parsedJSON.colorList[i].r" + parsedJSON.colorList[i].r);
        delta = math.abs(rxColor.r - parsedJSON.colorList[i].r) + math.abs(rxColor.g - parsedJSON.colorList[i].g) + math.abs(rxColor.b - parsedJSON.colorList[i].b);
        console.log("Delta" + delta);
        if (delta < minDelta) {
            calcColor = parsedJSON.colorList[i].color;
            console.log("calcColor" + calcColor);
            minDelta = delta;
            position = i;
        }
    };
    console.log(calcColor);
    return position;
}



// Red (255,29,26)
// Orange (210,130,60)
// Yellow (155,160,20)
// Lime (25, 180,0)
// Green (0,180,0)
// Aqua (0,180,50)
// Cyan (0,180,180)
// Teal (0,150,255)
// Blue (0,120,255)
// Blue Magenta (150,120,250)
// Magenta (230,100,230)
// Red Magenta (250,60,160)
