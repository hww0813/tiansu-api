package com.yuanqing.project.tiansu.service.assets.impl;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.constant.Constants;
import com.yuanqing.common.enums.DeviceType;
import com.yuanqing.common.enums.SaveType;
import com.yuanqing.common.exception.CustomException;
import com.yuanqing.common.queue.ClientTerminalMap;
import com.yuanqing.common.utils.DateUtils;
import com.yuanqing.common.utils.StringUtils;
import com.yuanqing.common.utils.ip.IpUtils;
import com.yuanqing.project.tiansu.domain.assets.Client;
import com.yuanqing.project.tiansu.domain.assets.ClientTerminal;
import com.yuanqing.project.tiansu.domain.assets.ServerTree;
import com.yuanqing.project.tiansu.domain.assets.dto.ClientTerminalDto;
import com.yuanqing.project.tiansu.mapper.assets.ClientMapper;
import com.yuanqing.project.tiansu.mapper.assets.ClientTerminalMapper;
import com.yuanqing.project.tiansu.service.assets.IClientService;
import com.yuanqing.project.tiansu.service.assets.IClientTerminalService;
import com.yuanqing.project.tiansu.service.assets.IServerTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xucan on 2021-01-18 16:29
 *
 * @author xucan
 */

@Service
public class ClientTerminalServiceImpl implements IClientTerminalService {

    private static final Logger log = LoggerFactory.getLogger(ClientTerminalServiceImpl.class);

    @Autowired
    private ClientTerminalMapper clientTerminalMapper;

    @Autowired
    private IServerTreeService serverTreeService;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private IClientService clientService;

    @Override
    public boolean changStatus(String[] ids) {
        return clientTerminalMapper.changStatus(ids);
    }

    @Override
    public List<ClientTerminal> getListWithOrder(ClientTerminal clientTerminal, String orderStr) {
        return clientTerminalMapper.getListWithOrder(clientTerminal, orderStr);
    }

