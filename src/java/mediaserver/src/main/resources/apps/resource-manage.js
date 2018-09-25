importPackage(com.sengled.media.event)
importClass(java.util.HashMap)
importClass(com.alibaba.fastjson.JSON)
importClass(com.sengled.media.Sengled)

// 通知资源管理器
function onEvent(event) {
  var token = event.getToken();
  switch (event.getEventType()) {
    case EventType.ONLINE:
      var url = "http://127.0.0.1:8888/media/events/online?token=" + token;
      RestTemplate.getForObject(url, HashMap.class);
      break;
    case EventType.OFFLINE:
      var url = "http://127.0.0.1:8888/media/events/offline?token=" + token;
      RestTemplate.getForObject(url, HashMap.class);
      
      break;
    default:
      logger.debug("default");
  }
}