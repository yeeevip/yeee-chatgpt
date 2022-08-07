package vip.yeee.app.chatgpt.client.domain.redis;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Maps;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
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
    private static final AES aes = SecureUtil.aes(SecureUtil.md5("HFJAHSLKDFJDASKFJASKL").getBytes(StandardCharsets.UTF_8));
    private static final TimedCache<String, String> userTokenMap = CacheUtil.newTimedCache(TimeUnit.HOURS.toMillis(1));
    private static final Map<String, String> userOpenIdMap = Maps.newConcurrentMap();
    public Integer getULimitCountCache(String uid) {
        String uLimitKey = RedisKeys.CHATGPT_U_DAY_LIMIT;
        return Integer.parseInt(Optional.ofNullable(stringRedisTemplate.opsForHash().get(uLimitKey, uid)).orElse("0").toString());
    }

    public void incrULimitCountCache(String uid) {
        incrULimitCountCache(uid, 1L);
    }

    public void incrULimitCountCache(String uid, long incr) {
        String uLimitKey = RedisKeys.CHATGPT_U_DAY_LIMIT;
        boolean has = false;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(uLimitKey))) {
            has = true;
        }
        stringRedisTemplate.opsForHash().increment(uLimitKey, uid, incr);
        if (!has) {
            stringRedisTemplate.expire(uLimitKey, DateUtil.between(new Date(), DateUtil.endOfDay(new Date()), DateUnit.SECOND), TimeUnit.SECONDS);
        }
        incrUserUsedCount(uid);
    }

    public void reSetULimitCountCache(String uid, String count) {
        String uLimitKey = RedisKeys.CHATGPT_U_DAY_LIMIT;
        boolean has = false;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(uLimitKey))) {
            has = true;
        }
        stringRedisTemplate.opsForHash().put(uLimitKey, uid, count);
        if (!has) {
            stringRedisTemplate.expire(uLimitKey, DateUtil.between(new Date(), DateUtil.endOfDay(new Date()), DateUnit.SECOND), TimeUnit.SECONDS);
        }
        incrUserUsedCount(uid);
    }

    @Cached(cacheType = CacheType.LOCAL, expire = 60)
    public String getULimitExclude() {
        String key = RedisKeys.CHATGPT_ULEXCLUDE;
        return stringRedisTemplate.opsForValue().get(key);
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

    public String genTokenAndCache(String ipAddr) {
        String token = aes.encryptHex(ipAddr);
        userTokenMap.put(token, ipAddr);
        return token;
    }

    public String checkToken(String token) {
        return userTokenMap.get(token);
    }

    public void saveUserOpenIdCache(String uid, String openId) {
        userOpenIdMap.put(uid, openId);
    }

    public void incrUserUsedCount(String uid) {
        stringRedisTemplate.opsForHash().increment(RedisKeys.CHATGPT_USER_DETAIL + uid, "usedCount", 1L);
    }

    public void recordUserDetail(String uid, String attr, String val) {
        stringRedisTemplate.opsForHash().put(RedisKeys.CHATGPT_USER_DETAIL + uid, attr, val);
    }

    public String getUserOpenIdCache(String uid) {
        return userOpenIdMap.get(uid);
    }

    public Integer getUserSurplus(String ipAddr, String uKey) {
        return Math.max((StrUtil.isBlank(this.getULimitExclude())
                || Arrays.stream(this.getULimitExclude().split(",")).noneMatch(ex -> ex.equals(ipAddr) || ex.equals(uKey)))
                ? this.getULimitCount() - this.getULimitCountCache(uKey) : 1000, 0);
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

    interface RedisKeys {
        String CHATGPT_PREFIX = "YEEE:CHATGPT:";
        String CHATGPT_CONFIG_PREFIX = CHATGPT_PREFIX + "CONFIG:";
        String CHATGPT_U_DAY_LIMIT = CHATGPT_PREFIX + "ULIMIT";
        String CHATGPT_ULEXCLUDE = CHATGPT_CONFIG_PREFIX + "ULEXCLUDE";
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
