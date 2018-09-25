package com.sengled.media.algorithm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.sengled.media.algorithm.webapp.AlgorithmController;

/**
 * Created by las on 2017/3/16.
 * Modified by chenxh on 2018/5/3
 */
@Configuration
public class AlgorithmConfiguration {

    @Bean
    public AlgorithmController getAlgorithmController() {
        return new AlgorithmController();
    }

}
