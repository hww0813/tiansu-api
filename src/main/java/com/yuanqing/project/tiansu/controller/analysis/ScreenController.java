package com.yuanqing.project.tiansu.controller.analysis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.constant.ScreenConstants;
import com.yuanqing.framework.redis.RedisCache;
import com.yuanqing.framework.web.domain.AjaxResult;
import com.yuanqing.project.tiansu.domain.analysis.dto.ScreenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xucan
 */
@RestController
@RequestMapping(value = "/api")
@CrossOrigin
public class ScreenController {

    @Autowired
    private RedisCache redisCache;


    @PostMapping(value = "/assets/summary")
    public AjaxResult getSummary(){

        Object cacheObject = redisCache.getCacheObject(ScreenConstants.SUMMARY);

        return AjaxResult.success("操作成功",cacheObject);
    }

    @PostMapping(value = "/warn/total")
    public AjaxResult getWarnTotal(@RequestBody ScreenDto screenDto){

        JSONObject eventList = redisCache.getCacheObject(ScreenConstants.WARN);

        String eventCnt = "0";
        if( eventList != null && eventList.size()!=0 ) {
            if(screenDto.getDateType()==1){
                eventCnt = (String) eventList.get("day");
            }else if(screenDto.getDateType()==2){
                eventCnt = (String) eventList.get("week");
            }else if(screenDto.getDateType()==3){
                eventCnt = (String) eventList.get("month");
            }
        }
        JSONObject list = new JSONObject();
        list.put("warnCount",eventCnt);
        return AjaxResult.success("操作成功",list);
    }


    @PostMapping(value = "/person/top")
    public AjaxResult userTOP(@RequestBody ScreenDto screenDto){

        List<JSONObject> personList = null;
        if(screenDto.getDateType()==1){
            personList = redisCache.getCacheObject(ScreenConstants.USER_DAY);
        }else if(screenDto.getDateType()==2){
            personList = redisCache.getCacheObject(ScreenConstants.USER_WEEK);
        }else if(screenDto.getDateType()==3){
            personList = redisCache.getCacheObject(ScreenConstants.USER_MONTH);
        }

        return AjaxResult.success("操作成功",personList);
    }

    @PostMapping(value = "/camera/top")
    public AjaxResult cameraTOP(@RequestBody ScreenDto screenDto){

        List<JSONObject> cameraList = null;
        if(screenDto.getDateType()==1){
            cameraList = redisCache.getCacheObject(ScreenConstants.CAMERA_DAY);
        }else if(screenDto.getDateType()==2){
            cameraList = redisCache.getCacheObject(ScreenConstants.CAMERA_WEEK);
        }else if(screenDto.getDateType()==3){
            cameraList = redisCache.getCacheObject(ScreenConstants.CAMERA_MONTH);
        }

        return AjaxResult.success("操作成功",cameraList);
    }
    @PostMapping(value = "/terminal/top")
    public AjaxResult terminalTOP(@RequestBody ScreenDto screenDto){


        List<JSONObject> terminalList = null;
        if(screenDto.getDateType()==1){
            terminalList = redisCache.getCacheObject(ScreenConstants.TERMINAL_DAY);
        }else if(screenDto.getDateType()==2){
            terminalList = redisCache.getCacheObject(ScreenConstants.TERMINAL_WEEK);
        }else if(screenDto.getDateType()==3){
            terminalList = redisCache.getCacheObject(ScreenConstants.TERMINAL_MONTH);
        }

        return AjaxResult.success("操作成功",terminalList);
    }
    @PostMapping(value = "/behavior/trend")
    public AjaxResult behaviorTrend(){

        return AjaxResult.success("操作成功",redisCache.getCacheObject(ScreenConstants.OPERATION_CATEGORY));
    }

    @PostMapping(value = "/oper/add")
    public AjaxResult operAdd(){
        Object cacheObject = redisCache.getCacheObject(ScreenConstants.REAL_OPERATION_WARN);
        return AjaxResult.success("操作成功",cacheObject);
    }

    @PostMapping(value = "/operate/summary")
    public AjaxResult operateSummary(@RequestBody ScreenDto screenDto){

        JSONObject operateSummary = null;
        if(screenDto.getDateType()==1){
            operateSummary = redisCache.getCacheObject(ScreenConstants.OPERATION_NUM_DAY);
        }else if(screenDto.getDateType()==2){
            operateSummary = redisCache.getCacheObject(ScreenConstants.OPERATION_NUM_WEEK);
        }else if(screenDto.getDateType()==3){
            operateSummary = redisCache.getCacheObject(ScreenConstants.OPERATION_NUM_MONTH);
        }
        return AjaxResult.success("操作成功",operateSummary);
    }

    @PostMapping(value = "/video/top")
    public AjaxResult videoTop(@RequestBody ScreenDto screenDto){


        List<JSONObject> videoTop = null;
        if(screenDto.getDateType()==1){
            videoTop = redisCache.getCacheObject(ScreenConstants.VIDEO_NUM_DAY);
        }else if(screenDto.getDateType()==2){
            videoTop = redisCache.getCacheObject(ScreenConstants.VIDEO_NUM_WEEK);
        }else if(screenDto.getDateType()==3){
            videoTop = redisCache.getCacheObject(ScreenConstants.VIDEO_NUM_MONTH);
        }

        return AjaxResult.success("操作成功",videoTop);
    }

    @PostMapping(value = "/map/statis")
    public AjaxResult cameraMap(){

        return AjaxResult.success("操作成功",redisCache.getCacheObject(ScreenConstants.CAMERA_MAP));
    }




}