package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    //存放会话对象
    //注意：@ServerEndpoint标注的类由Tomcat为每条连接创建一个新实例（不经过Spring容器），
    //而业务代码注入的是Spring创建的单例，两者只能通过static的sessionMap共享状态，因此必须为static。
    //连接建立/断开由各自的Tomcat IO线程并发调用put/remove，群发则由业务线程遍历该Map，
    //故必须使用线程安全的ConcurrentHashMap。
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        //先登记会话再打日志，避免日志异常导致会话未注册
        sessionMap.put(sid, session);
        log.info("客户端：{}建立连接，当前在线连接数：{}", sid, sessionMap.size());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端：{}的信息:{}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
        log.info("连接断开:{}，当前在线连接数：{}", sid, sessionMap.size());
    }

    /**
     * 连接发生错误时调用的方法
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket连接发生错误，sessionId：{}", session == null ? null : session.getId(), error);
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        //遍历entrySet以便发送失败时按sid清理失效会话
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            String sid = entry.getKey();
            Session session = entry.getValue();

            //会话已关闭则直接清理，避免无效重试
            if (!session.isOpen()) {
                sessionMap.remove(sid, session);
                continue;
            }

            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                //发送失败通常意味着连接已断开（如客户端进程被强杀），此时@OnClose不一定会被回调，
                //必须在此主动清理，否则失效会话会残留在集合中被反复重试。
                //使用双参remove，保证sid已被新连接复用的情况下不会误删新会话。
                log.warn("向客户端：{}发送消息失败，移除该会话：{}", sid, e.getMessage());
                sessionMap.remove(sid, session);
            }
        }
    }

}
