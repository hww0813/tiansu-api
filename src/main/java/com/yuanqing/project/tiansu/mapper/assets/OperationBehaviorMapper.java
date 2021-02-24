package com.yuanqing.project.tiansu.mapper.assets;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.framework.web.mapper.BaseMapper;
import com.yuanqing.project.tiansu.domain.assets.SessionClient;
import com.yuanqing.project.tiansu.domain.operation.OperationBehavior;
import com.yuanqing.project.tiansu.domain.operation.OperationBehaviorSearch;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

/**
 * 操作行为
 *
 * @author jqchu
 * @version 1.0
 * @since 2017.12.17
 */
@Repository
public interface OperationBehaviorMapper  {

    public List<OperationBehavior> queryOperationBehaviorList(OperationBehavior qperationBehavior);

    public List<Long> getCameraidListBySession(Long sessionId);

    public Integer quertyOperationBehaviorCount(OperationBehavior qperationBehavior);

    public List<OperationBehavior> getRealTimeBehaviorList(OperationBehaviorSearch operationBehaviorSearch);

    public int updateOperationBehavior(OperationBehavior operationBehavior);

    List<HashMap> cameraAnalysisDetail(JSONObject filters);

    Integer cameraAnalysisCount(JSONObject filters);

    Integer queryRawCount(Object object);

}
