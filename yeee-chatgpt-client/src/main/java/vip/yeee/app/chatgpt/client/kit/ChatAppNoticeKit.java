package vip.yeee.app.chatgpt.client.kit;

import cn.hutool.core.collection.IterUtil;
import cn.hutool.extra.tokenizer.Word;
import vip.yeee.app.chatgpt.client.listener.WsEventSourceListener;
import vip.yeee.app.common.config.ADictionaryExtra;

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
        it = ADictionaryExtra.engine.parse(msg);
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                listener.onMsg(false, "chat", it.next().getText());
            }
        }
    }

    public static void sendQuesFastMsg(WsEventSourceListener listener) {
        Iterator<Word> it;
        it = ADictionaryExtra.engine.parse("提问太快了，请10s后重试！！！");
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                listener.onMsg(false, "chat", it.next().getText());
            }
        }
    }

    public static void sendPresetMsg(WsEventSourceListener listener, String presetMsg) {
        Iterator<Word> it;
        it = ADictionaryExtra.engine.parse(presetMsg);
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                listener.onMsg(false, "chat", it.next().getText());
            }
        }
    }
}
