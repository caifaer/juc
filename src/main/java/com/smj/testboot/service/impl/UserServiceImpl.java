package com.smj.testboot.service.impl;


import com.smj.testboot.bean.UserBean;
import com.smj.testboot.mapper.UserMapper;
import com.smj.testboot.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserBean getInfo() {
        return userMapper.getInfo();
    }
}
