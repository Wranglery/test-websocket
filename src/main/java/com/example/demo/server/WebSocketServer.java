package com.example.demo.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lyd
 * @Description: 因为WebSocket是类似客户端服务端的形式(采用ws协议)，那么这里的WebSocketServer其实就相当于一个ws协议的Controller，
 *               所以可以直接在前端调用@ServerEndpoint("")中的路径，相当于掉接口了
 * @date 15:48
 */
@Slf4j
@Component
@ServerEndpoint("/imserver/{userId}")
public class WebSocketServer {
	/**
	 * 静态变量，用来记录当前在线连接数
	 */
	private static int onlineCount = 0;

	/**
	 * Concurrent包的线程安全Set,用来存放每个客户端对应的WebSocket对象
	 */
	private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();

	/**
	 * 连接会话，给客户端发送数据
	 */
	private Session session;

	/**
	 * 接收userId
	 */
	private String userId = "";

	/**
	 * 连接建立成功调用的方法
	 */
	@OnOpen
	public void onOpen(Session session, @PathParam("userId") String userId) {

		this.session = session;
		this.userId = userId;

		if (webSocketMap.containsKey(userId)) {
			webSocketMap.remove(userId);
			webSocketMap.put(userId, this);
			addOnlineCount();
		} else {
			webSocketMap.put(userId, this);
			addOnlineCount();
		}

		log.info("用户" + userId + "连接,当前在线人数为：" + getOnlineCount());

		try {
			sendMessage("连接成功");
		} catch (IOException e) {
			log.error("用户：" + userId + ",网络异常，哈哈哈");
			e.printStackTrace();
		}

	}

	/**
	 * 连接关闭调用的方法
	 */
	@OnClose
	public void onClose() {
		if (webSocketMap.containsKey(userId)) {
			webSocketMap.remove(userId);
			subOnlineCount();
		}
		log.info("用户:" + userId + "退出成功，当前在线人数为：" + getOnlineCount());

	}

	/**
	 * 收到客户端消息后调用的方法
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		log.info("用户消息:" + userId + ",报文:" + message);
		//可以群发消息
		//消息保存到数据库、redis
		if (StringUtils.isNotBlank(message)) {
			try {
				// 解析报文
				JSONObject jsonObject = JSON.parseObject(message);
				// 追加发送人（防止串改）
				jsonObject.put("fromUserId", this.userId);
				String toUserId = jsonObject.getString("toUserId");
				if (StringUtils.isNotBlank(toUserId) && webSocketMap.containsKey(toUserId)) {
					webSocketMap.get(toUserId).sendMessage(jsonObject.toJSONString());
				}else {
					log.error("请求的userId:"+toUserId+"不在该服务器上");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * 实现服务器消息主动推送
	 *
	 * @param message
	 * @throws IOException
	 */
	public void sendMessage(String message) throws IOException {
		this.session.getBasicRemote().sendText(message);
	}

	/**
	 * 发送自定义消息
	 */
	public static void sendInfo(String message, @PathParam("userId") String userId) throws IOException {
		log.info("发送消息到:" + userId + "，报文:" + message);
		if (StringUtils.isNotBlank(userId) && webSocketMap.containsKey(userId)) {
			webSocketMap.get(userId).sendMessage(message);
		} else {
			log.error("用户" + userId + ",不在线！");
		}
	}

	/**
	 * 获得当前连接数
	 *
	 * @return
	 */
	public static synchronized int getOnlineCount() {
		return onlineCount;
	}

	/**
	 * 在线连接数加1
	 */
	public static synchronized void addOnlineCount() {
		WebSocketServer.onlineCount++;
	}

	/**
	 * 在线连接数减1
	 */
	public static synchronized void subOnlineCount() {
		WebSocketServer.onlineCount--;
	}

	/**
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		log.error("用户错误:" + this.userId + ",原因:" + error.getMessage());
		error.printStackTrace();
	}
}