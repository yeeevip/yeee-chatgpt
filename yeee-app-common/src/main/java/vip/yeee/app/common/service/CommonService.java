package vip.yeee.app.common.service;

import cn.hutool.core.util.StrUtil;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vip.yeee.memo.base.redis.constant.RedisConstant;
import vip.yeee.memo.base.util.IpAddressUtil;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * description......
 *
 * @author yeeee
 * @since 2022/8/11 11:20
 */
@Service
public class CommonService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Cached(cacheType = CacheType.LOCAL, localExpire = 5 * 60)
    public String getCachedAddressByIp(String ip) {
        String key = RedisConstant.KEY_PREFIX_VALUE + "ipaddr:" + ip;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        value = IpAddressUtil.getAddressByIp(ip);
        stringRedisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES);
        return value;
    }
}
