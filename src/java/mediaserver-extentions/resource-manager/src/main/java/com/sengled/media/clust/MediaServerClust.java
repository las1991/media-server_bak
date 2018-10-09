package com.sengled.media.clust;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import com.sengled.media.bootstrap.osmonitor.OSMonitor;
import com.sengled.media.clust.server.MediaResourceDao;
import com.sengled.media.clust.server.MediaServerDao;
import com.sengled.media.clust.server.MediaServerMetadata;
import com.sengled.media.clust.server.MediaServerRuntime;

public class MediaServerClust implements InitializingBean {
    /**
     * 每个 CPU  最多支持 240 个设备
     */
    private static final int MAX_SCORE_PER_CPU = 240;

    private static Logger LOGGER = LoggerFactory.getLogger(MediaServerClust.class);

    private final MediaServerMetadata local;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    private MediaServerDao mediaServerDao;
    @Autowired
    private MediaResourceDao mediaResourceDao;

    private String resourceKeys;
    private String name = "media";

    private int AVERAGE_CPULOAD = 60;
    private int MAX_CPULOAD = 85;
    private int MAX_SCORE = Runtime.getRuntime().availableProcessors() * MAX_SCORE_PER_CPU;

    public MediaServerClust(StringRedisTemplate redisTempalte, MediaServerMetadata local, String name) {
        this.local = local;
        this.redisTemplate = redisTempalte;

        this.name = name;
        this.resourceKeys = "resource:" + name + ":v3:~keys";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.warn("avaliable MeidaServer MAX_CUPLoad < {}%", MAX_CPULOAD);
        LOGGER.warn("avaliable MeidaServer AVERAGE_CUPLoad < {}%", AVERAGE_CPULOAD);
        LOGGER.warn("avaliable MeidaServer MAX_SCORE < {}%", MAX_SCORE);
    }

    public MediaServerMetadata local() {
        return local;
    }

    /**
     * 查询设备所在服务器
     *
     * @param token
     * @return
     * @author chenxh
     */
    public MediaServerMetadata getLocation(String token) {
        String serverId = mediaResourceDao.getDeviceLocation(token);

        return null != serverId ? mediaServerDao.getMetadata(serverId) : null;
    }


    public void updateLocation(String token) {
        mediaResourceDao.setDeviceLocation(token, local().getId());
        LOGGER.info("set location {}={}", token, local().getId());
    }

    public void removeLocation(String token) {
        MediaServerMetadata location = getLocation(token);
        if (null != location && StringUtils.equals(location.getId(), local().getId())) {
            mediaResourceDao.removeDevice(token, local().getId());
        }
    }

    public void registLocal(int connections) {
        MediaServerRuntime runtime = new MediaServerRuntime();
        runtime.setDeviceNum(connections);
        runtime.setMemory(OSMonitor.getInstance().getTotalMemory() >> 20);
        runtime.setCpuLoad(OSMonitor.getInstance().getSystemCpuLoad());
        runtime.setCpuIdle(OSMonitor.getInstance().getSystemCpuIdle());

        String serverId = local().getId();
        mediaServerDao.setMetadata(serverId, local());
        mediaServerDao.setRuntime(serverId, runtime);

        // 注册
        redisTemplate.execute(new RedisCallback<Void>() {
            @Override
            public Void doInRedis(RedisConnection connection) throws DataAccessException {
                Boolean setScore =
                        connection.zAdd(resourceKeys.getBytes(), runtime.getDeviceNum(), serverId.getBytes());
                LOGGER.debug("{}, set {} = {}, result = {}", resourceKeys, serverId, runtime.getDeviceNum(), setScore);
                return null;
            }
        });
    }

    /**
     * @author chenxh
     */
    public void unregistLocal() {
        String serverId = local().getId();
        removeServerInstance(serverId);
    }


