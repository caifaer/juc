package com.smj.testboot.bean;



import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;


@Data
public class ReportVo implements Serializable {


    private Integer id;
    private String schResultId;
    private String corpCode;
    private String deptCode;
    private String deptName;
    private String doctCode;
    private String doctName;
    private Integer channelType;
    private Integer appNum=0;
    private Integer regNum=0;
    private Integer expNum=0;
    private Integer finishNum=0;
    private Integer backNum=0;
    private Integer cancelNum=0;
    private Integer totalNum=0;

    private String uniqueKey;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date schDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
