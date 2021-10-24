package com.smj.testboot.mapper;


import com.smj.testboot.bean.ReportVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AppointMapper {

    List<ReportVo> selectAppointChannelInfoByDate(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    List<ReportVo> selectAppointInfoByDate(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    void updateAppointInfo(@Param("list") List<ReportVo> reportVos);

    void updateAppointChannelInfo(@Param("list") List<ReportVo> reportVos);


    void update(@Param("list") List<ReportVo> reportVos);
}
