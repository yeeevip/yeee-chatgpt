package vip.yeee.app.chatgpt.client.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vip.yeee.app.chatgpt.client.biz.ApiChatGptBiz;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
@Slf4j
@Component
@ServerEndpoint("/ws/airobot/{chatId}/{token}")
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

