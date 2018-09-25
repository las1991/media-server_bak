package com.sengled.media.storage;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.sengled.media.storage.metrics.custom.ServicesMetrics;

@Component
public class RecordCounter implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordCounter.class);

    private final static String METRICS_NAME = "storage";

    @Autowired
    private MetricRegistry metricRegistry;
    
    @Autowired
    private ServicesMetrics servicesMetrics;
    

    private AtomicLong notUploadFileNum = new AtomicLong();
    private AtomicLong closeStorageNum = new AtomicLong();

    private AtomicLong s3FailureCount = new AtomicLong();
    private AtomicLong s3SuccessfulCount = new AtomicLong();
    // private AtomicLong dynamodbFailureCount = new AtomicLong();
    private AtomicLong sqsFailureCount = new AtomicLong();
    private AtomicLong sqsSuccessfulCount = new AtomicLong();



    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Initializing...");
        try {
            initialize();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private void initialize() {

        metricRegistry.register(MetricRegistry.name(METRICS_NAME, "notUploadFileNum"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return notUploadFileNum.getAndSet(0);
                    }
                });

        metricRegistry.register(MetricRegistry.name(METRICS_NAME, "closeStorageNum"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return closeStorageNum.getAndSet(0);
                    }
                });


        metricRegistry.register(MetricRegistry.name(METRICS_NAME, "s3FailureCount"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return s3FailureCount.getAndSet(0);
                    }
                });
        metricRegistry.register(MetricRegistry.name(METRICS_NAME, "sqsFailureCount"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return sqsFailureCount.getAndSet(0);
                    }
                });
        metricRegistry.register(MetricRegistry.name(METRICS_NAME, "s3SuccessfulCount"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return s3SuccessfulCount.getAndSet(0);
                    }
                });
        metricRegistry.register(MetricRegistry.name(METRICS_NAME, "sqsSuccessfulCount"),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return sqsSuccessfulCount.getAndSet(0);
                    }

                });
    }

    public long setAndGetNotUploadFileNum(long delta) {
        return notUploadFileNum.getAndSet(delta);
    }

    public long addAndGetSqsSuccessfulCount(long delta) {
        return sqsSuccessfulCount.addAndGet(delta);
    }

    public long addAndGetS3SuccessfulCount(long delta) {
        return s3SuccessfulCount.addAndGet(delta);
    }

    public long addAndGetCloseStorageNum(long delta) {
        return closeStorageNum.addAndGet(delta);
    }
    
    public void markDynamodbFail(long delta){
        servicesMetrics.mark(ServicesMetrics.DYNAMODB_FAILURE, delta);
    }
    
    public void markDynamodbSuccess(long delta){
        servicesMetrics.mark(ServicesMetrics.DYNAMODB_SUCCESS, delta);
    }

}
