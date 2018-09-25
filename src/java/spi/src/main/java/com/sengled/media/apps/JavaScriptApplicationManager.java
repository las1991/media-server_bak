package com.sengled.media.apps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.event.Event;

/**
 * Created by las on 2017/3/1.
 */
public class JavaScriptApplicationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptApplicationManager.class);
    
    private ScriptEngineManager manager = new ScriptEngineManager();
    private Map<String, JavaScriptApplication> holders = new ConcurrentHashMap<>();


    /**
     * 为脚本引擎分发一个事件
     * 
     * @param event
     */
    public void fireEvent(Event event) {
        for (JavaScriptApplication holder : holders.values()) {
            holder.invoke(event, null);
        }
    }

    
    /**
     * 加载脚本文件
     * 
     * @param name
     * @param file
     * @param runtimeVariables
     * @return
     * @throws IOException
     * @author chenxh
     */
    public JavaScriptApplication loadApplication(String name, File file, Map<String, Object> runtimeVariables) throws IOException {
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        engine.put(ScriptEngine.FILENAME, file);

        // 绑定工具函数
        for (Entry<String, Object> entry : runtimeVariables.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
        }

        // 日志函数
        Logger logger = LoggerFactory.getLogger(getClass().getName() + "." + name);
        engine.put("logger", logger);
        
        // 宿主
        JavaScriptApplication holder = new JavaScriptApplication(name, engine);
        engine.put("holder", holder);
        
        try {
            holder.load(IOUtils.toString(file.toURI()));
        } catch (ScriptException e) {
            throw new IOException("Fail load '" + file.getAbsolutePath() + "' for " + e.getMessage(), e);
        }

        // 保存到内存中
        holders.put(name, holder);
        LOGGER.info("put [{}] {}", name, holder);

        return holder;
    }

    public JavaScriptApplication getApplication(String name) {
        return holders.get(name);
    }

    public JavaScriptApplication removeApplication(String name) {
        return holders.remove(name);
    }

    public List<String> getApplicationNames() {
        return new ArrayList<>(holders.keySet());
    }

}
