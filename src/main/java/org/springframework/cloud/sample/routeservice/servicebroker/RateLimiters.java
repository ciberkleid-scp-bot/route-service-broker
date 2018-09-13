package org.springframework.cloud.sample.routeservice.servicebroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RateLimiters implements ApplicationContextAware, InitializingBean {
    private final Logger log = LoggerFactory.getLogger(RateLimiters.class);

    public Map<String, RedisRateLimiter> redisRateLimiterMap;

    private ApplicationContext applicationContext;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    public RateLimiters() {
         redisRateLimiterMap = new HashMap<String, RedisRateLimiter>();
    }

    public RedisRateLimiter getLimiter(String instanceId) {
            return redisRateLimiterMap.get(instanceId);
    }

    public void addLimiter(String instanceId, int replenishRate, int burstCapacity) {
        RedisRateLimiter rrl = new RedisRateLimiter(replenishRate, burstCapacity);
        rrl.setApplicationContext(applicationContext);
        redisRateLimiterMap.put(instanceId, rrl);
        log.info("RateLimiters size = {}", redisRateLimiterMap.size());
    }

    public void removeLimiter(String instanceId) {
        redisRateLimiterMap.remove(instanceId);
        log.info("RateLimiters size = {}", redisRateLimiterMap.size());
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        // load map from repo
        serviceInstanceRepository.findAll().forEach(e -> addLimiter(e.getServiceInstanceId(), e.getReplenishRate(), e.getBurstCapacity()));
    }
}
