package com.atguigu.gulimall.product;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

/**
 * @author QingnanZhang
 * @creat 2022-07-01 17:05
 **/
public class JavaToJson {
    @Test
    public void test(){
        List<SourceDurationDto> sourceDurationDtos= new ArrayList<>();
        SourceDurationDto sourceDurationDto = new SourceDurationDto(1, 2, 3L, 4L, 5L, 2000L, 3000L);
        sourceDurationDtos.add(sourceDurationDto);
        String strs= JSON.toJSONString(sourceDurationDtos);
        System.out.println(strs);
    }

}


class SourceDurationDto {

    public SourceDurationDto(Integer appId, Integer subjectId, Long videoId, Long videoStudyProgress, Long videoDuration, Long beginTime, Long endTime) {
        this.appId = appId;
        this.subjectId = subjectId;
        this.videoId = videoId;
        this.videoStudyProgress = videoStudyProgress;
        this.videoDuration = videoDuration;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    private Integer appId;

    private Integer subjectId;

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getVideoStudyProgress() {
        return videoStudyProgress;
    }

    public void setVideoStudyProgress(Long videoStudyProgress) {
        this.videoStudyProgress = videoStudyProgress;
    }

    public Long getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Long videoDuration) {
        this.videoDuration = videoDuration;
    }

    public Long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Long beginTime) {
        this.beginTime = beginTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    private Long videoId;

    private Long videoStudyProgress;

    private Long videoDuration;

    private Long beginTime;

    private Long endTime;
}
