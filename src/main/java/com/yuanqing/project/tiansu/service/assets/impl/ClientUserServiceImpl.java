package com.yuanqing.project.tiansu.service.assets.impl;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.enums.DeviceStatus;
import com.yuanqing.common.enums.SaveType;
import com.yuanqing.common.exception.CustomException;
import com.yuanqing.common.utils.DateUtils;
import com.yuanqing.common.utils.SequenceIdGenerator;
import com.yuanqing.common.utils.ip.IpUtils;
import com.yuanqing.project.tiansu.domain.assets.Client;
import com.yuanqing.project.tiansu.domain.assets.ClientUser;
import com.yuanqing.project.tiansu.domain.assets.ReadUserExcel;
import com.yuanqing.project.tiansu.domain.assets.dto.ClientUserDto;
import com.yuanqing.project.tiansu.mapper.assets.ClientMapper;
import com.yuanqing.project.tiansu.mapper.assets.ClientUserMapper;
import com.yuanqing.project.tiansu.service.assets.IClientService;
import com.yuanqing.project.tiansu.service.assets.IClientUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xucan on 2021-01-19 17:52
 *
 * @author xucan
 */

@Service
public class ClientUserServiceImpl implements IClientUserService {

    private static final Logger log = LoggerFactory.getLogger(ClientUserServiceImpl.class);

    @Autowired
    private ClientUserMapper clientUserMapper;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private IClientService clientService;

    private static SequenceIdGenerator clientUserIdGenerator = new SequenceIdGenerator(1, 1);

