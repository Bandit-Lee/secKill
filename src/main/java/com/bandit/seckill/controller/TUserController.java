package com.bandit.seckill.controller;



import com.bandit.seckill.entity.TUser;
import com.bandit.seckill.rabbitmq.MQSender;
import com.bandit.seckill.service.impl.TUserServiceImpl;
import com.bandit.seckill.utils.MD5Util;
import com.bandit.seckill.vo.RespBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author LiChao
 * @since 2022-03-02
 */
@RestController
@RequestMapping("/user")
@Api(value = "用户表", tags = "用户表")
@Slf4j
public class TUserController {

    @Autowired
    TUserServiceImpl userService;

    @Autowired
    MQSender mqSender;

    @GetMapping("/createUser/{count}")
    @ApiOperation("压测用户写入数据库")
    @ResponseBody
    public RespBean createUser(@PathVariable("count") int count) throws IOException {
//        ArrayList<TUser> list = new ArrayList<>(count);
//        for (int i = 0; i < count; i++) {
//            TUser user = TUser.builder()
//                    .id(1300000L + i)
//                    .nickname("user" + i)
//                    .salt("1a2b3c4d")
//                    .password(MD5Util.inputPassToDBPass("123456", "1a2b3c4d"))
//                    .build();
//            list.add(user);
//        }
//        log.info("create user success.now writing to db");
//        userService.saveBatch(list);
//        log.info("insert completed.");
        List<TUser> list = userService.list();
        String urlString = "http://localhost:8883/login/doLogin";
        File file = new File("C:\\Users\\Bandit\\Desktop\\config.txt");
        if (file.exists()) {
            file.delete();
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.seek(0);
        for (int i = 0; i < list.size(); i++) {
            TUser tUser = list.get(i);
            //构建http请求流
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            String params = "mobile=" + tUser.getId() + "&password=" + MD5Util.inputPassToFromPass("123456");
            outputStream.write(params.getBytes());
            outputStream.flush();
            InputStream inputStream = httpURLConnection.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) >= 0) {
                byteArrayOutputStream.write(buff, 0, len);
            }
            inputStream.close();
            byteArrayOutputStream.close();
            //拿到响应
            String respone = new String(byteArrayOutputStream.toByteArray());
            ObjectMapper mapper = new ObjectMapper();
            RespBean respBean = mapper.readValue(respone, RespBean.class);
            String userTicket = (String) respBean.getObject();
            System.out.println("create userTicket:" + tUser.getId());
            String row = tUser.getId() + "," + userTicket;
            //写入文件
            randomAccessFile.seek(randomAccessFile.length());
            randomAccessFile.write(row.getBytes());
            randomAccessFile.write("\r\n".getBytes());
            System.out.println("write to file :" + tUser.getId());
        }
        randomAccessFile.close();
        log.info("ok");
        return RespBean.success();
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation("返回用户信息")
    public RespBean info(TUser user) {
        return RespBean.success(user);
    }


//    @RequestMapping(value = "/mq", method = RequestMethod.GET)
//    @ResponseBody
//    public void mq() {
//        mqSender.send("Hello");
//    }
//
//    @RequestMapping(value = "/mq/fanout", method = RequestMethod.GET)
//    @ResponseBody
//    public void mqFanout() {
//        mqSender.send("Hello");
//    }
//
//    @RequestMapping(value = "/mq/direct01", method = RequestMethod.GET)
//    @ResponseBody
//    public void mqDirect01() {
//        mqSender.send01("Hello Red");
//    }
//
//    @RequestMapping(value = "/mq/direct02", method = RequestMethod.GET)
//    @ResponseBody
//    public void mqDirect02() {
//        mqSender.send02("Hello Green");
//    }
//
//    @RequestMapping(value = "/mq/topic01", method = RequestMethod.GET)
//    @ResponseBody
//    public void mqtopic01() {
//        mqSender.send03("Hello Red");
//    }
//
//    @RequestMapping(value = "/mq/topic02", method = RequestMethod.GET)
//    @ResponseBody
//    public void mqtopic02() {
//        mqSender.send04("Hello Green");
//    }
//
//    @RequestMapping(value = "/mq/header01", method = RequestMethod.GET)
//    @ResponseBody
//    public void header01() {
//        mqSender.send05("Hello 01");
//    }
//
//    @RequestMapping(value = "/mq/header02", method = RequestMethod.GET)
//    @ResponseBody
//    public void header02() {
//        mqSender.send06("Hello 02");
//    }
}
