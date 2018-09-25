package com.sengled.media.bootstrap.route53;

public class DefaultDomainTemplate implements DomainTemplate {

    @Override
    public String getDomain(String ip) {
        return ip;
    }

}
