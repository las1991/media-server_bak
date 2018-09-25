package com.sengled.media.resourcemanager.webapp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sengled.media.resourcemanager.webapp.apiv1.ServerInfo;

public class ResourceInfoResponse {
    private String info;
    private String messageCode;
    private List<ResourceInfo> resource_info = new ArrayList<>();

    @SafeVarargs
    public static ResourceInfoResponse ok(Iterator<ResourceInfo>... itrs) {
        ResourceInfoResponse response = new ResourceInfoResponse();
        response.info = "successful";
        response.messageCode = "200";

        for (int i = 0; i < itrs.length; i++) {
            Iterator<ResourceInfo> items = itrs[i];
            while (items.hasNext()) {
                response.resource_info.add(items.next());
            }
        }

        return response;
    }

    public static final class ResourceInfo {
        private int max_conn = -1;
        private int cur_conn;
        private String resource_addr;
        private int resource_port;
        private String resource_type;
        private List<String> tokens;

        public ResourceInfo(String serverType, ServerInfo t, List<String> tokens) {
            this.cur_conn = t.getConnections();
            this.resource_addr = t.getHost();
            this.resource_port = t.getPort();
            this.resource_type = serverType;

            this.tokens = tokens;
        }

        public int getCur_conn() {
            return cur_conn;
        }

        public int getMax_conn() {
            return max_conn;
        }

        public String getResource_addr() {
            return resource_addr;
        }

        public int getResource_port() {
            return resource_port;
        }

        public String getResource_type() {
            return resource_type;
        }

        public List<String> getTokens() {
            return tokens;
        }
    }

    public String getInfo() {
        return info;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public List<ResourceInfo> getResource_info() {
        return resource_info;
    }
}
