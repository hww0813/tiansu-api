package com.yuanqing.project.tiansu.mapper.operation;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.framework.web.mapper.BaseMapper;
import com.yuanqing.project.tiansu.domain.operation.OperationBehaviorSession;

import java.util.List;

/**
 * @author Lmm
 * @Classname OperationBehaviorSessionMapper
 * @Description
 * @Date 2021/2/25 15:29
 * @Version V1.0
 */
public interface OperationBehaviorSessionMapper  extends BaseMapper<OperationBehaviorSession,Long> {


    Integer getCount(OperationBehaviorSession operationBehaviorSession);


    /**
     * 获取操作行为分类
     * @param filters
     * @return
     */
    List<JSONObject> getBehaviorCategory(JSONObject filters);
}
