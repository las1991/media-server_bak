package com.sengled.cloud.authication.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.cloud.authication.controller.LoginController;

@WebFilter(urlPatterns={
        "*.html", 
        "/devops/*",
        "/announcer/*",
        "/refresh", "/refresh/*", 
        "/beans", "/beans.json", 
        "/restart", "/restart.json", 
        // "/health","/health.json", // 监控需要
        // "/metrics", "/metrics/*", // 运维需要
        "/pause", "/pause.json", 
        "/configprops", "/configprops.json",
        "/mappings","/mappings.json",
        "/env", "/env/*",
        "/info", "/info.json",
        "/resume", "/resume.json"
        })
public class AccessFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessFilter.class);
    
    private String loginLocation = LoginController.SIGIN_PAGE;
    
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;
        
        if (isFromPrivateNetwork(request) 
                || hasLogin(request) 
                || loginLocation.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
        } else {
            LOGGER.warn("{} {} forbidden", request.getMethod(), request.getRequestURI());
            response.sendRedirect(loginLocation);
        }
    }

    private boolean isFromPrivateNetwork(HttpServletRequest request) {
        // 嘉兴的两台 media server， 自动放行
        String host = request.getHeader("Host");
        if (null != host) {
            boolean isAllowen = host.contains("101.68.222.220") 
                                || host.contains("101.68.222.221");
            if (isAllowen) {
                return true;
            }
        }
        
        // 内网的机器，自动放行
        String address = request.getRemoteAddr();
        return address.startsWith("10.")  // 10.x.x.x
            || address.startsWith("127.") // 127.0.0.1
            || address.startsWith("172.") // 172.16.x.x---172.31.x.x
            || address.startsWith("192.168") // 192.168.x.x
            ;
        
    }

    private boolean hasLogin(HttpServletRequest request) {
        return null != request.getSession(true).getAttribute(LoginController.SESSION_USER);
    }

    @Override
    public void destroy() {
        
    }
}
