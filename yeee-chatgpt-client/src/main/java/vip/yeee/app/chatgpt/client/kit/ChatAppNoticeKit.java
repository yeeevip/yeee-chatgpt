package vip.yeee.app.chatgpt.client.kit;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.tokenizer.TokenizerEngine;
import cn.hutool.extra.tokenizer.Word;
import com.alibaba.fastjson.JSON;
import vip.yeee.app.chatgpt.client.listener.AbstractStreamListener;
import vip.yeee.app.chatgpt.client.listener.WsEventSourceListener;
import vip.yeee.memo.base.web.utils.SpringContextUtils;
import vip.yeee.memo.common.websocket.netty.bootstrap.Session;

import java.util.Date;
import java.util.Iterator;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/4/25 16:40
 */
public class ChatAppNoticeKit {

    public static void sendUseLimitMsg(WsEventSourceListener listener, Integer count) {
        String msg = "你好[今日]免费次数已用完，请明日再来吧~~~\n\nTip：请在【手机端】点击右上角···[重新进入小程序]试试\n\n反馈建议QQ:1324459373";
        Iterator<Word> it;
        it = getTokenizerEngine().parse(msg);
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                listener.onMsg(false, "chat", it.next().getText());
            }
        }
    }

    public static void sendQuesFastMsg(WsEventSourceListener listener) {
        Iterator<Word> it;
        it = getTokenizerEngine().parse("提问太快了，请6s后重试！！！");
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                listener.onMsg(false, "chat", it.next().getText());
            }
        }
    }

    public static void sendPresetMsg(WsEventSourceListener listener, String presetMsg) {
        Iterator<Word> it;
        it = getTokenizerEngine().parse(presetMsg);
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                listener.onMsg(false, "chat", it.next().getText());
            }
        }
    }

    public static void sendHeartTimeoutMsg(Session session) {
        AbstractStreamListener.Message msg = new AbstractStreamListener.Message();
        msg.setMsgId("");
        msg.setKind("chat");
        msg.setMsg("检测到您的网络不稳定，请重新进入小程序！！！\n\n反馈建议QQ:1324459373");
//        msg.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        session.sendText(JSON.toJSONString(msg));
        session.close();
    }

    public static void sendAuthFailedMsg(Session session) {
        AbstractStreamListener.Message msg = new AbstractStreamListener.Message();
        msg.setMsgId("");
        msg.setKind("chat");
        msg.setMsg("身份认证过期，请重新进入小程序！！！");
        session.sendText(JSON.toJSONString(msg));
        session.close();
    }

    private static TokenizerEngine getTokenizerEngine() {
        return ((TokenizerEngine) SpringContextUtils.getBean(TokenizerEngine.class));
    }
}
