package vip.yeee.app.common.service;

import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Component;
import vip.yeee.memo.base.redis.kit.CheckRepeatKit;

import javax.annotation.Resource;
import java.util.Date;

@Component
public class CheckRepeatService {

    @Resource
    private CheckRepeatKit checkRepeatKit;

    public boolean canRepeatScheOpr(long s) {
        String key = "scheOpr:" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm");
        return checkRepeatKit.canRepeatOpr(key, s);
    }

    public boolean canRepeatWishUpdate(Long wishId, int s) {
        String key = "wishUpdate:" + wishId;
        return checkRepeatKit.canRepeatOpr(key, s);
    }

    public boolean canRepeatDoSendMsg(String uid, int s) {
        String key = "doSendMsg:" + uid;
        return checkRepeatKit.canRepeatOpr(key, s);
    }
}
