package com.algotrader.cache;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Service
public class BackgroundCache {

    @Value("${cache.comfy.level}")
    private long comfyLevel;

    private final Cache<LocalDateTime, Double> cache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    public void save(Double v) {

        cache.put(LocalDateTime.now(), v);
    }

    public OptionalDouble getAverage() {

        return cache.asMap()
                .values()
                .stream()
                .filter(Objects::nonNull)
                .mapToDouble(a -> a)
                .average();
    }

    public long getCount() {

        return cache.asMap()
                .values()
                .stream()
                .filter(Objects::nonNull)
                .mapToDouble(a -> a)
                .count();
    }

    public boolean isComfy() {

        return cache.size() > comfyLevel;
    }
}