    public MediaServerMetadata allocInstance(String token) {
        // 灯已经上线了，则直接访问所有服务器的地址
        MediaServerMetadata onlineMetadata = getLocation(token);
        if (null != onlineMetadata) {
            LOGGER.debug("[{}] is online, use existed server {}", token, onlineMetadata.getId());
            return onlineMetadata;
        }

        // 找到上次分配的机器, 如果负载不高就直接返回
        final String locationKey = "resource:" + getName() + ":allocator:location:" + token;
        String allocatedServerId =
                redisTemplate.execute(new RedisCallback<String>() {
                    @Override
                    public String doInRedis(RedisConnection connection) throws DataAccessException {
                        byte[] location = connection.get(locationKey.getBytes());
                        return null != location ? new String(location) : null;
                    }
                });
        MediaServerRuntime runtime = null != allocatedServerId
                ? mediaServerDao.getRunTime(allocatedServerId)
                : null;
        if (null != runtime && runtime.getCpuLoad() < AVERAGE_CPULOAD) {
            LOGGER.debug("[{}] use allocated server {}.", token, allocatedServerId);
            return mediaServerDao.getMetadata(allocatedServerId);
        }

        // 从现有的 media-server 实例列表中挑一个负载小的
        for (String serverId : getServerInstanceIds(5)) {
            runtime = mediaServerDao.getRunTime(serverId);
            if (null == runtime) {
                removeServerInstance(serverId);
                continue; // 实例不存在了
            }

            if (runtime.getCpuLoad() > MAX_CPULOAD) {
                continue; // 负载太高了
            }

            MediaServerMetadata metadata = mediaServerDao.getMetadata(serverId);
            if (null == metadata) {
                removeServerInstance(serverId);
                continue; // 实例不存在了
            }

            // 标记下 cpu
            redisTemplate.execute(new RedisCallback<Void>() {
                @Override
                public Void doInRedis(RedisConnection connection) throws DataAccessException {
                    // 标记 token， 下次还分到这个机器上
                    connection.set(locationKey.getBytes(), serverId.getBytes(), Expiration.seconds(23), SetOption.UPSERT);
                    LOGGER.info("{} = {}", locationKey, serverId);

                    // 更新权重
                    Double score =
                            connection.zIncrBy(resourceKeys.getBytes(), 1, serverId.getBytes());
                    LOGGER.info("{}, set {} = {}", resourceKeys, serverId, score);

                    return null;
                }
            });

            return metadata;
        }

        LOGGER.error("[{}] can't find any avaliable {} server instance", token, getName());
        return null;
    }

    private List<String> getServerInstanceIds(int limit) {
        List<String> instances =
                redisTemplate.execute(new RedisCallback<List<String>>() {
                    @Override
                    public List<String> doInRedis(RedisConnection connection) throws DataAccessException {
                        Set<byte[]> serverIdList =
                                connection.zRangeByScore(resourceKeys.getBytes(), Range.range().lte(MAX_SCORE), Limit.limit().count(limit));

                        if (null != serverIdList) {
                            return serverIdList.stream().map(Functions.bytes2Str()).collect(Collectors.toList());
                        }

                        return Collections.emptyList();
                    }
                });
        return instances;
    }

    public List<String> getServerInstanceIds() {
        List<String> instances =
                redisTemplate.execute(new RedisCallback<List<String>>() {
                    @Override
                    public List<String> doInRedis(RedisConnection connection) throws DataAccessException {
                        Set<byte[]> serverIdList =
                                connection.zRangeByScore(resourceKeys.getBytes(), Range.unbounded());

                        if (null != serverIdList) {
                            return serverIdList.stream().map(Functions.bytes2Str()).collect(Collectors.toList());
                        }

                        return Collections.emptyList();
                    }
                });
        return instances;
    }

    private void removeServerInstance(String serverId) {
        redisTemplate.execute(new RedisCallback<Void>() {
            @Override
            public Void doInRedis(RedisConnection connection) throws DataAccessException {
                connection.zRem(resourceKeys.getBytes(), serverId.getBytes());
                LOGGER.info("{} remove from {}", serverId, resourceKeys);
                return null;
            }
        });

        // 删除
        LOGGER.info("{} delete runtime", serverId);
        mediaServerDao.deleteRunTime(serverId);

        LOGGER.info("{} delete metadata", serverId);
        mediaServerDao.deleteMetadata(serverId);
    }

    public double getScore(String serverId) {
        Double score =
                redisTemplate.execute(new RedisCallback<Double>() {
                    @Override
                    public Double doInRedis(RedisConnection connection) throws DataAccessException {
                        return connection.zScore(resourceKeys.getBytes(), serverId.getBytes());
                    }
                });

        return null != score ? score : -1.0;
    }

    public String getName() {
        return name;
    }

    public MediaServerMetadata getMetadata(String serverId) {
        return mediaServerDao.getMetadata(serverId);
    }

    public MediaServerRuntime getRuntime(String serverId) {
        return mediaServerDao.getRunTime(serverId);
    }

}
