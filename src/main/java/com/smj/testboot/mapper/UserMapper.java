package com.smj.testboot.mapper;


import com.smj.testboot.bean.UserBean;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper {

     UserBean getInfo();

}
