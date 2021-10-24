package com.smj.testboot.controller;


import com.smj.testboot.bean.UserBean;
import com.smj.testboot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {


    @Autowired
    private UserService userService;

    @RequestMapping("/index")
    public String index() {
        return "index";
    }


    @RequestMapping("/hello")
    public void hello() {
        System.out.println(123);
    }


    @RequestMapping("getUser")
    public UserBean getInfo() {
       return userService.getInfo();
    }



}
