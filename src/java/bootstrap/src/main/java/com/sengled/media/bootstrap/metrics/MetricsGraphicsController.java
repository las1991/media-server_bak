package com.sengled.media.bootstrap.metrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.Metric;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.net.HttpHeaders;
import com.sengled.media.bootstrap.osmonitor.OSMonitor;

/**
 * 用来生成统计图
 *
 * @author chenxh
 */
@Controller
public class MetricsGraphicsController implements InitializingBean, MetricsGraphics {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsGraphicsController.class);


    @Autowired(required=false)
    KinesisProducer algorithmKinesisProducer;

    @Autowired
    private MetricRegistry metricRegistry;
    private final ConcurrentHashMap<String, List<Graphics>> graphicsList = new ConcurrentHashMap<String, List<Graphics>>();

    @Override
    public void afterPropertiesSet() throws Exception {


        final OSMonitor monitor = OSMonitor.getInstance();

        // id, CPU 空闲
        final String osMetrics = "os";
        metricRegistry.register(MetricRegistry.name(osMetrics, "cpuIdle"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                int idle = monitor.getSystemCpuIdle();

                return idle;
            }
        });

        // us, 用户 CPU 使用率
        metricRegistry.register(MetricRegistry.name(osMetrics, "cpuLoad"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                int processCpuLoad = monitor.getProcessCpuLoad();
                return processCpuLoad;
            }
        });

        // heap memory
        metricRegistry.register(MetricRegistry.name(osMetrics, "heapMemory"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                long heapMemoryUsed = monitor.getHeapMemoryUsed();
                return heapMemoryUsed;
            }
        });

        // non head memory
        metricRegistry.register(MetricRegistry.name(osMetrics, "nonHeapMemory"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                final Long nonHeapMemoryUsed = monitor.getNonHeapMemoryUsed();
                return nonHeapMemoryUsed;
            }
        });

        String kinesisMetrics = "kinesis";

        metricRegistry.register(MetricRegistry.name(kinesisMetrics, "UserRecordsDataPut"), new MetricSet() {
            private final ConcurrentMap<String, com.codahale.metrics.Metric> metrics = new ConcurrentHashMap<>();
            private List<Metric> metricList;
            private long last = System.currentTimeMillis();

            @Override
            public Map<String, com.codahale.metrics.Metric> getMetrics() {
                metrics.putIfAbsent("sum", new Gauge<Double>() {
                    @Override
                    public Double getValue() {
                        List<Metric> tmp = getMetric();
                        double total = 0;
                        if (tmp != null) {
                            for (Metric m : tmp) {
                                if (m.getDimensions().size() == 0) {
                                    total += m.getSum() / m.getDuration();
                                }
                            }
                        }
                        return total;
                    }
                });

                metrics.putIfAbsent("sumMap", new Gauge<Map<String, Double>>() {
                    @Override
                    public Map<String, Double> getValue() {
                        Map<String, Double> map = new HashMap<>();
                        List<Metric> tmp = getMetric();
                        if (tmp != null) {
                            for (Metric m : tmp) {
                                if (m.getDimensions().size() == 0) {
                                    map.put("sum", (map.get("sum") == null ? 0 : map.get("sum")) + m.getSum() / m.getDuration());
                                } else if (m.getDimensions().size() == 1) {
                                    map.put(m.getDimensions().get("StreamName"), m.getSum() / m.getDuration());
                                }
                            }
                        }
                        return map;
                    }
                });

                metrics.putIfAbsent("countMap", new Gauge<Map<String, Double>>() {
                    @Override
                    public Map<String, Double> getValue() {
                        Map<String, Double> map = new HashMap<>();
                        List<Metric> tmp = getMetric();
                        if (tmp != null) {
                            for (Metric m : tmp) {
                                if (m.getDimensions().size() == 0) {
                                    map.put("sum", (map.get("sum") == null ? 0 : map.get("sum")) + m.getSampleCount() / m.getDuration());
                                } else if (m.getDimensions().size() == 1) {
                                    map.put(m.getDimensions().get("StreamName"), m.getSampleCount() / m.getDuration());
                                }
                            }
                        }
                        return map;
                    }
                });
                return metrics;
            }

            public List<Metric> getMetric() {
                if ((System.currentTimeMillis() - last) >= 25 * 1000) {
                    try {
                        metricList = new ArrayList<>();
                        if (null != algorithmKinesisProducer) {
                            metricList.addAll(algorithmKinesisProducer.getMetrics("UserRecordsDataPut", 25));
                        }
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    }
                }
                return metricList;
            }
        });

        GraphicsReporter reporter = GraphicsReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.SECONDS)
                .build(this);
        reporter.start(60, TimeUnit.SECONDS);
        try {
            reporter.report();
        } catch(Exception ex) {
            LOGGER.error("MetricsGraphics report failed: {}", ex.getMessage(), ex);
        }
    }

    private Metric getMetric() {

        return null;
    }

    @Override
    public Graphics getOrCreateGraphics(String name, String type, String colTemplates) {
        List<Graphics> tables = graphicsList.get(type);
        if (null == tables) {
            tables = new ArrayList<Graphics>();
            graphicsList.put(type, tables);
        }

        for (Graphics table : tables) {
            if (StringUtils.equals(name, table.getName())) {
                return table;
            }
        }

        Graphics table = null;
        if (null != colTemplates) {
            table = new Graphics(name, colTemplates);
            tables.add(table);

            LOGGER.info("add {} griphics, name = {}, columns = {}", type, name, colTemplates);

        }
        return table;
    }

    @GetMapping(path = "/graphics/{type}")
    public ResponseEntity<?> getGraphics(
            @PathVariable(value = "type") String type,
            @RequestParam(name = "name", required = true) String name,
            @RequestParam(name = "column", required = false, defaultValue = "value") String column) throws IOException {
        List<Graphics> graphics = graphicsList.get(type.toUpperCase());
        if (null == graphics) {
            return ResponseEntity.notFound().build();
        }

        for (Graphics item : graphics) {
            if (item.getName().equals(name)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                boolean foundColumn = item.ouput(out, column);

                if (foundColumn) {
                    return ResponseEntity.ok().header(HttpHeaders.CONNECTION, "Close").body(out.toString("UTF-8"));
                }
            }
        }

        return ResponseEntity.notFound().build();
    }
}
