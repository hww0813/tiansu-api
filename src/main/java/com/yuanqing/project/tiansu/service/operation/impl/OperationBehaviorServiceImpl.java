package com.yuanqing.project.tiansu.service.operation.impl;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.enums.ActionDetailType;
import com.yuanqing.common.enums.ActionType;
import com.yuanqing.common.utils.DateUtils;
import com.yuanqing.common.utils.ip.IpUtils;
import com.yuanqing.framework.aspectj.lang.annotation.OperScope;
import com.yuanqing.framework.redis.RedisCache;
import com.yuanqing.framework.web.domain.AjaxResult;
import com.yuanqing.framework.web.domain.PageResult;
import com.yuanqing.project.tiansu.domain.operation.OperationBehavior;
import com.yuanqing.project.tiansu.mapper.operation.OperationBehaviorMapper;
import com.yuanqing.project.tiansu.service.operation.IOperationBehaviorService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Dong.Chao
 * @Classname OperationBehaviorServiceImpl
 * @Description
 * @Date 2021/2/25 11:37
 * @Version V1.0
 */
@Service
public class OperationBehaviorServiceImpl implements IOperationBehaviorService {

    @Resource
    private OperationBehaviorMapper operationBehaviorMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    @OperScope(operAlias = "v")
    public PageResult queryOperationList(OperationBehavior operationBehavior) throws Exception {

        if (StringUtils.isNotBlank(operationBehavior.getOrderType()) && StringUtils.isNotBlank(operationBehavior.getOrderValue())) {
            operationBehavior.setOrderType(operationBehavior.getOrderValue() + " " + operationBehavior.getOrderType());
        }
        //地区码判断
        if (operationBehavior.getRegionList() != null) {
            String[] regionList = operationBehavior.getRegionList();
            String region = regionList[regionList.length - 1];
            if (regionList.length == 1) {
                int count = region.length();
                if (count == 2) {
                    operationBehavior.setProvinceRegion(region);
                }
                if (count == 4) {
                    operationBehavior.setCityRegion(region);
                }
                if (count == 6) {
                    operationBehavior.setCityRegion(region);
                }
            }
            if (regionList.length == 2) {
                int count = region.length();
                if (count == 4) {
                    operationBehavior.setCityRegion(region);
                }
                if (count == 6) {
                    operationBehavior.setCountryRegion(region);
                }
            }
            if (regionList.length == 3) {
                operationBehavior.setCountryRegion(region);
            }
        }
        operationBehavior.setNum(operationBehavior.getSize() * (operationBehavior.getNum() - 1));
        //总数据
        CompletableFuture<Integer> totalFuter = CompletableFuture.supplyAsync(() -> operationBehaviorMapper.quertyOperationBehaviorCount(operationBehavior));
        //操作行为列表
        CompletableFuture<List<OperationBehavior>> operationBehaviorsFuture = CompletableFuture.supplyAsync(() -> operationBehaviorMapper.getList(operationBehavior));

        return PageResult.success(operationBehaviorsFuture.get(), operationBehavior.getNum(), operationBehavior.getSize(), totalFuter.get());
    }

    @Override
    public AjaxResult getCharts(LocalDate startDate, LocalDate endDate, String action, String sort, String type) {
        String timeType = DateUtils.getTimeType(startDate, endDate);
        Long actionType = getActionType(action);
        //拼 redis Key
        if (StringUtils.isNotBlank(type)) {
            type += "_";
        } else {
            type = "";
        }
        String cacheKey = type + timeType + "_" + actionType;
        if (!"desc".equals(sort)) {
            cacheKey = cacheKey + "_REVERSE";
        }
        return AjaxResult.success("success", redisCache.getCacheObject(cacheKey));
    }


    public Long getActionType(String action) {
        Long actionType = null;
        if (StringUtils.isBlank(action)) {
            actionType = -1L;
        } else {
            actionType = Long.parseLong(action);
        }
        return actionType;
    }

