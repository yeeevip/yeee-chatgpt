package vip.yeee.app.chatgpt.client.controller;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import vip.yeee.app.chatgpt.client.biz.ApiChatGptBiz;
import vip.yeee.memo.common.websocket.netty.annotation.*;
import vip.yeee.memo.common.websocket.netty.bootstrap.Session;

import vip.yeee.memo.common.websocket.netty.annotation.PathParam;
import java.io.IOException;
@Slf4j
@Component
@ServerEndpoint(path = "/ws/airobot/{chatId}", port = "8802", readerIdleTimeSeconds = "15")
public class ChatAppWebSocket {

    private static ApiChatGptBiz apiChatGptBiz = null;

    public ChatAppWebSocket() {
    }

    @Autowired
    public ChatAppWebSocket(ApiChatGptBiz apiChatGptBiz) {
        ChatAppWebSocket.apiChatGptBiz = apiChatGptBiz;
    }

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, @PathParam("chatId") String chatId, @RequestParam MultiValueMap<String, String> reqParams) {
        apiChatGptBiz.handleWsOnOpen(session, headers, chatId, reqParams);
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        apiChatGptBiz.handleWsOnMessage(session, msg);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        apiChatGptBiz.handleWsOnClose(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        apiChatGptBiz.handleWsOnError(session, error);
    }

    @OnEvent
    public void onEvent(Session session, Object evt) {
        apiChatGptBiz.handleWsOnEvent(session, evt);
    }

}

