package com.sengled.media.bootstrap.route53;

class ProxyRoute53Template implements DomainTemplate {
    private DomainTemplate proxy = new DefaultDomainTemplate();
    
    public void setProxy(DomainTemplate proxy) {
        this.proxy = proxy;
    }
    
    @Override
    public String getDomain(String ip) {
        return proxy.getDomain(ip);
    }
}
