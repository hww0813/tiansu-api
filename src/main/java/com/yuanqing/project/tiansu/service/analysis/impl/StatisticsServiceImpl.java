package com.yuanqing.project.tiansu.service.analysis.impl;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.enums.ActionDetailType;
import com.yuanqing.common.enums.ActionType;
import com.yuanqing.common.utils.DateUtils;
import com.yuanqing.common.utils.DoubleUtils;
import com.yuanqing.common.utils.IdUtils;
import com.yuanqing.common.utils.StringUtils;
import com.yuanqing.common.utils.bean.BeanUtils;
import com.yuanqing.common.utils.ip.IpUtils;
import com.yuanqing.framework.web.domain.AjaxResult;
import com.yuanqing.framework.web.domain.BaseEntity;
import com.yuanqing.project.tiansu.domain.analysis.CameraVisit;
import com.yuanqing.project.tiansu.domain.analysis.Statistics;
import com.yuanqing.project.tiansu.domain.analysis.TerminalVisit;
import com.yuanqing.project.tiansu.domain.analysis.VisitedRate;
import com.yuanqing.project.tiansu.domain.analysis.dto.CameraVisitDto;
import com.yuanqing.project.tiansu.domain.assets.Camera;
import com.yuanqing.project.tiansu.domain.assets.ClientTerminal;
import com.yuanqing.project.tiansu.domain.macs.MacsRegion;
import com.yuanqing.project.tiansu.domain.operation.OperationBehavior;
import com.yuanqing.project.tiansu.mapper.analysis.StatisticsMapper;
import com.yuanqing.project.tiansu.mapper.operation.OperationBehaviorMapper;
import com.yuanqing.project.tiansu.service.analysis.IStatisticsService;
import com.yuanqing.project.tiansu.service.assets.ICameraService;
import com.yuanqing.project.tiansu.service.assets.IClientTerminalService;
import com.yuanqing.project.tiansu.service.macs.IMacsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xucan
 * @version 1.0
 * @Date 2021-03-01 14:44
 */

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    @Autowired
    private IMacsConfigService macsConfigService;

    @Autowired
    private ICameraService cameraService;

    @Autowired
    private IClientTerminalService terminalService;

    @Autowired
    private StatisticsMapper statisticsMapper;

    @Resource
    private OperationBehaviorMapper operationBehaviorMapper;

    @Autowired
    private IStatisticsService statisticsService;

    @Override
    public List<Statistics> getList(Statistics statistics) {

        return statisticsMapper.getList(statistics);
    }

    @Override
    public List<Statistics> getClientUserList(Statistics statistics) {

        return statisticsMapper.getClientUserList(statistics);
    }

    @Override
    public List<JSONObject> getClientList(Statistics statistics) {
        List<Statistics> statisticsList = statisticsMapper.getClientUserList(statistics);
        List<JSONObject> list = new ArrayList<>();
        statisticsList.stream().forEach(h -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("srcIp", IpUtils.longToIPv4(h.getSrcIp()));
            if (!StringUtils.isEmpty(h.getUsername())) {
                jsonObject.put("username", h.getUsername());
            } else {
                jsonObject.put("username", "");
            }
            jsonObject.put("count", h.getCount());
            list.add(jsonObject);
        });
        return list;
    }


    /**
     * 1.??????regionID ??????????????????
     * 2.??????????????????????????????
     * 3.???????????????????????????
     * 4.???????????????????????????????????????
     *
     * @param regionId ????????????
     * @param dateType ????????????
     * @return
     */
    @Override
    public List<JSONObject> getVisitedRate(String regionId, String dateType) {

        JSONObject filter = new JSONObject();

        //???????????????
        switch (dateType) {
            default:
            case "day":
                filter = DateUtils.getDayTime();
                break;
            case "week":
                filter = DateUtils.getWeek();
                break;
            case "month":
                filter = DateUtils.getMonth();
                break;
            case "all":
                filter.put("startDate", null);
                filter.put("endDate", null);
                break;
        }

        MacsRegion region = macsConfigService.getRegion(regionId);

        List<JSONObject> rateList = new ArrayList<>();

        if (region != null) {
            log.info("??????????????????-->??????????????????,???????????????:{} ???????????????:{}", region.getId(), region.getName());

            //?????????????????? ????????????????????????   lowerRegion Map<String,List<String>>
            List<MacsRegion> cityRegion = macsConfigService.getLowerRegion(region.getId());

            if (CollectionUtils.isEmpty(cityRegion)) {

                log.error("??????????????????????????????,???????????????:{} ???????????????:{}", region.getId(), region.getName());
                return null;

            } else {
                log.info("1.????????????????????????????????????,???????????????:{}", cityRegion.toString());
                //??????lowerRegion Map<String,List<String>> ???????????????sql ????????????????????? ????????????????????????????????? Map.String?????????
                JSONObject finalFilter = filter;
                cityRegion.stream().forEach(f -> {

                    //?????????????????????????????????  eg:f??????????????????????????????????????????
                    List<MacsRegion> areaRegion = macsConfigService.getLowerRegion(f.getId());
                    areaRegion.add(f);
                    List<String> list = areaRegion.stream().map(a -> a.getId()).collect(Collectors.toList());
                    log.info("2.????????????????????????????????????,???????????????:{}", list);

                    VisitedRate visitedRate = statisticsMapper.visitedRate(finalFilter, list);

                    //?????????????????????????????????????????????0
                    JSONObject visited = new JSONObject();
                    visited.put("cityName", f.getName());
                    visited.put("cityCode", f.getId());
                    //???????????????
                    visited.put("cameraCnt", visitedRate.getAllCount() == null ? 0 : visitedRate.getAllCount());
                    //?????????
                    visited.put("clientCnt", visitedRate.getTerminalCnt() == null ? 0 : visitedRate.getTerminalCnt());
                    //?????????????????????
                    visited.put("visitedCnt", visitedRate.getVisitedCamera() == null ? 0 : visitedRate.getVisitedCamera());
                    //????????????
                    visited.put("visitCnt", visitedRate.getVisitedCnt() == null ? 0 : visitedRate.getVisitedCnt());
                    Double rate = 0D;
                    if (visitedRate.getAllCount() != 0) {
                        rate = ((double) visitedRate.getVisitedCamera() / (double) visitedRate.getAllCount());
                    }

                    visited.put("rate", DoubleUtils.roundOff((rate * 100), 2) + "%");
                    rateList.add(visited);

                });
                return rateList;
            }
        }
        return null;
    }

    @Override
    public List<TerminalVisit> getTerminalVisit(TerminalVisit terminalVisit, String orderStr) {

        return statisticsMapper.getTerminalVisit(terminalVisit, orderStr);
    }


    /**
     * @param cameraList ???????????????
     * @return ????????????????????????
     */
    @Override
    public List<CameraVisitDto> getCameraVisit(List<Camera> cameraList, CameraVisit cameraVisit, String orderStr) {

        List<String> deviceCodeList = null;
        List<CameraVisit> cameraVisitList = new ArrayList<>();

        //???????????????list ????????????????????????????????????????????????????????????????????????
        if (!CollectionUtils.isEmpty(cameraList)) {
            deviceCodeList = cameraList.stream().map(f -> f.getDeviceCode()).collect(Collectors.toList());
            cameraVisitList = statisticsMapper.getCameraVisit(deviceCodeList, cameraVisit, orderStr);
        }

        Map<String, CameraVisitDto> deviceCodeMap = new HashMap<>();

        // ???????????????????????????????????????????????????????????????????????????list??????????????????????????????
        cameraVisitList.stream().forEach(f -> {
            if (!CollectionUtils.isEmpty(cameraList)) {
                cameraList.stream().forEach(h -> {
                    if (f.getDeviceCode().equals(h.getDeviceCode())) {
                        f.setDeviceName(h.getDeviceName());
                        f.setIpAddress(h.getIpAddress());
                        f.setRegionName(h.getRegionName());
                    }
                });
            } else {
                Camera camera = cameraService.findByCode(f.getDeviceCode());
                f.setDeviceName(camera.getDeviceName());
                f.setIpAddress(camera.getIpAddress());
                f.setRegionName(camera.getRegionName());
            }

            //?????????????????????????????????????????????????????????????????????????????????
            f.setId(IdUtils.simpleUUID());

            //?????????????????????????????????????????????????????????key,?????????????????????????????????dto?????????children??????
            CameraVisitDto cameraVisitDto;
            if (!deviceCodeMap.containsKey(f.getDeviceCode())) {
                cameraVisitDto = new CameraVisitDto();
                BeanUtils.copyProperties(f, cameraVisitDto);
                deviceCodeMap.put(f.getDeviceCode(), cameraVisitDto);
            } else {
                deviceCodeMap.get(f.getDeviceCode()).getChildren().add(f);
            }

        });

        //?????????????????????Map ?????????List
        Set<String> keys = deviceCodeMap.keySet();

        List<CameraVisitDto> cameraVisitDtoList = new ArrayList<>();

        keys.stream().forEach(f -> {
            CameraVisitDto cameraVisitDto = deviceCodeMap.get(f);

            cameraVisitDtoList.add(cameraVisitDto);
        });

        return cameraVisitDtoList;
    }

    /**
     * @param cameraList ??????????????????????????????
     * @return ???????????????
     */
    @Override
    public List<String> getCameraVisited(List<Camera> cameraList, CameraVisit cameraVisit) {

        if (CollectionUtils.isEmpty(cameraList)) {
            log.error("cameraList???????????????device_code?????????????????????????????????");
            return null;
        }

        List<String> deviceCodeList = cameraList.stream().map(f -> f.getDeviceCode()).collect(Collectors.toList());

        List<String> cameraVisitList = statisticsMapper.getCameraVisited(deviceCodeList, cameraVisit);

        return cameraVisitList;
    }

    @Override
    public List<JSONObject> associateCameraInfo(List<OperationBehavior> operationBehaviorList) {

        List<Long> cameraIdList = operationBehaviorList.stream().map(f -> f.getCameraId()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(cameraIdList)) {
            log.error("cameraIdList??????");
            return null;
        }

        List<Camera> cameraList = cameraService.batchGetCameraById(cameraIdList);

        if (CollectionUtils.isEmpty(cameraList)) {
            log.error("??????cameraList??????");
            return null;
        }

        List<JSONObject> terminalVisitedCameraList = new ArrayList<>();

        operationBehaviorList.stream().forEach(f -> {
            cameraList.stream().forEach(h -> {
                if (f.getCameraId().longValue() == h.getId().longValue()) {
                    JSONObject j = new JSONObject();
                    j.put("dstCode", h.getDeviceCode());
                    j.put("dstIp", h.getIpAddress());
                    j.put("dstDeviceName", h.getDeviceName());
                    j.put("dstPort", h.getDomainPort());
                    j.put("action", f.getAction());
                    j.put("actionDetail", f.getActionDetail());
                    j.put("stamp", f.getStamp());
                    j.put("callId", f.getCallId());
                    j.put("id", f.getId());
                    terminalVisitedCameraList.add(j);
                }
            });
        });

        return terminalVisitedCameraList;
    }


    @Override
    public List<JSONObject> associateTerminalInfo(List<OperationBehavior> operationBehaviorList) {

        List<Long> terminalIpList = operationBehaviorList.stream().map(f -> f.getSrcIp()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(terminalIpList)) {
            log.error("terminalIpList??????");
            return null;
        }

        List<ClientTerminal> terminalList = terminalService.getTerminalByIpList(terminalIpList, new ClientTerminal());

        if (CollectionUtils.isEmpty(terminalList)) {
            log.error("??????terminalList??????");
            return null;
        }

        List<JSONObject> cameraVisitedTerminalList = new ArrayList<>();

        operationBehaviorList.stream().forEach(f -> {
            terminalList.stream().forEach(h -> {
                if (f.getSrcIp().longValue() == h.getIpAddress().longValue()) {
                    JSONObject j = new JSONObject();
                    j.put("srcIp", h.getIpAddress());
                    j.put("srcPort", h.getDomainPort());
                    j.put("action", f.getAction());
                    j.put("actionDetail", f.getActionDetail());
                    j.put("stamp", f.getStamp());
                    j.put("username", f.getUsername());
                    j.put("callId", f.getCallId());
                    j.put("id", f.getId());
                    cameraVisitedTerminalList.add(j);
                }
            });
        });

        return cameraVisitedTerminalList;
    }

    @Override
    public List<Long> getTerminalVisited(List<Camera> cameraList, BaseEntity baseEntity) {

        if (CollectionUtils.isEmpty(cameraList)) {
            log.error("cameraList??????");
            return null;
        }

        List<String> deviceCodeList = cameraList.stream().map(f -> f.getDeviceCode()).collect(Collectors.toList());

        List<Long> terminalIpList = statisticsMapper.getTerminalVisited(deviceCodeList, baseEntity);

        return terminalIpList;
    }

    @Override
    public List<JSONObject> getClientVisitToReport(JSONObject filters) {
        List<JSONObject> clientVisitObjectList = new ArrayList<>();

        TerminalVisit condTerminalVisit = new TerminalVisit();
        condTerminalVisit.setstartDate(filters.getString("startDate"));
        condTerminalVisit.setendDate(filters.getString("endDate"));
        if (StringUtils.isNotEmpty(filters.getString("clientIp"))) {
            condTerminalVisit.setIpAddress(IpUtils.ipToLong(filters.getString("clientIp")));
        }
        condTerminalVisit.setUsername(filters.getString("user"));
        if (StringUtils.isNotEmpty(filters.getString("action"))) {
            condTerminalVisit.setAction(Integer.valueOf(filters.getString("action")));
        }
        // TODO: ????????????????????????????????????
        List<TerminalVisit> terminalVisitList = statisticsMapper.getTerminalVisit(condTerminalVisit, null);
        if (!CollectionUtils.isEmpty(terminalVisitList)) {
            for (TerminalVisit terminalVisit : terminalVisitList) {
                JSONObject jsonObject = new JSONObject();
                // TODO: ????????????????????????
                jsonObject.put("CLIENT_CODE", "");
                if (terminalVisit.getIpAddress() != null) {
                    jsonObject.put("CLIENT_IP", IpUtils.longToIPv4(terminalVisit.getIpAddress()));
                } else {
                    jsonObject.put("CLIENT_IP", "");
                }
                if (StringUtils.isNotEmpty(terminalVisit.getUsername())) {
                    jsonObject.put("USERNAME", terminalVisit.getUsername());
                } else {
                    jsonObject.put("USERNAME", "");
                }
                Integer action = terminalVisit.getAction();
                if (0 == action) {
                    jsonObject.put("ACTION", "????????????");
                } else if (1 == action) {
                    jsonObject.put("ACTION", "????????????");
                } else if (2 == action) {
                    jsonObject.put("ACTION", "????????????");
                } else if (3 == action) {
                    jsonObject.put("ACTION", "????????????");
                } else if (4 == action) {
                    jsonObject.put("ACTION", "??????");
                } else if (5 == action) {
                    jsonObject.put("ACTION", "??????");
                } else if (6 == action) {
                    jsonObject.put("ACTION", "????????????");
                } else if (7 == action) {
                    jsonObject.put("ACTION", "??????");
                } else if (8 == action) {
                    jsonObject.put("ACTION", "??????");
                } else if (9 == action) {
                    jsonObject.put("ACTION", "??????");
                } else if (10 == action) {
                    jsonObject.put("ACTION", "PLAY_DEFEAT");
                } else {
                    jsonObject.put("ACTION", "OTHER");  // ??????
                }

                Long visitCnt = terminalVisit.getVisitCnt();
                if (visitCnt != null) {
                    jsonObject.put("VISIT_CNT", BigDecimal.valueOf(visitCnt));
                } else {
                    jsonObject.put("VISIT_CNT", BigDecimal.valueOf(0L));
                }

                Long cameraCnt = terminalVisit.getCameraCnt();
                if (cameraCnt != null) {
                    jsonObject.put("CAMERA_CNT", BigDecimal.valueOf(cameraCnt));
                } else {
                    jsonObject.put("CAMERA_CNT", BigDecimal.valueOf(0L));
                }

                clientVisitObjectList.add(jsonObject);
            }
        }
        return clientVisitObjectList;
    }

    @Override
    public List<JSONObject> getClientVisitCntToReport(OperationBehavior operationBehavior) {
        List<OperationBehavior> operList = operationBehaviorMapper.getVisitedRelatedOperation(operationBehavior);

        List<JSONObject> list = new ArrayList<>();

        for (OperationBehavior oper : operList) {
            JSONObject jsonObject = new JSONObject();
            if (StringUtils.isNotEmpty(oper.getDstCode())) {
                jsonObject.put("dstCode", oper.getDstCode());
            } else {
                jsonObject.put("dstCode", "");
            }
            if (StringUtils.isNotEmpty(oper.getDstDeviceName())) {
                jsonObject.put("dstDeviceName", oper.getDstDeviceName());
            } else {
                jsonObject.put("dstDeviceName", "");
            }
            if (oper.getDstIp() != null) {
                jsonObject.put("dstIp", IpUtils.longToIPv4(oper.getDstIp()));
            } else {
                jsonObject.put("dstIp", "");
            }
            if (oper.getDstPort() != null) {
                jsonObject.put("dstPort", oper.getDstPort());
            } else {
                jsonObject.put("dstPort", "");
            }
            if (StringUtils.isNotBlank(oper.getAction())) {
                jsonObject.put("action", ActionType.getLabel(oper.getAction()));
            } else {
                jsonObject.put("action", "");
            }
            if (StringUtils.isNotBlank(oper.getActionDetail())) {
                jsonObject.put("actionDetail", ActionDetailType.getLabel(oper.getActionDetail()));
            } else {
                jsonObject.put("actionDetail", "");
            }
            if (oper.getStamp() != null) {
                jsonObject.put("stamp", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, oper.getStamp()));
            } else {
                jsonObject.put("stamp", "");
            }
            list.add(jsonObject);
        }

        return list;
    }

    @Override
    public List<JSONObject> getClientVisitRelateCameraToReport(JSONObject filters) {
        Statistics condStatistics = new Statistics();
        condStatistics.setAction(filters.getInteger("action"));
        condStatistics.setDstCode(filters.getString("cameraCode"));
        condStatistics.setUsername(filters.getString("username"));
        condStatistics.setSrcIp(IpUtils.ipToLong(filters.getString("clientIp")));
        condStatistics.setstartDate(filters.getString("startDate"));
        condStatistics.setendDate(filters.getString("endDate"));

        Camera condCamera = new Camera();
        condCamera.setDeviceName(filters.getString("cameraName"));
        condCamera.setIpAddress(IpUtils.ipToLong(filters.getString("cameraIp")));
        condCamera.setRegion(filters.getInteger("region"));

        List<Statistics> statisticsList = this.getList(condStatistics);

        List<String> cameraCodeList = statisticsList.stream().map(f -> f.getDstCode()).collect(Collectors.toList());

        List<Camera> cameraList = cameraService.batchGetCameraByCode(cameraCodeList, condCamera);

//        List<CameraVisit> cameraVisitList = new ArrayList<>();

        List<JSONObject> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(cameraList)) {
            cameraList.stream().forEach(f -> {

                Long sum = statisticsList.stream()
                        .filter(h -> h.getDstCode().equals(f.getDeviceCode()))
                        .mapToLong(h -> h.getCount())
                        .sum();

                JSONObject jsonObject = new JSONObject();
                if (f.getDeviceCode() != null) {
                    jsonObject.put("CAMERA_CODE", f.getDeviceCode());
                } else {
                    jsonObject.put("CAMERA_CODE", "");
                }
                if (f.getDeviceName() != null) {
                    jsonObject.put("CAMERA_NAME", f.getDeviceName());
                } else {
                    jsonObject.put("CAMERA_NAME", "");
                }
                if (f.getIpAddress() != null) {
                    jsonObject.put("CAMERA_IP", IpUtils.longToIPv4(f.getIpAddress()));
                } else {
                    jsonObject.put("CAMERA_IP", "");
                }
                if (f.getRegionName() != null) {
                    jsonObject.put("CAMERA_REGION", f.getRegionName());
                } else {
                    jsonObject.put("CAMERA_REGION", "");
                }
                if (sum != null) {
                    jsonObject.put("VISITED_CNT", sum.toString());
                } else {
                    jsonObject.put("VISITED_CNT", "0");
                }

                list.add(jsonObject);
            });
        }


