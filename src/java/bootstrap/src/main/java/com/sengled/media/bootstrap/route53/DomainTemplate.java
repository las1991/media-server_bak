package com.sengled.media.bootstrap.route53;

public interface DomainTemplate {

    /**
     * 先通过 {@link #getDomainString(String)} 得到域名地址， <br/>
     * 
     * 通过 InetAddress.getByName(domain) 对域名解析，如果解析得到<br/>
     * 的 hostAddress 与 ip 相同，则认为域名有效，返回域名；<br/>
     * 否则返回 IP;
     * 
     * @param ip
     * @return ip if can't find host
     */
    String getDomain(String ip);

}