    @Override
    public List<JSONObject> getAllToReport(OperationBehavior oper) {
        List<JSONObject> reportList = new ArrayList<JSONObject>();

        List<OperationBehavior> operationBehaviorList = operationBehaviorMapper.getList(oper);
        if (!CollectionUtils.isEmpty(operationBehaviorList)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (OperationBehavior operationBehavior : operationBehaviorList) {
                JSONObject jsonObject = new JSONObject();
                if (operationBehavior.getSrcIp() != null) {
                    jsonObject.put("srcIp", IpUtils.longToIPv4(operationBehavior.getSrcIp()));
                } else {
                    jsonObject.put("srcIp", "");
                }
                if (operationBehavior.getDstIp() != null) {
                    jsonObject.put("dstIp", IpUtils.longToIPv4(operationBehavior.getDstIp()));
                } else {
                    jsonObject.put("dstIp", "");
                }
                if (StringUtils.isNotBlank(operationBehavior.getDstCode())) {
                    jsonObject.put("dstCode", operationBehavior.getDstCode());
                } else {
                    jsonObject.put("dstCode", "");
                }
                if (StringUtils.isNotBlank(operationBehavior.getDstDeviceName())) {
                    jsonObject.put("dstDeviceName", operationBehavior.getDstDeviceName());
                } else {
                    jsonObject.put("dstDeviceName", "");
                }

                if (StringUtils.isNotBlank(operationBehavior.getAction())) {
                    jsonObject.put("action", ActionType.getLabel(operationBehavior.getAction()));
                } else {
                    jsonObject.put("action", "");
                }

                if (StringUtils.isNotBlank(operationBehavior.getActionDetail())) {
                    jsonObject.put("actionDetail", ActionDetailType.getLabel(operationBehavior.getActionDetail()));
                } else {
                    jsonObject.put("actionDetail", "");
                }

//                if (operationBehavior.getUpFlow() != null) {
//                    jsonObject.put("upFlow", FlowUtil.setFlow(operationBehavior.getUpFlow()));
//                } else {
//                    jsonObject.put("upFlow", "0");
//                }
//                if (operationBehavior.getDownFlow() != null) {
//                    jsonObject.put("downFlow", FlowUtil.setFlow(operationBehavior.getDownFlow()));
//                } else {
//                    jsonObject.put("downFlow", "0");
//                }
                if (StringUtils.isNotBlank(operationBehavior.getUsername())) {
                    jsonObject.put("username", operationBehavior.getUsername());
                } else {
                    jsonObject.put("username", "");
                }
                if (operationBehavior.getStamp() != null) {
                    jsonObject.put("stamp", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, operationBehavior.getStamp()));
                } else {
                    jsonObject.put("stamp", "");
                }
//                if (StringUtils.isNotBlank(operationBehavior.getResult())) {
//                    if ("1".equals(operationBehavior.getResult())) {
//                        jsonObject.put("result", "成功");
//                    } else if ("0".equals(operationBehavior.getResult())) {
//                        jsonObject.put("result", "失败");
//                    } else {
//                        jsonObject.put("result", "未知");
//                    }
//                } else {
//                    jsonObject.put("result", "未知");
//                }
                if(operationBehavior.getPlatformName()!=null){
                    jsonObject.put("platformName", operationBehavior.getPlatformName());
                }else{
                    jsonObject.put("platformName", "本机");
                }
                reportList.add(jsonObject);
            }
        }
        return reportList;
    }


    @Override
    @OperScope(operAlias = "bo")
    public PageResult getVisitedRateRelatedOperation(OperationBehavior operationBehavior,List<String> cameraCodeList ) throws Exception {

        operationBehavior.setNum(operationBehavior.getSize() * (operationBehavior.getNum() - 1));

        CompletableFuture<Integer> totalFuter = CompletableFuture.supplyAsync(() -> operationBehaviorMapper.getVisitedRateRelatedOperationCount(cameraCodeList,operationBehavior));
        //操作行为列表
        CompletableFuture<List<OperationBehavior>> operationBehaviorsFuture = CompletableFuture.supplyAsync(() -> operationBehaviorMapper.getVisitedRateRelatedOperation(cameraCodeList, operationBehavior));

        return PageResult.success(operationBehaviorsFuture.get(), operationBehavior.getNum(), operationBehavior.getSize(), totalFuter.get());

    }

    @Override
    public OperationBehavior getOperationBehaviorById(Long id) {
        return operationBehaviorMapper.findById(id);
    }

    @Override
    public OperationBehavior getOperationBehaviorByUuid(String uuid) {
        return operationBehaviorMapper.findByUuid(uuid);
    }

    @Override
    public List<OperationBehavior> getOperationListByUuids(int pageNum, int pageSize, JSONObject filters) {
        return operationBehaviorMapper.operListByUuids(filters);
    }

    @Override
    public Integer selectOperationByTimeAndName(JSONObject filters) {
        return operationBehaviorMapper.selectOperationByTimeAndName(filters);
    }

    @Override
    public List<Long> selectOperationIpsByTimeAndName(JSONObject filters) {
        return operationBehaviorMapper.selectOperationIpsByTimeAndName(filters);
    }

    @Override
    public List<String> selectOperationNamesByTime(JSONObject filters) {
        return operationBehaviorMapper.selectOperationNamesByTime(filters);
    }

    @Override
    public Long getOperNumByTime(String startDate, String endDate) {
        return operationBehaviorMapper.getOperNumByTime(startDate, endDate);
    }
}
