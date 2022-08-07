package vip.yeee.app.chatgpt.client.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import vip.yeee.app.chatgpt.client.model.ChatMessage;
import vip.yeee.app.chatgpt.client.model.ChatResult;
import vip.yeee.app.chatgpt.client.model.ChoiceModel;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractStreamListener extends EventSourceListener {

    protected String lastMessage = "";

    private String gptRole = "";
    protected String chatId = "";
    protected String uid = "";
    protected Date startTime;
    protected Date firstRespTime;
    @Getter
    protected boolean hasDone;

    @Setter
    @Getter
    protected Consumer<AbstractStreamListener> onComplete = listener -> {

    };

    @Setter
    @Getter
    protected Consumer<AbstractStreamListener> firstReceiveFunc = listener -> {

    };

    @Setter
    @Getter
    protected Consumer<AbstractStreamListener> closeFunc = listener -> {

    };

    /**
     * Called when a new message is received.
     * 收到消息 单个字
     *
     * @param message the new message
     */
    public abstract void onMsg(boolean cache, String kind, String message);

    /**
     * Called when an error occurs.
     * 出错时调用
     *
     * @param throwable the throwable that caused the error
     * @param response  the response associated with the error, if any
     */
    public abstract void onError(Throwable throwable, String response);

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.info("【CHAT-GPT】- OKHTTP SSE onOpen = {}", response);
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.info("【CHAT-GPT】- OKHTTP SSE closed = {}", eventSource);
        closeFunc.accept(this);
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {

        if (data.equals("[DONE]")) {
            this.hasDone = true;
            onComplete.accept(this);
            return;
        }

        if (this.firstRespTime == null) {
            firstRespTime = new Date();
            firstReceiveFunc.accept(this);
        }

        ChatResult response = JSON.parseObject(data, ChatResult.class);
        // 读取Json
        List<ChoiceModel> choices = response.getChoices();
        if (choices == null || choices.isEmpty()) {
            return;
        }
        ChatMessage delta = choices.get(0).getDelta();
        String text = delta.getContent();

        if (StrUtil.isBlank(gptRole)) {
            gptRole = delta.getRole();
        }

        if (text != null) {

            lastMessage += text;

            onMsg(true, "chat", text);

        }

    }


    @SneakyThrows
    @Override
    public void onFailure(EventSource eventSource, Throwable throwable, Response response) {

        try {
            log.error("【CHAT-GPT】- OKHTTP SSE Stream connection error: response = {}", response, throwable);

            String responseText = "";

            if (Objects.nonNull(response)) {
                responseText = response.body().string();
            }

            log.error("【CHAT-GPT】- OKHTTP SSE responseText：{}", responseText);

            String forbiddenText = "Your access was terminated due to violation of our policies";

            if (StrUtil.contains(responseText, forbiddenText)) {
                log.error("Chat session has been terminated due to policy violation");
                log.error("检测到号被封了");
            }

            String overloadedText = "That model is currently overloaded with other requests.";

            if (StrUtil.contains(responseText, overloadedText)) {
                log.error("检测到官方超载了，赶紧优化你的代码，做重试吧");
            }

            this.onError(throwable, responseText);

        } catch (Exception e) {
            log.warn("【CHAT-GPT】- OKHTTP SSE onFailure error", e);
            // do nothing

        } finally {
            eventSource.cancel();
        }
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFirstRespTime() {
        return firstRespTime;
    }

    public void setFirstRespTime(Date firstRespTime) {
        this.firstRespTime = firstRespTime;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getGptRole() {
        return gptRole;
    }

    @Data
    public static class Message {
        private String kind;
        private String msg;
        private String msgId;
        private String createTime;
    }
}
