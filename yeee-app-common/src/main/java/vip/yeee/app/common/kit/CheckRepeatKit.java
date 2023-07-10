package vip.yeee.app.common.kit;

import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Component;
import vip.yeee.memo.base.redis.kit.RedisKit;

import javax.annotation.Resource;
import java.util.Date;

@Component
public class CheckRepeatKit {

    @Resource
    private RedisKit redisKit;

    public boolean canRepeatScheOpr(long s) {
        String key = "scheOpr:" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm");
        return redisKit.canRepeatOpr(key, s);
    }

    public boolean canRepeatWishUpdate(Long wishId, int s) {
        String key = "wishUpdate:" + wishId;
        return redisKit.canRepeatOpr(key, s);
    }

    public boolean canRepeatDoSendMsg(String uid, int s) {
        String key = "doSendMsg:" + uid;
        return redisKit.canRepeatOpr(key, s);
    }
}
