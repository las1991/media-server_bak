package com.sengled.cloud.authication;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * 用户信息
 * 
 * @author chenxh
 */
public class UserInfo {
    private List<String> modelPrefixes = new  ArrayList<>();
    
    private String password;
    private String name;
    private String id;
    private String email;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getPassword() {
        return password;
    }
    public boolean hasRight(String uri) {
        for (String model : modelPrefixes) {
            if (StringUtils.startsWith(uri, model)) {
                return true;
            }
        }
        return false;
    }
    public void addRight(String prefix) {
        modelPrefixes.add(prefix);
    }
}
