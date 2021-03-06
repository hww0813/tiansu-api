package com.yuanqing.project.tiansu.service.operation;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.framework.web.domain.AjaxResult;
import com.yuanqing.framework.web.domain.PageResult;
import com.yuanqing.project.tiansu.domain.operation.OperationBehavior;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Dong.Chao
 * @Classname OperationBehaviorService
 * @Description
 * @Date 2021/2/25 11:37
 * @Version V1.0
 */
public interface IOperationBehaviorService {

    PageResult queryOperationList(OperationBehavior operationBehavior) throws Exception;

    AjaxResult getCharts(LocalDate startDate,LocalDate endDate,String action,String sort,String type);

    List<JSONObject> getAllToReport(OperationBehavior oper);

    /**
     * 根据摄像头编号和过滤条件查询操作行为
     * @param cameraCodeList 摄像头编号集合
     * @param operationBehavior 过滤条件集合
     * @return
     */
    PageResult getVisitedRateRelatedOperation(@Param("filter") OperationBehavior operationBehavior,
                                              @Param("list") List<String> cameraCodeList) throws ExecutionException, InterruptedException, Exception;

    OperationBehavior getOperationBehaviorById(Long id);

    OperationBehavior getOperationBehaviorByUuid(String uuid);

    List<OperationBehavior> getOperationListByUuids(int pageNum, int pageSize, JSONObject filters);

    /**
     * 获取开始时间到结束时间段内用户名的操作数
     * @return
     */
    Integer selectOperationByTimeAndName(JSONObject filters);

    List<Long> selectOperationIpsByTimeAndName(JSONObject filters);

    List<String> selectOperationNamesByTime(JSONObject filters);

    /**
     * 获取时间段内所有操作行为总数
     * @param startDate
     * @param endDate
     * @return
     */
    Long getOperNumByTime(String startDate, String endDate);
}
