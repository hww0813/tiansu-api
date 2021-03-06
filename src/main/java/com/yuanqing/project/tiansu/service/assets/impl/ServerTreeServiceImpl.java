package com.yuanqing.project.tiansu.service.assets.impl;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.constant.Constants;
import com.yuanqing.common.enums.DeviceType;
import com.yuanqing.common.enums.SaveType;
import com.yuanqing.common.exception.CustomException;
import com.yuanqing.common.queue.ServerTreeMap;
import com.yuanqing.common.utils.SequenceIdGenerator;
import com.yuanqing.common.utils.StringUtils;
import com.yuanqing.common.utils.ip.IpUtils;
import com.yuanqing.project.tiansu.domain.macs.MacsProbe;
import com.yuanqing.project.tiansu.domain.report.ReadServerExcel;
import com.yuanqing.project.tiansu.domain.assets.Client;
import com.yuanqing.project.tiansu.domain.assets.ClientTerminal;
import com.yuanqing.project.tiansu.domain.assets.ServerTree;
import com.yuanqing.project.tiansu.mapper.assets.ClientMapper;
import com.yuanqing.project.tiansu.mapper.assets.ClientTerminalMapper;
import com.yuanqing.project.tiansu.mapper.macs.MacsProbeMapper;
import com.yuanqing.project.tiansu.mapper.operation.OperationBehaviorMapper;
import com.yuanqing.project.tiansu.mapper.assets.ServerTreeMapper;
import com.yuanqing.project.tiansu.service.assets.ICameraService;
import com.yuanqing.project.tiansu.service.assets.IServerTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by xucan on 2021-01-19 10:29
 *
 * @author xucan
 */

@Service
@Transactional(readOnly = false)
public class ServerTreeServiceImpl implements IServerTreeService {

    private static final Logger log = LoggerFactory.getLogger(ServerTreeServiceImpl.class);

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private ClientTerminalMapper clientTerminalMapper;

    @Autowired
    private ServerTreeMapper serverTreeMapper;

    @Autowired
    private ICameraService cameraService;

    @Autowired
    private MacsProbeMapper macsProbeMapper;


    private static SequenceIdGenerator serverIdGenerator = new SequenceIdGenerator(1, 1);

    @Override
    public ServerTree findOne(Long serverIp) {
        List<ServerTree> list = serverTreeMapper.findOne(serverIp);
        return (list != null && list.size() > 0) ? list.get(0) : null;
    }

    @Override
    public List<JSONObject> getAllToReport(JSONObject filters) {
        ServerTree condServerTree = new ServerTree();
        condServerTree.setServerCode(filters.getString("serverCode"));
        condServerTree.setServerDomain(filters.getString("serverDomain"));
        condServerTree.setServerIp(IpUtils.ipToLong(filters.getString("serverIp")));
        condServerTree.setServerType(filters.getString("deviceType"));
        condServerTree.setIsDelete(filters.getShort("isDelete"));
        List<ServerTree> serverTreeList = serverTreeMapper.getList(condServerTree);

        List<JSONObject> reportList = new ArrayList<JSONObject>();
        if (!CollectionUtils.isEmpty(serverTreeList)) {
            for (ServerTree serverTree : serverTreeList) {
                JSONObject jsonObject = new JSONObject();
                if (!StringUtils.isEmpty(serverTree.getServerCode())) {
                    jsonObject.put("serverCode", serverTree.getServerCode());
                }else{
                    jsonObject.put("serverCode", "");
                }
                if (!StringUtils.isEmpty(serverTree.getServerName())) {
                    jsonObject.put("serverName", serverTree.getServerName());
                } else {
                    jsonObject.put("serverName", "");
                }
                if (serverTree.getServerIp() != null) {
                    jsonObject.put("serverIp", IpUtils.longToIPv4(serverTree.getServerIp()));
                }else{
                    jsonObject.put("serverIp", "");
                }
                if (!StringUtils.isEmpty(serverTree.getServerDomain())) {
                    jsonObject.put("serverDomain", serverTree.getServerDomain());
                }else{
                    jsonObject.put("serverDomain", "");
                }
                if (!StringUtils.isEmpty(serverTree.getServerType())) {
                    jsonObject.put("serverType", DeviceType.getLabel(serverTree.getServerType()));
                }else{
                    jsonObject.put("serverType", "");
                }
                reportList.add(jsonObject);
            }
        }
        return reportList;
    }


