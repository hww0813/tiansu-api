package com.yuanqing.project.tiansu.controller.assets;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.enums.SaveType;
import com.yuanqing.common.utils.StringUtils;
import com.yuanqing.framework.redis.RedisCache;
import com.yuanqing.framework.web.controller.BaseController;
import com.yuanqing.framework.web.domain.AjaxResult;
import com.yuanqing.framework.web.page.TableDataInfo;
import com.yuanqing.project.tiansu.domain.assets.Client;
import com.yuanqing.project.tiansu.domain.assets.ClientUser;
import com.yuanqing.project.tiansu.domain.assets.dto.ClientUserDto;
import com.yuanqing.project.tiansu.service.assets.IClientService;
import com.yuanqing.project.tiansu.service.assets.IClientUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yuanqing.common.constant.Constants.INDEX_USER_COUNTS_CACHE;

/**
 * @author xucan
 */
@RestController
@RequestMapping(value = "/api/device/sip-user")
@CrossOrigin
@Api(value = "终端用户", description = "终端用户相关Api")
public class ClientUserController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientUserController.class);

    @Autowired
    private IClientUserService clientUserService;

    @Autowired
    private IClientService clientService;

    @Autowired
    private RedisCache redisCache;


    @GetMapping("/list")
    @ApiOperation(value = "获取终端用户列表", httpMethod = "GET")
    public AjaxResult getAll(@ApiParam("状态")@RequestParam(value = "status", required = false) Integer status,
                             @ApiParam("ID")@RequestParam(value = "id", required = false) Long id,
                             @ApiParam("用户名")@RequestParam(value = "username", required = false) String username,
                             @ApiParam("IP地址")@RequestParam(value = "ipAddress", required = false) Long ipAddress,
                             @ApiParam("排序")@RequestParam(required = false) String orderType,
                             @ApiParam("排序对象")@RequestParam(required = false) String orderValue) {
        ClientUser clientUser = new ClientUser();

        clientUser.setStatus(status);
        clientUser.setUsername(username);
        clientUser.setId(id);

        String orderStr = null;
        if (!StringUtils.isEmpty(orderType) && !StringUtils.isEmpty(orderValue)) {
            orderStr = StringUtils.humpToUnderline(orderValue) + " " + orderType;
        }

        List<ClientUser> list = null;

        //判断 ipAddress 是否为空
        if (ipAddress != null) {
            Client client = new Client();
            client.setIpAddress(ipAddress);

            // 根据用户名查询client列表  需要用IP
            List<Client> clientList = clientService.getList(client);

            startPage();
            list = clientUserService.getClientUserByUsername(clientList, orderStr);

        } else {
            startPage();
            list = clientUserService.getList(clientUser, orderStr);
        }

        //查询终端数量
        List<ClientUserDto> clientUserDtoList = clientUserService.handleClientUserTerminalNum(list);
        if (clientUserDtoList == null) {
            clientUserDtoList = new ArrayList<>();
        }

        return AjaxResult.success(getDataTable(clientUserDtoList, list));
    }

    @DeleteMapping
    @ApiOperation(value = "删除终端用户", httpMethod = "PUT")
    public AjaxResult deleteSipClient(@Valid @ApiParam("ID")@RequestParam(value = "id") Long id,
                                      @Valid @ApiParam("用户名")@RequestParam(value = "username") String username) {

        clientUserService.delete(id, username);
        return AjaxResult.success();
    }

    @PutMapping
    @ApiOperation(value = "更新终端用户", httpMethod = "PUT")
    public AjaxResult putSipClient(@Valid @ApiParam("终端用户")@RequestBody ClientUser dto) {

        clientUserService.save(dto, SaveType.UPDATE);
        return AjaxResult.success();
    }

    @PutMapping("/updateStatus")
    @ApiOperation(value = "更新终端用户状态为已确认", httpMethod = "PUT")
    public AjaxResult updateStatus(@Valid @ApiParam("勾选的终端用户ID")@RequestBody JSONObject jsonObject) {
        String str1 = String.valueOf(jsonObject.get("id"));
        String str = str1.substring(1, str1.length() - 1);
        String[] array = str.split(",");

        clientUserService.changStatus(array);
        return AjaxResult.success();
    }

    // 导入excel
    @PostMapping(value = "/import")
    @ApiOperation(value = "导入用户列表")
    public Map<String, Object> importExcel(@ApiParam("用户列表EXCEL")@RequestParam(value = "file", required = false) MultipartFile file) {

        Map<String, Object> map = new HashMap<String, Object>();
        String AjaxResult = clientUserService.readExcelFile(file);
        map.put("message", AjaxResult);
        return map;
    }

//    @GetMapping("/getUserDatas")
//    @ApiOperation(value = "获取首页用户数据", httpMethod = "GET")
//    public AjaxResult getClientDatas() {
//        return AjaxResult.success(homePageManager.findObject("user","user_num"));
//    }

    @GetMapping("/active")
    @ApiOperation(value = "获取活跃用户列表")
    public TableDataInfo getActiveClient() {

        startPage();
        List<ClientUser> activeClientUser = clientUserService.getActiveClientUser(new ClientUser());

        List<ClientUserDto> dtoList = clientUserService.handleClientUserTerminalNum(activeClientUser);

        return getDataTable(dtoList,activeClientUser);
    }


    @GetMapping("/getUserDatas")
    @ApiOperation(value = "获取用户数据", httpMethod = "GET")
    public AjaxResult getUserDatas() {
        return AjaxResult.success("success", redisCache.getCacheObject(INDEX_USER_COUNTS_CACHE));
    }

}
