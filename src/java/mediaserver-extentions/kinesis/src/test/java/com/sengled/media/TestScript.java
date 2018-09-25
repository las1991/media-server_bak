package com.sengled.media;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class TestScript {
    public static void main(String[] args) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ScriptException {
        for (int time = 0; time < 6; time++) {
            int times = 1000;
            for (int i = 0; i < time; i++) {
                times *= 10;
            }
            
            // java 反射
            Method addMethod = TestScript.class.getMethod("add", int.class, int.class);
            long startAt = System.currentTimeMillis();
            startAt = System.currentTimeMillis();
            for (int i = 0; i < times; i++) {
                addMethod.invoke(TestScript.class, i, i);
            }
            System.out.println("java add " + times + " times , cost " + (System.currentTimeMillis() - startAt) + "ms");
            
            // 脚本引擎反射
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine jsEngine = manager.getEngineByName("js");
            jsEngine.eval("function add(a, b) {return a + b;}");
            Invocable invocable = (Invocable)jsEngine;
            startAt = System.currentTimeMillis();
            for (int i = 0; i < times; i++) {
                 invocable.invokeFunction("add", i, i);
            }
            System.out.println("js add " + times + " times , cost " + (System.currentTimeMillis() - startAt) + "ms");

            System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<" + jsEngine);
        }
    }
    
    public static final int add(int a, int b) {
        return a + b;
    }
}
