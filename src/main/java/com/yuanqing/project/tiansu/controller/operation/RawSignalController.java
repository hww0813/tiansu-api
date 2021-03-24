package com.yuanqing.project.tiansu.controller.operation;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.yuanqing.common.utils.StringUtils;
import com.yuanqing.common.utils.ip.IpUtils;
import com.yuanqing.framework.web.controller.BaseController;
import com.yuanqing.framework.web.domain.PageResult;
import com.yuanqing.project.tiansu.domain.operation.RawSignal;
import com.yuanqing.project.tiansu.service.operation.IRawSignalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

/**
 * @author Dong.Chao
 * @Classname RawSignalController
 * @Description
 * @Date 2021/2/25 16:19
 * @Version V1.0
 */
@RestController
@RequestMapping(value = "/api/video/rawsignal")
@CrossOrigin
@Api(value = "原始信令")
public class RawSignalController extends BaseController {

    @Autowired
    private IRawSignalService IRawSignalService;

    @GetMapping("/list")
    @ApiOperation(value = "获取原始信令列表", httpMethod = "GET")
    public PageResult getAll(@RequestParam(value = "pageNum", defaultValue = "1") int num,
                             @RequestParam(value = "pageSize", defaultValue = "20") int size,
                             @RequestParam(value = "connectType", required = false) String connectType,
                             @RequestParam(value = "srcIp", required = false) String srcIp,
                             @RequestParam(value = "dstIp", required = false) String dstIp,
                             @RequestParam(required = false) String orderType,
                             @RequestParam(required = false) String orderValue, RawSignal rawSignal) {
        rawSignal.setNum(num);
        rawSignal.setSize(size);
        rawSignal.setSrcIp(IpUtils.ipToLong(srcIp));
        rawSignal.setDstIp(IpUtils.ipToLong(dstIp));
        if (StringUtils.isNotBlank(orderValue) && StringUtils.isNotBlank(orderType)) {
            rawSignal.setOrderType(orderValue + " " + orderType);
        }
        try {
            return IRawSignalService.queryRawSignals(rawSignal);
        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.error("请求原始信令列表错误");
        }

    }

    @GetMapping("/findByCallId")
    @ApiOperation(value = "获取原始信令列表", httpMethod = "GET")
    public PageResult findByCallId(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                   @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                                   @RequestParam(value = "srcIp", required = false) String srcIp,
                                   @RequestParam(value = "dstIp", required = false) String dstIp,RawSignal rawSignal) {


        rawSignal.setNum(pageNum);
        rawSignal.setSize(pageSize);
        rawSignal.setSrcIp(IpUtils.ipToLong(srcIp));
        rawSignal.setDstIp(IpUtils.ipToLong(dstIp));

        try {
            return IRawSignalService.queryRawSignals(rawSignal);
        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.error("请求原始信令列表错误");
        }
    }
}