//        List<JSONObject> list = clientVisitMapper.getClientVisitRelatedCameraList(filters);
//        for (JSONObject jsonObject : list) {
//            if (jsonObject.getString("CAMERA_NAME") == null) jsonObject.put("CAMERA_NAME", "");
//        }
        return list;
    }

    @Override
    public List<JSONObject> getCameraVisitedToReport(CameraVisit cameraVis, Camera camera, String orderStr) {

        List<Camera> cameraList = null;

        cameraList = cameraService.getActiveList(camera);

        macsConfigService.setLowerRegionByCamera(cameraList);

        List<CameraVisitDto> cameraVisitList = getCameraVisit(cameraList, cameraVis, orderStr);


        List<JSONObject> cameraVisitedObjectList = new ArrayList<>();


        if (!CollectionUtils.isEmpty(cameraVisitList)) {
            for (CameraVisitDto cameraVisit : cameraVisitList) {
                JSONObject jsonObject = new JSONObject();

                if (StringUtils.isNotEmpty(cameraVisit.getDeviceCode())) {
                    jsonObject.put("CAMERA_CODE", cameraVisit.getDeviceCode());
                } else {
                    jsonObject.put("CAMERA_CODE", "");
                }
                if (cameraVisit.getIpAddress() != null) {
                    jsonObject.put("CAMERA_IP", IpUtils.longToIPv4(cameraVisit.getIpAddress()));
                } else {
                    jsonObject.put("CAMERA_IP", "");
                }
                if (StringUtils.isNotEmpty(cameraVisit.getDeviceName())) {
                    jsonObject.put("CAMERA_NAME", cameraVisit.getDeviceName());
                } else {
                    jsonObject.put("CAMERA_NAME", "");
                }
                if (StringUtils.isNotEmpty(cameraVisit.getRegionName())) {
                    jsonObject.put("CAMERA_REGION", cameraVisit.getRegionName());
                } else {
                    jsonObject.put("CAMERA_REGION", "");
                }
                Integer action = cameraVisit.getAction();
                if (action != null) {
                    if (action == 0) {
                        jsonObject.put("ACTION", "????????????");
                    } else if (action == 1) {
                        jsonObject.put("ACTION", "????????????");
                    } else if (action == 2) {
                        jsonObject.put("ACTION", "????????????");
                    } else if (action == 3) {
                        jsonObject.put("ACTION", "????????????");
                    } else if (action == 4) {
                        jsonObject.put("ACTION", "??????");
                    } else if (action == 5) {
                        jsonObject.put("ACTION", "??????");
                    } else if (action == 6) {
                        jsonObject.put("ACTION", "????????????");
                    } else if (action == 7) {
                        jsonObject.put("ACTION", "??????");
                    } else if (action == 8) {
                        jsonObject.put("ACTION", "??????");
                    } else if (action == 9) {
                        jsonObject.put("ACTION", "??????");
                    } else if (action == 10) {
                        jsonObject.put("ACTION", "PLAY_DEFEAT");  // ????????????
                    } else {
                        jsonObject.put("ACTION", "OTHER");  // ??????
                    }
                } else {
                    jsonObject.put("ACTION", "OTHER");  // ??????
                }

                Long visitCnt = cameraVisit.getVisitCnt();
                if (visitCnt != null) {
                    jsonObject.put("VISITED_CNT", BigDecimal.valueOf(visitCnt));
                } else {
                    jsonObject.put("VISITED_CNT", BigDecimal.valueOf(0L));
                }

                Long terminalCnt = cameraVisit.getTerminalCnt();
                if (terminalCnt != null) {
                    jsonObject.put("CLIENT_CNT", BigDecimal.valueOf(terminalCnt));
                } else {
                    jsonObject.put("CLIENT_CNT", BigDecimal.valueOf(0L));
                }

                cameraVisitedObjectList.add(jsonObject);
            }
        }
        return cameraVisitedObjectList;
    }

    @Override
    public List<JSONObject> getCameraVisitedCntToReport(OperationBehavior operationBehavior) {
        if (StringUtils.isNotBlank(operationBehavior.getOrderType()) && StringUtils.isNotBlank(operationBehavior.getOrderValue())) {
            operationBehavior.setOrderType(operationBehavior.getOrderValue() + " " + operationBehavior.getOrderType());
        }
        //???????????????
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
        Integer count = operationBehaviorMapper.quertyOperationBehaviorCount(operationBehavior);
        operationBehavior.setNum(0);
        operationBehavior.setSize(count);
        List<OperationBehavior> operList = operationBehaviorMapper.getList(operationBehavior);
        List<JSONObject> list = statisticsService.associateTerminalInfo(operList);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        list.stream().forEach(f -> {
            f.put("srcIp", IpUtils.longToIPv4(f.getLong("srcIp")));
            f.put("action", ActionType.getLabel(f.getString("action")));
            f.put("actionDetail", ActionDetailType.getLabel(f.getString("actionDetail")));
            f.put("stamp", formatter.format(f.getTimestamp("stamp")));
        });

        return list;
    }


}
