package com.yuanqing.project.tiansu.service.assets;

import com.alibaba.fastjson.JSONObject;
import com.yuanqing.framework.web.service.BaseService;
import com.yuanqing.project.tiansu.domain.assets.ServerTree;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by xucan on 2021-01-19 10:27
 * @author xucan
 */
public interface IServerTreeService extends BaseService<ServerTree,Long> {

    /**
     * 跟据 serverIp 查询服务器
     * @param serverIp 服务器IP
     * @return 如果查出多条记录，只取第一条服务器数据
     */
    ServerTree findOne(Long serverIp);


    /**
     * 获取报表
     *
     * TODO:excel相关暂时搁置
     * @param filters 过滤
     * @return 返回报表
     */
    List<JSONObject> getAllToReport(JSONObject filters);


    /**
     * 获取服务器会话列表
     * @param filters 过滤
     * @return 返回列表集合
     */
    List<ServerTree> getSessionServerList(JSONObject filters);

    /**
     * 读取excel中的数据,生成list
     * TODO:excel相关暂时搁置
     * @param file excel文件
     * @return 文件名
     */
    String readExcelFile(MultipartFile file);

    /**
     * 批量新增
     * @param list 新增服务器集合
     */
    void batchInsert(List<ServerTree> list);

    /**
     * 获取服务器下一个自增序列的值
     * @return 自增序列下一个的值
     */
    Long findId ();

    /**
     * 新增一条服务器 会根据服务器IP查询缓存
     * @param serverTree
     */
    public void insert(ServerTree serverTree);

}