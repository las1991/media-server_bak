process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

var http = require('https')

var sengled = 'http://snap.cloud.sengled.com:8000/camera/'
var sengleds = 'https://snap.cloud.sengled.com:9000/camera/'


var url = sengleds;

//var localClient;
//var remoteClient;


var arguments = process.argv.splice(2);

var haproxyIpAddr = arguments[1];
var haproxyIpPort = arguments[2];
var listenPort = arguments[0];


function createTcpServer(){
	var net = require('net');
	var server = net.createServer(function(c) {
                var localClient;
                var remoteClient;
		console.log("server connected...");
		c.on('end', function() {
			console.log("server disconnect...");
		});
		localClient = c;
		
		console.log("ip: " + haproxyIpAddr);
		console.log("port: " + haproxyIpPort);

		var ssl = require('tls');   
                //var remoteClient;
                remoteClient = ssl.connect({port:haproxyIpPort, host:haproxyIpAddr}, function(){
                    console.log('remote server connected......');
		    localClient.pipe(remoteClient);
		    remoteClient.pipe(localClient);
                });
		
		localClient.on('error', function(err) {
			console.log('localClient connect error: ' + e.message)
			console.log(e.stack)
		});
		
		remoteClient.on('error', function(err) {
			console.log('remoteClient connect error: ' + e.message)
			console.log(e.stack)
		});
		
	});
	
	if(haproxyIpAddr == null || haproxyIpPort == null || listenPort == null) {
		console.log("Usage example: node app_tcp_server.js 8888 192.168.1.1 1554");
		process.exit();
	}
	server.listen(listenPort, function() {
		console.log("Already bound");
	});
}

createTcpServer()