    @Override
    public List<ServerTree> getSessionServerList(ServerTree serverTree, List<Long> serverIpList) {

        if (CollectionUtils.isEmpty(serverIpList)) {
            log.error("?????????IP????????????");
            return null;
        }
        List<ServerTree> sessionServerList = serverTreeMapper.getServerByServerIpList(serverIpList, serverTree);

        return sessionServerList;
    }


    @Override
    public String readExcelFile(MultipartFile file) {
        String result = "";
        //????????????EXCEL??????
        ReadServerExcel readExcel = new ReadServerExcel();
        int count = 0;
        //??????excel???????????????????????????
        List<Map<String, Object>> serverList = readExcel.getExcelInfo(file);
        //???????????????excel?????????????????????list?????????,????????????????????????list,??????????????????????????????,??????????????????,

        for (Map<String, Object> server : serverList) {
            ServerTree serverTree = new ServerTree();
            try {
                serverTree.setServerCode(server.get("servercode").toString());
            } catch (NullPointerException e) {
            }
            try {
                serverTree.setServerName(server.get("serverName").toString());
            } catch (NullPointerException e) {
            }
            try {
                serverTree.setServerIp(IpUtils.ipToLong(server.get("serverIp").toString()));
            } catch (NullPointerException e) {
            }
            try {
                serverTree.setServerDomain(server.get("serverDomain").toString());
            } catch (NullPointerException e) {
                serverTree.setServerDomain("");
            }
            try {
                if (server.get("serverType") != null && server.get("serverType").equals("???????????????")) {
                    serverTree.setServerType(DeviceType.CENTER_CONTROL_SERVER.getValue());
                } else {
                    serverTree.setServerType(DeviceType.OTHER.getValue());
                }
            } catch (NullPointerException e) {
                serverTree.setServerType(DeviceType.OTHER.getValue());
            }

            try {

                Long id = this.save(serverTree);

                if (id > 0) {
                    count++;
                    result = "????????????";
                } else {
                    result = "????????????";
                }
            } catch (Exception e) {
                result = "????????????";
                log.error(e.getMessage());
            }

             /*int ret=cameraMapper.insertCamera(camera1);
             if(ret == 0){
                 result = "?????????????????????";
             }*/
        }
//        if (cameraList != null && !cameraList.isEmpty()) {
        if (count > 0) {
            result = "????????????";
        } else {
            result = "????????????";
        }
        return result;
    }

    @Override
    public void batchInsert(List<ServerTree> list) {
        serverTreeMapper.batchInsert(list);
        //????????????????????????
        list.stream().forEach(f -> putServerTreeMap(f));
    }

    @Override
    public Long findId() {
        return serverTreeMapper.findId();
    }

    @Override
    public void insert(ServerTree serverTree) {
        throw new CustomException("??????????????????????????????");
    }


//    @Override
//    public void insert(ServerTree serverTree) {
//        Long IP = serverTree.getServerIp();
//        //??????ip???????????????
//        List<Client> clientList = clientMapper.findByIP(IP);
//
//        //??????ip????????????,??????????????????
//        String clientTerminalKey = String.format(Constants.TWO_FORMAT, Constants.CLIENT_TERMINAL, IP);
//        ClientTerminal clientTerminal = ClientTerminalMap.get(clientTerminalKey);
//
//        //??????????????????????????????????????????????????????
//        if (clientTerminal == null) {
//            clientTerminal = clientTerminalMapper.findByIpAddress(IP);
//            //????????????????????????
//            if (clientTerminal != null) {
//                ClientTerminalMap.put(clientTerminalKey, clientTerminal);
//            }
//        }
//        //?????????????????????/??????????????????????????????/????????????????????????
//        handleClient(clientList, clientTerminal);
//
//        //????????????
//        serverTreeMapper.insertServerTree(serverTree);
//        //?????????
//        putServerTreeMap(serverTree, IP);
//    }

    @Override
    public Long save(@Valid @NotNull(message = "????????????????????????????????????") ServerTree entity, SaveType type) {
        throw new CustomException("??????????????????????????????,??????SaveType");
    }