    @Override
    public List<JSONObject> getAllToReport(JSONObject filters) {
        ClientTerminal condClientTerminal = new ClientTerminal();
        condClientTerminal.setIpAddress(IpUtils.ipToLong(filters.getString("ipAddress")));
        condClientTerminal.setStatus(filters.getInteger("status"));
//        condClientTerminal.setDeviceCode(deviceCode);
//        condClientTerminal.setSipServerId(sipServerId);
//        condClientTerminal.setId(id);
//        condClientTerminal.setRegionId(regionId);

        String username = filters.getString("username");

        List<ClientTerminal> list = null;
        //?????? username ????????????
        if (StringUtils.isNotEmpty(username)) {
            Client client = new Client();
            client.setUsername(username);

            // ?????????????????????client??????  ?????????IP
            List<Client> clientList = clientService.getList(client);

            list = getTerminalByClientList(clientList);

        } else {
            list = getList(condClientTerminal);
        }

        //?????????
        List<ClientTerminalDto> clientTerminalDtoList = handleTerminalUserNum(list);

        List<JSONObject> reportList = new ArrayList<JSONObject>();
        if (!CollectionUtils.isEmpty(clientTerminalDtoList)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ClientTerminalDto clientTerminalDto : clientTerminalDtoList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("deviceCode", clientTerminalDto.getDeviceCode());
                if (clientTerminalDto.getIpAddress() != null) {
                    jsonObject.put("ipAddress", IpUtils.longToIPv4(clientTerminalDto.getIpAddress()));
                }
                jsonObject.put("macAddress", clientTerminalDto.getMacAddress());
                if (clientTerminalDto.getUserCnt() != null) {
                    jsonObject.put("usercnt", String.valueOf(clientTerminalDto.getUserCnt()));
                } else {
                    jsonObject.put("usercnt", "0");
                }
                if (clientTerminalDto.getStatus() != null && 0 == clientTerminalDto.getStatus()) {
                    jsonObject.put("status", "?????????");
                } else if (clientTerminalDto.getStatus() == null || 1 == clientTerminalDto.getStatus()) {
                    jsonObject.put("status", "?????????");
                }
                if (clientTerminalDto.getUpdateTime() != null) {
                    jsonObject.put("updateTime", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, clientTerminalDto.getUpdateTime()));
                }
                reportList.add(jsonObject);
            }
        }

        return reportList;
    }


    public Long insertInto(ClientTerminal clientTerminal) {
        //??????IP????????????
        ClientTerminal clientTerminal1 = clientTerminalMapper.findByIpAddress(clientTerminal.getIpAddress());
        //??????IP???????????????
        Long IP = clientTerminal.getIpAddress();
        ServerTree serverTree = serverTreeService.findOne(IP);
        if (clientTerminal1 != null) {
            //??????????????????????????????????????????????????????IP????????????????????????????????????????????????????????????????????????
            if (serverTree != null) {
                clientTerminal.setDeviceType(DeviceType.PROXY_SERVER.getValue());
            }
            //???????????????????????????????????????????????????????????????
            clientTerminalMapper.updateClientTerminal(clientTerminal);
        } else {
            //????????????????????????????????????????????????IP??????????????????????????????????????????????????????
            if (serverTree != null) {
                clientTerminal.setDeviceType(DeviceType.PROXY_SERVER.getValue());
            }
            //?????????????????????????????????????????????IP??????????????????????????????????????????????????????
//            clientTerminalMapper.insertInto(clientTerminal);
        }
        //????????????
        putClientTerminalMap(clientTerminal);
        return clientTerminal.getId();
    }

    @Override
    public List<ClientTerminal> getActiveTerminal(ClientTerminal clientTerminal) {
        return clientTerminalMapper.getActiveTerminal(clientTerminal);
    }


    @Override
    public List<ClientTerminalDto> handleTerminalUserNum(List<ClientTerminal> clientTerminalList) {

        //????????????????????????????????????dto
        List<ClientTerminalDto> dtoList = new ArrayList<>();

        if (CollectionUtils.isEmpty(clientTerminalList)) {
            log.error("??????????????????");
            return null;
        }

        //????????????IP
        List<Long> ipList = clientTerminalList.stream().map(f -> f.getIpAddress()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(ipList)) {
            log.error("????????????IP??????,????????????");

            return null;
        }

        //??????IP ??????????????????
        List<JSONObject> originalUserNum = clientMapper.getUserNumByTerminal(ipList);

        if (CollectionUtils.isEmpty(originalUserNum)) {
            log.error("??????IP??????????????????????????????");
            clientTerminalList.stream().forEach(f -> {
                ClientTerminalDto dto = doBackward(f);
                dto.setUserCnt(0);
                dtoList.add(dto);
            });
            return dtoList;
        }

        Map<Long, Integer> map = new HashMap<>();

        //?????????????????????
        originalUserNum.stream().forEach(f -> map.put(f.getLong("ip_address"), f.getInteger("userCnt")));

        if (CollectionUtils.isEmpty(map)) {
            log.error("?????????????????????????????????,map??????");
            return null;
        }

        clientTerminalList.stream().forEach(f -> {
            ClientTerminalDto dto = doBackward(f);
            Integer cnt = map.get(f.getIpAddress());
            dto.setUserCnt(cnt == null ? 0 : cnt);
            dtoList.add(dto);
        });
        return dtoList;
    }

    @Override
    public List<ClientTerminal> getTerminalByClientList(List<Client> clientList) {
        List<ClientTerminal> list = null;
        if (!CollectionUtils.isEmpty(clientList)) {
            List<Long> ipList = clientList.stream().map(f -> f.getIpAddress()).collect(Collectors.toList());
            list = clientTerminalMapper.getClientTerminalByIpList(ipList,new ClientTerminal());
        }
        return list;
    }

    @Override
    public List<ClientTerminal> getTerminalByIpList(List<Long> ipList, ClientTerminal clientTerminal ) {
        List<ClientTerminal> list = null;
        if (!CollectionUtils.isEmpty(ipList)) {
            list = clientTerminalMapper.getClientTerminalByIpList(ipList,clientTerminal);
        }
        return list;
    }

    @Override
    public ClientTerminal findByIpAddress(Long ip) {
        return clientTerminalMapper.findByIpAddress(ip);
    }


    @Override
    public Long save(ClientTerminal clientTerminal, SaveType type) {

        if (type.getCode() == SaveType.INSERT.getCode()) {
            clientTerminalMapper.insert(clientTerminal);
        } else if (type.getCode() == SaveType.UPDATE.getCode()) {
            clientTerminalMapper.update(clientTerminal);
        }
        return clientTerminal.getId();
    }

    @Override
    public Long save(ClientTerminal clientTerminal) {
        throw new CustomException("??????????????????????????????,????????????SaveType");
    }

    @Override
    public void deleteById(@Valid @NotNull(message = "??????ID?????????ID????????????") Long id) {
        clientTerminalMapper.delete(id);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long ipAddress) {
        ClientTerminal clientTerminal = findById(id);
        if (clientTerminal == null) {
            throw new RuntimeException("entity not existed");
        }

        //???????????????
        clientTerminalMapper.delete(id);
        clientMapper.deleteByIpAddress(ipAddress);
        //????????????????????????
        delClientTerminalMap(clientTerminal);
    }

    @Override
    public ClientTerminal findById(Long id) {
        return clientTerminalMapper.findById(id);
    }


    @Override
    public List<ClientTerminal> getList(ClientTerminal clientTerminal) {
        return clientTerminalMapper.getList(clientTerminal);
    }

    /**
     * ??????clientTerminal ??????clientTerminalMap??????
     *
     * @param clientTerminal
     */
    private void putClientTerminalMap(ClientTerminal clientTerminal) {
        String clientTerminalKey = String.format(Constants.TWO_FORMAT, Constants.CLIENT_TERMINAL, clientTerminal.getIpAddress());
        ClientTerminalMap.put(clientTerminalKey, clientTerminal);
    }

    /**
     * ??????clientTerminal ??????clientTerminalMap??????
     *
     * @param clientTerminal
     */
    private void delClientTerminalMap(ClientTerminal clientTerminal) {
        String clientTerminalKey = String.format(Constants.TWO_FORMAT, Constants.CLIENT_TERMINAL, clientTerminal.getIpAddress());
        ClientTerminalMap.remove(clientTerminalKey);
    }

    protected ClientTerminalDto doBackward(ClientTerminal clientTerminal) {
        ClientTerminalDto dto = new ClientTerminalDto();
        BeanUtils.copyProperties(clientTerminal, dto);
        return dto;
    }
}
