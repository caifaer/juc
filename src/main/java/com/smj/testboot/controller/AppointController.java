package com.smj.testboot.controller;


import com.smj.testboot.service.AppointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class AppointController {


    @Autowired
    private AppointService appointService;


    @RequestMapping("/update")
    public void update(@DateTimeFormat(pattern = "yyyy-MM-dd")Date startDate, @DateTimeFormat(pattern = "yyyy-MM-dd")Date endDate) {
        appointService.update(startDate, endDate);
    }




}
