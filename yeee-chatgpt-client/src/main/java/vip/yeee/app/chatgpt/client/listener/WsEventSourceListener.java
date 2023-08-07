package vip.yeee.app.chatgpt.client.listener;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import vip.yeee.app.chatgpt.client.domain.local.ChatLocalRepository;
import vip.yeee.app.chatgpt.client.model.ChatMessage2;

import vip.yeee.memo.common.websocket.netty.bootstrap.Session;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WsEventSourceListener extends AbstractStreamListener {

    private EventSource eventSource;

    private final LinkedBlockingQueue<String> messageLinkedBlockingQueue;

    @Getter
    private final String question;

    private final String msgId;

    private volatile boolean canReply;

    private StringBuffer errSendMsg = new StringBuffer();

    public WsEventSourceListener(String chatId, String uid, String question) {
        this.setChatId(chatId);
        this.setUid(uid);
        this.question = question;
        WsEventSourceListener sourceListener = ChatAppWsContext.getUserRecentESL(chatId, uid);
        if (sourceListener != null) {
            sourceListener.clear();
        }
        messageLinkedBlockingQueue = new LinkedBlockingQueue<>();
        msgId = IdUtil.simpleUUID();
        this.startThreadSendWsMsg();
    }

    @Override
    public void onMsg(boolean cache, String kind, String message) {
        try {
            Message msg = new Message();
            msg.setMsgId(msgId);
            msg.setKind(kind);
            msg.setMsg(message);
            msg.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
            this.messageLinkedBlockingQueue.put(JSON.toJSONString(msg));
            if (cache) {
                // 写入缓存
                List<ChatMessage2> messages = ChatLocalRepository.get(chatId);
                ChatMessage2 chatMessage;
                if (CollectionUtil.isNotEmpty(messages) && msgId.equals((chatMessage = messages.get(messages.size() - 1)).getMagId())) {
                    chatMessage.setContent(chatMessage.getContent() + message);
                } else {
                    chatMessage = new ChatMessage2();
                    chatMessage.setMagId(msgId);
                    chatMessage.setRole(getGptRole());
                    chatMessage.setContent(message);
                    messages.add(chatMessage);
                    ChatLocalRepository.put(chatId,messages);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable throwable, String response) {
        Session session = ChatAppWsContext.getUserSession(this.chatId, uid);
        if (session == null || !session.isOpen()) {
            return;
        }
        Message msg = new Message();
        msg.setMsgId(msgId);
        msg.setKind("chat");
        if (response.contains("context_length_exceeded")) {
            ChatLocalRepository.getChatContext(this.chatId).clear();
        }
        if (response.contains("Rate limit reached")) {
            msg.setMsg("\n当前请求人数太多，请10s后重试！！！");
        } else {
            msg.setMsg("\n服务器开小差了，请5s后重试！！！");
        }
        msg.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        session.sendText(JSON.toJSONString(msg));
    }

    public void clear() {
        this.canReply = false;
        this.messageLinkedBlockingQueue.clear();
        this.eventSource.cancel();
    }

    public void setEventSource(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    public void setCanReply(boolean canReply) {
        this.canReply = canReply;
    }

    public String getMsgId() {
        return msgId;
    }

    public void startThreadSendWsMsg() {
        this.canReply = true;
        Session session = ChatAppWsContext.getUserSession(chatId, uid);
         new Thread(() -> {
            while (canReply) {
                String message = null;
                try {
                    if (session != null && session.isOpen()) {
                        message = messageLinkedBlockingQueue.take();
                        if (StrUtil.isNotBlank(errSendMsg.toString())) {
                            session.sendText(errSendMsg.toString());
                            errSendMsg = new StringBuffer();
                            TimeUnit.MILLISECONDS.sleep(70);
                        }
                        session.sendText(message);
                        TimeUnit.MILLISECONDS.sleep(70);
                    }
                } catch (Exception e) {
                    log.error("sendMsg error", e);
                    if (e instanceof IllegalStateException) {
                        if (StrUtil.isNotBlank(message)) {
                            errSendMsg = new StringBuffer();
                            errSendMsg.append(message);
                            break;
                        }
                    }
                }
            }
        }).start();
    }
}
