package com.sengled.media.bootstrap;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

public class AmazonAwsConfig {
    private final AWSCredentialsProvider provider;
    private final String awsRegion;
    
    public AmazonAwsConfig(String awsRegion) {
        this.provider = DefaultAWSCredentialsProviderChain.getInstance();
        this.awsRegion = awsRegion;
    }

    public AWSCredentialsProvider getCredentialsProvider() {
        return provider;
    }

    public String getRegion() {
        return awsRegion;
    }

}
