package com.sengled.media.event;

/**
 * Created by las on 2017/3/23.
 */

/**
 * {
 * id:”设备ID”,                                     // [必要字段]设备id
 * token:”23AC148A1D8DC7DB21583C45BADB9DAE”,  // [必要字段]设备token
 * created:”2017-03-21 10:16:01.001”,          // [必要字段]消息发送的时间
 * product:”snap”                                  // [不必要字段],默认snap
 * }
 */
public class UpdateConfigMessage {

    private String id;
    private String token;
    private String created;
    private String product;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "UpdateConfigMessage{" +
                "id='" + id + '\'' +
                ", token='" + token + '\'' +
                ", created='" + created + '\'' +
                ", product='" + product + '\'' +
                '}';
    }
}
