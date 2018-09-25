function setInterval(func, interval) {
  holder.setInterval(new java.lang.Runnable() {
      run: func
  }, interval);
}

importClass(java.util.HashMap)
importClass(com.sengled.media.Sengled)
importPackage(com.sengled.media.plugin.sqs)
importPackage(com.sengled.media.plugin)

var sqs_queue = Sengled.getProperty("MEDIA_SQS_DEVICE_SETUP");
logger.info("I will receive SQS message from {}", sqs_queue);

function reloadConfig(token) {
  if (token) {
    var map = RestTemplate.getForObject("http://127.0.0.1:8888/media/location?token=" + token, HashMap.class);
    
    if ("ok" == map.status) {
      var host = map.location;
      RestTemplate.getForObject("http://" + host + ":8888/scripts/camera/reloadConfig?args=" + token, HashMap.class);
    } else {
      logger.info("{} NOT online", token);
    }
  } else {
    logger.warn("illegal message: {}", message);
  }
}

//从 SQS 接收配置更新消息
function startWatchSQS() {
  SqsMessageConsumers.getInstance().consume(SQSTemplate, sqs_queue, new JsonMessageHandler(){
    handle:function(json){
      var token = json.get("token")
      reloadConfig(token);
    }
  });
  
  logger.info("consume SQS [{}]", sqs_queue);
}

//接收本机系统触发的事件
function onEvent(event) {
}


//开始监听 SQS 消息
startWatchSQS();