    @Override
    public List<JSONObject> getAllToReport(JSONObject filters) {
        ClientUser condClientUser = new ClientUser();
        condClientUser.setUsername(filters.getString("username"));
        condClientUser.setStatus(filters.getInteger("status"));

        String orderStr = filters.getString("orderStr");

        Long ipAddress = IpUtils.ipToLong(filters.getString("ipAddress"));

        List<ClientUser> list = null;
        //?????? ipAddress ????????????
        if (ipAddress != null) {
            Client client = new Client();
            client.setIpAddress(ipAddress);

            // ?????????????????????client??????  ?????????IP
            List<Client> clientList = clientService.getList(client);

            list = getClientUserByUsername(clientList, orderStr);

        } else {
            list = getList(condClientUser, orderStr);
        }

        //??????????????????
        List<ClientUserDto> clientUserDtoList = handleClientUserTerminalNum(list);

        List<JSONObject> reportList = new ArrayList<JSONObject>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ClientUserDto clientUserDto : clientUserDtoList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", clientUserDto.getUsername());

            if (clientUserDto.getTerminalCnt() != null) {
                jsonObject.put("ipCnt", String.valueOf(clientUserDto.getTerminalCnt()));
            }
            if (clientUserDto.getStatus() != null && 0 == clientUserDto.getStatus()) {
                jsonObject.put("status", "?????????");
            } else if (clientUserDto.getStatus() == null || 1 == clientUserDto.getStatus()) {
                jsonObject.put("status", "?????????");
            }
            if (clientUserDto.getUpdateTime() != null) {
                jsonObject.put("updateTime", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, clientUserDto.getUpdateTime()));
            }
            reportList.add(jsonObject);
        }
        return reportList;
    }

    @Override
    public boolean changStatus(String[] ids) {
        return clientUserMapper.changStatus(ids);
    }

    @Override
    public List<ClientUserDto> handleClientUserTerminalNum(List<ClientUser> clientUserList) {

        List<ClientUserDto> dtoList = new ArrayList<>();

        if (CollectionUtils.isEmpty(clientUserList)) {
            log.error("??????????????????");
            return null;
        }
        //?????????????????????
        List<String> usernameList = clientUserList.stream().map(f -> f.getUsername()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(usernameList)) {
            log.error("?????????????????????,????????????");
            return null;
        }

        List<JSONObject> originalTerminalNum = clientMapper.getTerminalNumByUserName(usernameList);

        if (CollectionUtils.isEmpty(originalTerminalNum)) {
            log.error("????????????????????????????????????????????????");
            clientUserList.stream().forEach(f -> {
                ClientUserDto dto = doBackward(f);
                dto.setTerminalCnt(0);
                dtoList.add(dto);
            });
            return dtoList;
        }

        Map<String, Integer> map = new HashMap<>();

        originalTerminalNum.stream().forEach(f -> map.put(f.getString("username"), f.getInteger("terminalCnt")));

        if (CollectionUtils.isEmpty(map)) {
            log.error("?????????????????????????????????,map??????");
            return null;
        }


        clientUserList.stream().forEach(f -> {
            ClientUserDto dto = doBackward(f);
            Integer cnt = map.get(f.getUsername());
            dto.setTerminalCnt(cnt == null ? 0 : cnt);
            dtoList.add(dto);
        });
        return dtoList;
    }

    @Override
    public String readExcelFile(MultipartFile file) {
        String result = "";
        //????????????EXCLE??????
        ReadUserExcel readExcel = new ReadUserExcel();
        int count = 0;
        List<Map<String, Object>> clientUserList = readExcel.getExcelInfo(file);
        //????????????excle?????????????????????list??????????????????????????????list??????????????????????????????????????????

        for (Map<String, Object> clientUser : clientUserList) {
            ClientUser insertClientUser = new ClientUser();
            try {
                insertClientUser.setUsername(clientUser.get("username").toString());
            } catch (NullPointerException e) {
                insertClientUser.setUsername("");
            }
            try {
                String status = String.valueOf(clientUser.get("status").toString());
                if ("?????????".equals(status) || "0".equals(status)) {
                    insertClientUser.setStatus(Integer.valueOf(DeviceStatus.CONFIRM.getValue()));
                } else if ("?????????".equals(status) || "1".equals(status)) {
                    insertClientUser.setStatus(Integer.valueOf(DeviceStatus.NEW.getValue()));
                } else if ("??????".equals(status) || "2".equals(status)) {
                    insertClientUser.setStatus(Integer.valueOf(DeviceStatus.CHANGED.getValue()));
                } else if ("?????????".equals(status) || "3".equals(status)) {
                    insertClientUser.setStatus(Integer.valueOf(DeviceStatus.UNAUTHORIZED.getValue()));
                } else {
                    insertClientUser.setStatus(Integer.valueOf(DeviceStatus.CONFIRM.getValue()));
                }
            } catch (NullPointerException e) {
                insertClientUser.setStatus(Integer.valueOf(DeviceStatus.NEW.getValue()));
            }
            boolean flag = this.findClientUser(clientUser.get("username").toString());
            if (flag) {
                try {
                    Long id = this.insert(insertClientUser);
                    if (id > 0) {
                        count++;
                        result = "????????????";
                    } else {
                        result = "????????????";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result = "????????????";
                }
            } else {
                result = "??????????????????";
            }
        }

        if (count > 0) {
            result = "????????????";
        } else {
            result = "????????????";
        }
        return result;
    }

    /**
     * ??????????????????????????????????????????false????????????????????????????????????true?????????????????????
     * @param username
     * @return
     */
    @Override
    public boolean findClientUser(String username) {
        ClientUser condClientUser = new ClientUser();
        condClientUser.setUsername(username);
        List<ClientUser> clientUserList = clientUserMapper.getList(condClientUser);
        if(CollectionUtils.isEmpty(clientUserList)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Long insert(ClientUser clientUser) {
        if(clientUser.getId() == null) {
            clientUser.setId(clientUserIdGenerator.nextId());
        }
        return clientUserMapper.insert(clientUser);
    }

    @Override
    public List<ClientUser> getActiveClientUser(ClientUser clientUser) {
        return clientUserMapper.getActiveClientUser(clientUser);
    }

    @Override
    public List<ClientUser> getClientUserByUsername(List<Client> clientList, String orderStr) {
        List<ClientUser> clientUserList = null;
        if (!CollectionUtils.isEmpty(clientList)) {
            clientUserList = clientUserMapper.getClientUserByUsername(clientList, orderStr);
        }
        return clientUserList;
    }

    @Override
    public Long save(@Valid @NotNull(message = "????????????????????????????????????") ClientUser clientUser, SaveType type) {
        if (type.getCode() == 0) {
            return clientUserMapper.update(clientUser);
        } else if (type.getCode() == 1) {
            return clientUserMapper.insert(clientUser);
        }
        return clientUser.getId();
    }

    @Override
    public Long save(ClientUser clientUser) {
        throw new CustomException("??????????????????????????????,????????????SaveType");
    }

    @Override
    public void deleteById(@Valid @NotNull(message = "??????ID?????????ID????????????") Long id) {
        clientUserMapper.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, String username) {
        ClientUser clientUser = findById(id);
        if (clientUser == null) {
            throw new CustomException("??????????????????");
        }
        deleteById(id);
        clientMapper.deleteByUsername(username);
    }

    @Override
    public ClientUser findById(@Valid @NotNull(message = "??????ID?????????ID????????????") Long id) {
        return clientUserMapper.findById(id);
    }

    @Override
    public List<ClientUser> getList(ClientUser clientUser) {
        return clientUserMapper.getList(clientUser);
    }

    @Override
    public List<ClientUser> getList(ClientUser clientUser, String orderStr) {
        return clientUserMapper.getListWithOrder(clientUser, orderStr);
    }


    protected ClientUserDto doBackward(ClientUser clientUser) {
        ClientUserDto dto = new ClientUserDto();
        BeanUtils.copyProperties(clientUser, dto);
        return dto;
    }
}
