package com.smj.testboot.service.impl;

import com.google.common.collect.Lists;
import com.smj.testboot.bean.ReportVo;
import com.smj.testboot.mapper.AppointMapper;
import com.smj.testboot.service.AppointService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class AppointServiceImpl implements AppointService {

    @Resource
    private AppointMapper appointMapper;

    @Override
    public void update(Date startDate, Date endDate) {

       List<ReportVo> reportVos =  appointMapper.selectAppointInfoByDate(startDate, endDate);

       List<List<ReportVo>> partition = Lists.partition(reportVos, 500);




        for (List<ReportVo> vos : partition) {
            long start = System.currentTimeMillis();
            appointMapper.update(vos);
            long end = System.currentTimeMillis();
            System.out.println(end - start);

            break;
        }





    }

}
