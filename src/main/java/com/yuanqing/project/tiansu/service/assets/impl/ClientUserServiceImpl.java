package com.yuanqing.project.tiansu.service.assets.impl;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.enums.SaveType;
import com.yuanqing.common.exception.CustomException;
import com.yuanqing.common.utils.DateUtils;
import com.yuanqing.project.tiansu.domain.assets.ClientTerminal;
import com.yuanqing.project.tiansu.domain.assets.ClientUser;
import com.yuanqing.project.tiansu.domain.assets.dto.ClientTerminalDto;
import com.yuanqing.project.tiansu.domain.assets.dto.ClientUserDto;
import com.yuanqing.project.tiansu.mapper.assets.ClientMapper;
import com.yuanqing.project.tiansu.mapper.assets.ClientUserMapper;
import com.yuanqing.project.tiansu.service.assets.IClientUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xucan on 2021-01-19 17:52
 * @author xucan
 */

@Service
public class ClientUserServiceImpl implements IClientUserService {

    @Autowired
    private ClientUserMapper clientUserMapper;

    @Autowired
    private ClientMapper clientMapper;

    @Override
    public List<JSONObject> getAllToReport(JSONObject filters) {
        return null;
    }

    @Override
    public boolean changStatus(String[] ids) {
        return clientUserMapper.changStatus(ids);
    }

    @Override
    public List<ClientUserDto> handleClientUserTerminalNum(List<ClientUser> list) {

        if(list.size()<1){
            throw new CustomException("没有查询到结果");
        }
        //提取集合用户名
        List<String> usernameList = list.stream().map(f -> f.getUsername()).collect(Collectors.toList());

        List<JSONObject> originalTerminalNum = clientMapper.getTerminalNumByUserName(usernameList);

        Map<String,Integer> map = new HashMap<>();

        originalTerminalNum.stream().forEach(f -> map.put(f.getString("username"),f.getInteger("terminalCnt")));

        List<ClientUserDto> dtoList = new ArrayList<>();

        list.stream().forEach(f -> {
            ClientUserDto dto = doBackward(f);
            Integer cnt = map.get(f.getUsername());
            dto.setTerminalCnt(cnt==null?0:cnt);
            dtoList.add(dto);
        });
        return dtoList;
    }

    @Override
    public String readExcelFile(MultipartFile file) {
        return null;
    }

    @Override
    public boolean findClientUser(String username) {
        return false;
    }

    @Override
    public Long insert(ClientUser clientUser) {
        return clientUserMapper.insert(clientUser);
    }

    @Override
    public List<ClientUser> getActiveClientUser() {
        ClientUser clientUser = (ClientUser) DateUtils.getDayTime(ClientUser.class);
        return clientUserMapper.getList(clientUser);
    }

    @Override
    public Long save(@Valid @NotNull(message = "保存或更新的实体不能为空") ClientUser clientUser, SaveType type) {
        if(type.getCode()==0){
            return clientUserMapper.update(clientUser);
        }else if(type.getCode()==1){
            return clientUserMapper.insert(clientUser);
        }
        return clientUser.getId();
    }

    @Override
    public Long save(ClientUser clientUser) {
        throw new CustomException("暂不支持这种保存方式,需要传入SaveType");
    }

    @Override
    public void deleteById(@Valid @NotNull(message = "根据ID删除的ID不能为空") Long id) {
        clientUserMapper.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id,String username){
        ClientUser clientUser = findById(id);
        if(clientUser == null){
            throw new CustomException("该用户不存在");
        }
        deleteById(id);
        clientMapper.deleteByUsername(username);
    }

    @Override
    public ClientUser findById(@Valid @NotNull(message = "根据ID查询的ID不能为空") Long id) {
        return clientUserMapper.findById(id);
    }

    @Override
    public List<ClientUser> getList(ClientUser clientUser) {
        return clientUserMapper.getList(clientUser);
    }



    protected ClientUserDto doBackward(ClientUser clientUser) {
        ClientUserDto dto = new ClientUserDto();
        BeanUtils.copyProperties(clientUser, dto);
        return dto;
    }
}