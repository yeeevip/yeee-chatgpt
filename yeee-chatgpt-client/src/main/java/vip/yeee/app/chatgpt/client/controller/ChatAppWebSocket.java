package vip.yeee.app.chatgpt.client.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vip.yeee.app.chatgpt.client.biz.ApiChatGptBiz;
import vip.yeee.memo.common.websocket.netty.annotation.*;
import vip.yeee.memo.common.websocket.netty.bootstrap.Session;

import vip.yeee.memo.common.websocket.netty.annotation.PathParam;
import java.io.IOException;
@Slf4j
@Component
@ServerEndpoint(path = "/ws/airobot/{chatId}/{token}", port = "8802")
public class ChatAppWebSocket {

    private Session session;

    private static ApiChatGptBiz apiChatGptBiz = null;

    public ChatAppWebSocket() {
    }

    @Autowired
    public ChatAppWebSocket(ApiChatGptBiz apiChatGptBiz) {
        ChatAppWebSocket.apiChatGptBiz = apiChatGptBiz;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("chatId") String chatId, @PathParam("token") String token) throws IOException {
        this.session = session;
        apiChatGptBiz.handleWsOnOpen(session, chatId, token);
    }

    @OnMessage
    public void onMessage(String msg) {
        apiChatGptBiz.handleWsOnMessage(this.session, msg);
    }

    @OnClose
    public void onClose() throws IOException {
        apiChatGptBiz.handleWsOnClose(this.session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        apiChatGptBiz.handleWsOnError(session, error);
    }

}

