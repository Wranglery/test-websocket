package com.example.demo.controller;

import com.example.demo.server.WebSocketServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.websocket.server.PathParam;
import java.io.IOException;

/**
 * @author lyd
 * @Description:
 * @date 16:31
 */
@RestController
public class DemoController {

	/**
	 * 接口测试
	 * @return
	 */
	@RequestMapping("index")
	public ResponseEntity index(){
		return ResponseEntity.ok("请求成功");
	}

	/**
	 * 页面跳转
	 * @return
	 */
	@RequestMapping("page")
	public ModelAndView page(){
		return new ModelAndView("testPage.html");
	}

	/**
	 * 消息推送
	 * @param message
	 * @param toUserId
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("pushToWeb")
	public ResponseEntity<String> pushToWeb(String message, @PathParam("toUserId") String toUserId) throws IOException {
		WebSocketServer.sendInfo(message,toUserId);
		return ResponseEntity.ok("MSG SEND SUCCESS");
	}


}