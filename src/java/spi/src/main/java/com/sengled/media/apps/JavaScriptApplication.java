package com.sengled.media.apps;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.eventbus.Subscribe;
import com.sengled.media.event.Event;
import com.sengled.media.event.EventBusListener;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author chenxh, cxh
 */
public class JavaScriptApplication implements EventBusListener<Event> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptApplication.class);

    private final String name;
    private final ScriptEngine engine;
    private final ScheduledExecutorService singleExecutor;
    
    public JavaScriptApplication(String name, ScriptEngine engine) {
        this.name = name;
        this.engine = engine;
        this.singleExecutor = Executors.newScheduledThreadPool(2 + Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("ScriptEngine-" + name, true));

    }
    
    @Subscribe
    public void onEvent(Event event) {
        invoke(event, Collections.emptyMap());
    }
    
    public Future<Object> invoke(final Event event, final Map<String, Object> args) {
        
        final Invocable invocable = (Invocable) engine;

        Future<Object> future = singleExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    return invocable.invokeFunction("onEvent", event, args);
                } catch (Exception e) {
                    LOGGER.error("{} Fail invoke {} for {}.", name, event, e.getMessage(), e);
                }
                return null;
            }
        });
        return future;
    }
    
    public Future<Object> invokeFunctionString(String function, Object[] args) {
        final Invocable invocable = (Invocable) engine;

        Future<Object> future = singleExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object result = invocable.invokeFunction(function, args);
                return result;
            }
        });
        return future;
    }


    public void load(String script) throws ScriptException {
        // Nashorn 引擎默认不支持 importClass， 需要依赖 mozilla_compat.js
        if (engine.getClass().getSimpleName().equals("NashornScriptEngine")) {
            engine.eval("load(\"nashorn:mozilla_compat.js\")");
        }
        
        // 加载业务脚本
        engine.eval(script);
    }

    /**
     * 给脚本调用的，定时任务
     * 
     * @param r
     * @param interval
     * @author chenxh
     */
    public void setInterval(Runnable r, int interval) {
        singleExecutor.scheduleWithFixedDelay(r, interval, interval, TimeUnit.MILLISECONDS);
    }

    public String getName() {
        return name;
    }
}
