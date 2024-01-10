package vip.yeee.app.chatgpt.client.domain.redis;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Maps;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vip.yeee.memo.common.appauth.client.model.ApiAuthedUser;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/4/1 17:57
 */
@Component
public class ChatRedisRepository {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final Map<String, String> userOpenIdMap = Maps.newConcurrentMap();
    public Integer getULimitCountCache(String uid) {
        String uLimitKey = RedisKeys.CHATGPT_U_DAY_LIMIT;
        return Integer.parseInt(Optional.ofNullable(stringRedisTemplate.opsForHash().get(uLimitKey, uid)).orElse("0").toString());
    }

    public void incrULimitCountCache(String uKey) {
        incrULimitCountCache(uKey, 1L);
    }

    public void incrULimitCountCache(String uKey, long incr) {
        String uLimitKey = RedisKeys.CHATGPT_U_DAY_LIMIT;
        boolean has = false;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(uLimitKey))) {
            has = true;
        }
        stringRedisTemplate.opsForHash().increment(uLimitKey, uKey, incr);
        if (!has) {
            stringRedisTemplate.expire(uLimitKey, DateUtil.between(new Date(), DateUtil.endOfDay(new Date()), DateUnit.SECOND), TimeUnit.SECONDS);
        }
        incrUserUsedCount(uKey);
    }

    public void reSetULimitCountCache(String uKey, String count) {
        String uLimitKey = RedisKeys.CHATGPT_U_DAY_LIMIT;
        boolean has = false;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(uLimitKey))) {
            has = true;
        }
        stringRedisTemplate.opsForHash().put(uLimitKey, uKey, count);
        if (!has) {
            stringRedisTemplate.expire(uLimitKey, DateUtil.between(new Date(), DateUtil.endOfDay(new Date()), DateUnit.SECOND), TimeUnit.SECONDS);
        }
        incrUserUsedCount(uKey);
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public Boolean getULimitExclude(String uid, String limitUserKey) {
        String key = RedisKeys.CHATGPT_ULEXCLUDE;
        return "1".equals(stringRedisTemplate.opsForValue().get(key + uid)) || "1".equals(stringRedisTemplate.opsForValue().get(key + limitUserKey));
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public String getApiToken() {
        String key = RedisKeys.CHATGPT_APITOKEN;
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public String getApiHost() {
        String key = RedisKeys.CHATGPT_APIHOST;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(val)) {
            return val;
        }
        return null;
    }

    public void saveUserOpenIdCache(String uid, String openId) {
        userOpenIdMap.put(uid, openId);
    }

    public void incrUserUsedCount(String uKey) {
        stringRedisTemplate.opsForHash().increment(RedisKeys.CHATGPT_USER_DETAIL + uKey, "usedCount", 1L);
    }

    public void recordUserDetail(String uid, String attr, String val) {
        stringRedisTemplate.opsForHash().put(RedisKeys.CHATGPT_USER_DETAIL + uid, attr, val);
    }

    public String getUserKey(String uid) {
        String value = this.getUserOpenIdCache(uid);
        return StrUtil.isNotBlank(value) ? value : uid;
    }

    public Integer getUserSurplus(ApiAuthedUser authedUser) {
        return Math.max(!this.getULimitExclude(authedUser.getUid(), authedUser.getOpenid())
                ? this.getULimitCount() - this.getULimitCountCache(authedUser.getOpenid()) : 1000, 0);
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public Integer getULimitCount() {
        String key = RedisKeys.CHATGPT_LIMITCOUNT;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(val)) {
            return Integer.parseInt(val);
        }
        return null;
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public Map<String, String> getPresetAnswers() {
        String key = RedisKeys.CHATGPT_PRESET_ANSWERS;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        Map<String, String> res = Maps.newHashMap();
        entries.forEach((k, v) -> res.put(k.toString(), v.toString()));
        return res;
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public String getReplaceRegex() {
        String key = RedisKeys.CHATGPT_REPLACE_REGEX;
        return StrUtil.emptyToDefault(stringRedisTemplate.opsForValue().get(key), "[- +.。^:：,，？?\\n\\r]");
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public List<String> getProhibitKeyword() {
        String key = RedisKeys.CHATGPT_PROHIBIT_KEYWORD;
        return Optional
                .ofNullable(stringRedisTemplate.opsForList().range(key, 0, 200))
                .orElse(Collections.emptyList());
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public Integer getPreRecordCount() {
        String key = RedisKeys.CHATGPT_PRERECORD_COUNT;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(val)) {
            return Integer.parseInt(val);
        }
        return 2;
    }

    private String getUserOpenIdCache(String uid) {
        return userOpenIdMap.get(uid);
    }

    interface RedisKeys {
        String CHATGPT_PREFIX = "YEEE:CHATGPT:";
        String CHATGPT_CONFIG_PREFIX = CHATGPT_PREFIX + "CONFIG:";
        String CHATGPT_U_DAY_LIMIT = CHATGPT_PREFIX + "ULIMIT";
        String CHATGPT_ULEXCLUDE = CHATGPT_CONFIG_PREFIX + "ULEXCLUDE:";
        String CHATGPT_APITOKEN = CHATGPT_CONFIG_PREFIX + "APITOKEN";
        String CHATGPT_APIHOST = CHATGPT_CONFIG_PREFIX + "APIHOST";
        String CHATGPT_LIMITCOUNT = CHATGPT_CONFIG_PREFIX + "LIMITCOUNT";
        String CHATGPT_PRESET_ANSWERS = CHATGPT_CONFIG_PREFIX + "PRESET_ANSWERS";
        String CHATGPT_PROHIBIT_KEYWORD = CHATGPT_CONFIG_PREFIX + "PROHIBIT_KEYWORD";
        String CHATGPT_REPLACE_REGEX = CHATGPT_CONFIG_PREFIX + "REPLACE_REGEX";
        String CHATGPT_PRERECORD_COUNT = CHATGPT_CONFIG_PREFIX + "PRERECORD_COUNT";
        String CHATGPT_USER_DETAIL = CHATGPT_PREFIX + "USER_DETAIL:";
    }
}
