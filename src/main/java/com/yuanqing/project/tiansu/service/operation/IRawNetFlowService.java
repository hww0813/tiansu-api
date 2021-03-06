package com.yuanqing.project.tiansu.service.operation;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.project.tiansu.domain.operation.RawNetFlow;
import io.lettuce.core.dynamic.annotation.Param;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface IRawNetFlowService {

    /**
     * 查询原始流量
     *
     * @param id 原始流量ID
     * @return 原始流量
     */
    public RawNetFlow selectBusiRawNetFlowById(Long id);

    /**
     * 查询原始流量列表
     *
     * @param rawNetFlow 原始流量
     * @return 原始流量集合
     */
    public List<RawNetFlow> selectBusiRawNetFlowList(RawNetFlow rawNetFlow);

    /**
     * 新增原始流量
     *
     * @param rawNetFlow 原始流量
     * @return 结果
     */
    public int insertBusiRawNetFlow(RawNetFlow rawNetFlow);

    /**
     * 修改原始流量
     *
     * @param rawNetFlow 原始流量
     * @return 结果
     */
    public int updateBusiRawNetFlow(RawNetFlow rawNetFlow);

    /**
     * 批量删除原始流量
     *
     * @param ids 需要删除的原始流量ID
     * @return 结果
     */
    public int deleteBusiRawNetFlowByIds(Long[] ids);

    /**
     * 删除原始流量信息
     *
     * @param id 原始流量ID
     * @return 结果
     */
    public int deleteBusiRawNetFlowById(Long id);


    public  List<JSONObject>getServerFlowTrend (Long dstIp, Date startTime,Date endTime) throws ParseException;

    /**
     *
     *获取终端当天的流量趋势
     *
     */
    List<JSONObject>getClientRawFlowTrend (Long srcIp,Date startTime, Date endTime) throws ParseException;

    public List<JSONObject> getServerFlowRelationClient (RawNetFlow rawNetFlow) ;

    /**
     * 获取流量列表终端排行
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param orderType 排序字段
     * @return
     */
    public List<JSONObject> getRawClientRank(Date startTime,Date endTime,String orderType);
}
