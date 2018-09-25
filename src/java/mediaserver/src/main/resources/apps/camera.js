
function setInterval(func, interval) {
  holder.setInterval(new java.lang.Runnable() {
      run: func
  }, interval);
}

importClass(java.util.HashMap)
importClass(com.alibaba.fastjson.JSON)
importClass(com.sengled.media.Sengled)
importPackage(com.sengled.media.event)
importPackage(com.sengled.media.plugin)
importPackage(com.sengled.media.plugin.kinesis)
importClass(com.sengled.media.plugin.config.storage.StorageConfig)

//var sqs_queue = 'chenxh'
var getServiceConfigUrl = "http://" + Sengled.getProperty("SNAP_SERVER_SOCKET_ADDRESS") + "/camera/m3/device/getStorageConfig.json";
logger.info("I will get device config by RESTful API {}", getServiceConfigUrl);

// 算法服务器通过这个 kinesis 流接收视频数据
var algorithmStreamName = Sengled.getProperty("MEDIA_KINESIS_ALGORITHM");
logger.info("I will send I frame to Algorithm Server by kinesis stream [{}]", algorithmStreamName);

// 截图服务器通过这个 kinesis 流接收视频数据
var capturerStreamName = Sengled.getProperty("MEDIA_KINESIS_CAPTURER");
logger.info("I will send I frame to Capturer Server by kinesis stream [{}]", capturerStreamName);

var default_config = Sengled.getProperty("media.js.algorithm.config", "false") === 'true';
var capturerInterval = 120; // 截图时间间隔


// 接收本机系统触发的事件
function onEvent(event) {
  var isTalkback = event.getToken().contains("_");
  if (isTalkback) {
    return;
  }
  
  var token = event.getToken();
  switch (event.getEventType()) {
    case EventType.OFFLINE:
        stopStorage(token); // 下线要通知停止录像
        break;
    case EventType.ONLINE:
    case EventType.DISCONTINUED:
    case EventType.RECORDER_INTERRUPTED:
        stopStorage(token);
        reloadConfig(token); // 重启录像服务
      break;
    default:
        logger.info("{} ignored", event.getEventType());
        break;
  }
}



// 从 web api 中接收配置更新消息
// 在 camera-sqs-consumer 中被调用
function reloadConfig(token) {
  // 包含 '_' 表示是对讲
  if (!token || token.contains("_")) {
      return;
  }
  
  var config = null;
  var url = getServiceConfigUrl + "?token=" + token;
  try {
      // 从 camera 加载配置
      var response = RestTemplate.getForObject(url, HashMap.class);
      if (!response) {
        logger.error("[{}] fail load config, {}", token, response);
        return;
      }
      
      // 存储服务为空的
      if (!response.storage) {
        logger.error("[{}] load config, but response is {}", token, response);
        stopAlgorithm(token);
        stopStorage(token);
        return;
      }
      
      // 通知存储、算法、截图等服务
      fireEvents(token, response.storage);
      return response;
  } catch (err) {
      logger.error("Fail call {}", url, err);
      return "Exception:" + err.message;
  }
}



function fireEvents(token, storageConfig) {
  // 通知启动算法
  try {
    if (storageConfig.enable) {
        startAlgorithm(token, storageConfig);
      } else {
        stopAlgorithm(token); // 停止算法
      } 
  } catch(e) {
    logger.error("Fail start algorithm for {}", e.message, e);
  }
  
  // 通知开启截图
  try {
    var storer = new KinesisFrameStorer(KinesisProducer, capturerStreamName);
    var success = Capturer.start(ServerContext, token, storer, capturerInterval);
  } catch(e) {
    logger.error("Fail start capturer for {}", e.message, e);
  }

  
  // 通知开启存储
  try {
    if (storageConfig.enable) {
      startStorage(token, storageConfig);
    } else {
      stopStorage(token); // 停止录像
    }  
  } catch(e) {
    logger.error("{}", e.message);
  }
}

// 开始录像
function startStorage(token, config) {
  var url = "http://127.0.0.1:8888/storage/startIfAbsent?token=" + token + "&timeZone=" + config.timeZone + "&storageTime=" + (config.fileExpires * 24);
  RestTemplate.getForEntity(url, HashMap.class);
    
  return "none";
}

// 停止录像
function stopStorage(token) {
    var stopStorageUrl = "http://127.0.0.1:8888/storage/stop?token=" + token;
    RestTemplate.getForEntity(stopStorageUrl, HashMap.class);
}

function startAlgorithm(token, storageConfig) {
	  var url = "http://127.0.0.1:8888/algorithm/startIfAbsent?token=" + token;
	  RestTemplate.postForEntity(url, storageConfig, HashMap.class);
	    
	  return "none";
}
function stopAlgorithm(token) {
    var stopStorageUrl = "http://127.0.0.1:8888/algorithm/stop?token=" + token;
    RestTemplate.getForEntity(stopStorageUrl, HashMap.class);
}

// 定时同步系统的配置
logger.info("reload tokens configs interval is {} ms", 1000 * 60 * 10);
setInterval(function () {
  var tokens = ServerContext.getMediaSourceNames();
  
  var it = tokens.iterator();
  while (it.hasNext()) {
      var token = it.next();
      reloadConfig(token);
  }
}, 1000 * 60 * 10);