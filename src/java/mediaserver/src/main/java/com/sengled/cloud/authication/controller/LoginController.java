package com.sengled.cloud.authication.controller;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.security.Credential.MD5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sengled.cloud.authication.UserInfo;

@Controller
public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    
    public static final String SESSION_USER = "user";
    public static final String SIGIN_PAGE = "/signin.html";
    
    @Value("${server.admin.email}")
    private String email;
    
    @Value("${server.admin.name}")
    private String name;

    @Value("${server.admin.password}")
    private String pass;

    
    @RequestMapping(path="/login")
    public ModelAndView login(@RequestParam("email") String email, @RequestParam("password") String password, HttpSession session) {
        UserInfo user = getUserByEmail(email);
        if (null == user) {
            ModelAndView view = new ModelAndView("redirect:" + SIGIN_PAGE + "?email=" + email);
            return view;
        }

        String pass = user.getPassword();
        String input = digest(password);
        if (!StringUtils.equalsIgnoreCase(MD5.__TYPE + pass, input)) {
            LOGGER.warn("illegal password: <{}, {}>", email, input);
            
            ModelAndView sign = new ModelAndView("redirect:/signin.html?email=" + email);
            sign.setStatus(HttpStatus.FORBIDDEN);
            return sign;
        }
        
        
        session.setAttribute(SESSION_USER, user);
        return new ModelAndView("redirect:/");
    }

    static String digest(String password) {
        return MD5.digest(password);
    }
    
    private UserInfo getUserByEmail(String loginUser) {
        if (StringUtils.equals(loginUser, email)) {
            UserInfo user = new UserInfo();
            user.setEmail(email);
            user.setName(name);
            user.setPassword(pass);
            
            return user;
        }
        
        return null;
    }

    @RequestMapping(path="/logout")
    public ModelAndView logout(HttpSession session) {
        session.removeAttribute(SESSION_USER);
        return new ModelAndView("redirect:/");
    }
    
    @GetMapping(path="/getUserInfo")
    @ResponseBody
    public Map<String, Object> getUser(@RequestParam("field") String field, HttpSession session) {
        UserInfo user = (UserInfo) session.getAttribute(SESSION_USER);
        
        if (null != user) {
            switch (field) {
            case "name":
                return Collections.singletonMap(field, user.getName());
            case "email":
                return Collections.singletonMap(field, user.getEmail());
            case "id":
                return Collections.singletonMap(field, user.getId());
            default:
                break;
            }
        }

        return Collections.singletonMap(field, null);
    }
 
}
