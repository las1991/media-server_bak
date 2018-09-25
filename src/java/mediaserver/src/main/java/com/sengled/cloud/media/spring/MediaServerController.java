package com.sengled.cloud.media.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.sengled.media.apps.JavaScriptApplication;
import com.sengled.media.server.rtsp.RtspServerContext;

@RestController
public class MediaServerController {
    private RtspServerContext serverContext;
    public MediaServerController(RtspServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * 调用 media server 内部的 js 函数
     * 
     * @param jsFile
     * @param func
     * @param args
     * @return
     */
    @GetMapping(path={"/scripts/{js-file}.js/{js-func}", "/scripts/{js-file}/{js-func}"})
    @ResponseBody
    public Map<String, Object> invoke(@PathVariable("js-file") String jsFile, @PathVariable("js-func") String func, @RequestParam(required=false, name="args") String args) {
        JavaScriptApplication holder =  serverContext.getApplication(jsFile);
        if (null == holder) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ScriptNotFound");
            response.put("description", "illegal argument, " + jsFile + " NOT existed");
            return response;
        }


        Map<String, Object> response = new HashMap<>();
        response.put("status", "fail");
        Future<Object> result = holder.invokeFunctionString(func, StringUtils.split(args, ","));
        try {
            Object str = result.get(6, TimeUnit.SECONDS);
            

            response.put("status", "ok");
            response.put("data", str);
            return response;
        } catch (InterruptedException e) {
            response.put("description", e.getClass().getSimpleName() + ":" + e.getMessage());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            response.put("description", cause.getClass().getSimpleName() + ":" + cause.getMessage());
        } catch (TimeoutException e) {
            response.put("description", e.getClass().getSimpleName() + ":" + e.getMessage());
        } 
        
        return response;
    }
}