    @Override
    public Long save(ServerTree serverTree) {
        Long IP = serverTree.getServerIp();

        //??????ip???????????????
        List<Client> clientList = clientMapper.findByIP(IP);
        //??????ip????????????
        ClientTerminal clientTerminal = clientTerminalMapper.findByIpAddress(IP);

        //?????????????????????/??????????????????????????????/????????????????????????
        handleClient(clientList, clientTerminal);

        ServerTree server = findOne(IP);

        //??????
        if (serverTree.getId() == null && server == null) {
            serverTree.setId(serverIdGenerator.nextId());
            serverTree.setIsDelete(Short.valueOf("1"));
            serverTreeMapper.insert(serverTree);
        } else {
            serverTreeMapper.update(serverTree);
        }
        //?????????
        putServerTreeMap(serverTree);
        return serverTree.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        ServerTree server = findById(id);
        if (server == null) {
            throw new RuntimeException("entity not existed");
        }
        serverTreeMapper.delete(id);

        //????????????????????????
        String serverKey = String.format(Constants.TWO_FORMAT, Constants.SERVER_TREE, server.getServerIp());
        ServerTreeMap.remove(serverKey);

        //???????????????????????????????????????????????????????????????????????????
        Long serverIp = server.getServerIp();
        //????????????
        clientTerminalMapper.updateMark(serverIp);
        //???????????????
        clientMapper.updateMark(serverIp);
        //??????ip????????????????????????????????????????????????
        cameraService.updateIsNotServer(serverIp);
    }

    @Override
    public ServerTree findById(Long id) {
        return serverTreeMapper.findById(id);
    }

    @Override
    public List<ServerTree> getList(ServerTree serverTree) {
        return serverTreeMapper.getList(serverTree);
    }

    /**
     * ??????clientTerminal ??????clientTerminalMap??????
     *
     * @param serverTree
     */
    private void putServerTreeMap(ServerTree serverTree) {
        String serverKey = String.format(Constants.TWO_FORMAT, Constants.SERVER_TREE, serverTree.getServerIp());
        ServerTreeMap.put(serverKey, serverTree);
    }

    private void putServerTreeMap(ServerTree serverTree, Long ip) {
        String serverKey = String.format(Constants.TWO_FORMAT, Constants.SERVER_TREE, ip);
        ServerTreeMap.put(serverKey, serverTree);
    }


    /**
     * ??????????????????????????????????????????????????????????????????
     * ????????????????????????????????????????????????????????????
     *
     * @param clientList     ?????????
     * @param clientTerminal ??????
     */
    private void handleClient(List<Client> clientList, ClientTerminal clientTerminal) {
        if (clientList.size() > 0) {
            for (Client client : clientList) {
                client.setDeviceType(DeviceType.PROXY_SERVER.getValue());
                clientMapper.update(client);
            }
        }
        if (clientTerminal != null) {
            clientTerminal.setDeviceType(DeviceType.PROXY_SERVER.getValue());
            clientTerminalMapper.updateClientTerminal(clientTerminal);
        }
    }

    @Override
    public List<JSONObject> getProbeName() {

        List<MacsProbe> probeList = macsProbeMapper.getProbeList();
        ServerTree serverTree = new ServerTree();
        serverTree.setIsDelete((short) 1);
        List<ServerTree> serverTreeList = getList(serverTree);

        List<JSONObject> list = new ArrayList<>();

        serverTreeList.stream().forEach(f ->{
            probeList.stream().forEach(h ->{
                if(h.getIpAddress().equals(f.getServerIp())){
                    JSONObject probeName = new JSONObject();
                    String e = f.getServerName()==null ? IpUtils.longToIp(f.getServerIp()):f.getServerName();
                    probeName.put("hd_info",h.getHdInfo());
                    probeName.put("probe_name",e);
                    list.add(probeName);
                }
            });
        });
        if(CollectionUtils.isEmpty(list)){
            JSONObject o = new JSONObject();
            if(probeList.size()>0) {
                o.put("hd_info", probeList.get(0).getHdInfo());
                o.put("probe_name", "??????");

            }else{
                o.put("hd_info", "null");
                o.put("probe_name", "??????????????????");
            }
            list.add(o);
        }

        return list;
    }

}
