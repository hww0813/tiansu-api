package com.yuanqing.project.tiansu.service.macs.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yuanqing.common.constant.Constants;
import com.yuanqing.common.enums.SaveType;
import com.yuanqing.common.exception.config.ConfigFileException;
import com.yuanqing.common.exception.config.ConfigNullException;
import com.yuanqing.common.exception.config.ConfigParseException;
import com.yuanqing.common.queue.MacsMap;
import com.yuanqing.common.utils.StringUtils;
import com.yuanqing.common.utils.file.FileUtils;
import com.yuanqing.framework.redis.RedisCache;
import com.yuanqing.framework.web.domain.AjaxResult;
import com.yuanqing.project.tiansu.domain.assets.Camera;
import com.yuanqing.project.tiansu.domain.macs.MacsConfig;
import com.yuanqing.project.tiansu.domain.macs.MacsRegion;
import com.yuanqing.project.tiansu.service.feign.MacsFeignClient;
import com.yuanqing.project.tiansu.service.macs.IMacsConfigService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 获取配置中心配置
 * @author xucan
 */
@Service
public class MacsConfigServiceImpl implements IMacsConfigService {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MacsConfigServiceImpl.class);

    public static final String MACS_TOKEN = "BearereyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6ImUzNjQ4YmI0LWQyMWEtNDRmZi05Mj" +
            "c0LWJjMDcxMDBjMzgzOSJ9.Ix7KKtW5Q4UZCbKgK5roz0y7xv7z5a_tb37k8alIwuAG9uiga6R6dBuCDEsx8HWqlUnXTVqNxHRaeo_6RY_e-w";


    @Value("${tiansu.macshost}")
    private String prefix;

    @Value("${tiansu.apiConfig}")
    private String apiConfig;

    @Autowired
    private RedisCache redisCache;

    @Resource
    private MacsFeignClient macsFeignClient;


    @Override
    public List<MacsConfig> selectMacsConfigByTypeAndName(MacsConfig macsConfig) throws ConfigFileException{

        if(macsConfig == null) {
            return null;
        }

        String macsKey = macsConfig.getType() + "-" + macsConfig.getName();

        String rspStr;

        try{

            rspStr =  macsFeignClient.getConfigById(macsConfig.getType(),macsConfig.getName());

        }catch (Exception e){

            LOGGER.error("请求Macs接口异常,读取本地配置信息:"+macsKey);
            e.printStackTrace();

            return getLocationConfig(macsKey);
        }

        if (StringUtils.isEmpty(rspStr)) {

            LOGGER.error("请求Macs接口返回结果为空,读取本地配置信息:"+macsKey);

            return getLocationConfig(macsKey);
        }

        JSONObject jsonObject = (JSONObject) JSONObject.parse(rspStr);

        if(jsonObject == null || !jsonObject.containsKey("data")) {

            LOGGER.error("请求Macs接口配置为空,读取本地配置信息:" + macsKey);

            return getLocationConfig(macsKey);

        }else{

            LOGGER.info("请求Macs接口成功"+macsKey);

            JSONArray data = jsonObject.getJSONArray("data");

            List<MacsConfig> Config = data.toJavaList(MacsConfig.class);

            LOGGER.info("将配置信息写入缓存...");
            MacsMap.put(macsKey,Config);

            JSONObject MacsMapList = MacsMap.valueJson();

            LOGGER.info("将配置信息更新到本地配置文件...");
            FileUtils.writeFile(Paths.get(apiConfig),MacsMapList.toJSONString());

            return Config;
        }

    }


    /**
     * 获取下级地区
     * @param id
     * @return
     */
    @Override
    public List<MacsRegion> getLowerRegion(String id) {
        if(StringUtils.isEmpty(id)) {
            return null;
        }

        String key = Constants.CONFIG_LOWER_REGION + id;

        List<Object> redisConfig = redisCache.getCacheList(key);

        if(CollectionUtils.isEmpty(redisConfig)){
            String rspStr = macsFeignClient.getConfigById(id);

            if (StringUtils.isEmpty(rspStr)){
                LOGGER.error("获取区域异常 id={}", id);
                return null;
            }
            JSONObject jsonObject = (JSONObject) JSONObject.parse(rspStr);

            // || 200 != jsonObject.getIntValue("code")
            if(jsonObject == null || !jsonObject.containsKey("data")) {
                LOGGER.error("获取区域为空 id={}", id);
                return null;
            }
            JSONArray data = jsonObject.getJSONArray("data");
            List<MacsRegion> macsRegion = data.toJavaList(MacsRegion.class);

            redisCache.setCacheList(key,macsRegion,1);
            LOGGER.info("下级地区缓存更新成功");
            return macsRegion;
        }else{
            List<MacsRegion> lowerRegion =(List<MacsRegion>)(Object)redisConfig;
            return lowerRegion;
        }
    }

    /**
     * 根据regionId 获取区域详情
     * @param id
     * @return
     */
    @Override
    public MacsRegion selectMacsRegionInfo(String id) {
        if(StringUtils.isEmpty(id)) {
            return null;
        }
        String rspStr = macsFeignClient.getRegionInfo(id);
        if (StringUtils.isEmpty(rspStr))
        {
            LOGGER.error("获取区域异常 id={}", id);
            return null;
        }

        JSONObject jsonObject = (JSONObject) JSONObject.parse(rspStr);

        // || 200 != jsonObject.getIntValue("code")
        if(jsonObject == null || !jsonObject.containsKey("data")) {
            LOGGER.error("获取区域为空 id={}", id);
            return null;
        }

        JSONObject data = (JSONObject) jsonObject.get("data");
        MacsRegion macsRegion = data.toJavaObject(MacsRegion.class);

        return macsRegion;
    }

    @Override
    public MacsRegion getRegion(String cityCode) {
        MacsRegion region;
        if (cityCode == null || "".equals(cityCode)) {

            List<MacsConfig> macsConfig = selectMacsConfigByTypeAndName(new MacsConfig("system","region"));

            if(CollectionUtils.isEmpty(macsConfig)){
                LOGGER.error("获取区域异常");
                return null;
            }

            String[] regionCodeArr = macsConfig.get(0).getValue().split(",");
            String regionCode = regionCodeArr[regionCodeArr.length - 1];
            region = selectMacsRegionInfo(regionCode);
        } else {
            region = selectMacsRegionInfo(cityCode);
        }
        return region;

    }

    @Override
    public Long save(@Valid @NotNull(message = "保存或更新的实体不能为空") MacsConfig entity, SaveType type) {
        return null;
    }

    @Override
    public Long save(@Valid @NotNull(message = "保存或更新的实体不能为空") MacsConfig entity) {
        return null;
    }

    @Override
    public void deleteById(@Valid @NotNull(message = "根据ID删除的ID不能为空") Long id) {

    }

    @Override
    public MacsConfig findById(@Valid @NotNull(message = "根据ID查询的ID不能为空") Long id) {
        return null;
    }

    @Override
    public List<MacsConfig> getList(MacsConfig macsConfig) {
        return null;
    }

    @Override
    public List<MacsRegion> getRegionList() {

        String rspStr = macsFeignClient.getRegionList();

        if (StringUtils.isEmpty(rspStr))
        {
            LOGGER.error("获取区域异常");
            return null;
        }

        JSONObject jsonObject = (JSONObject) JSONObject.parse(rspStr);

        if(jsonObject == null || !jsonObject.containsKey("data")) {
            LOGGER.error("获取区域为空");
            return null;
        }

        JSONArray data = jsonObject.getJSONArray("data");
        List<MacsRegion> macsRegion = data.toJavaList(MacsRegion.class);
        return macsRegion;
    }


    @Override
    public List<String> getAllLowerRegion(String regionId) {
        //下级区也需要查询,将所有本级及下级平台的id存入string
        List<MacsRegion> areaRegion = getLowerRegion(regionId);
        List<String> region = new ArrayList<>();
        region.add(regionId);
        areaRegion.stream().forEach(f ->{
            region.add(f.getId());
        });
        return  region;
    }

    @Override
    public void setLowerRegionByCamera(List<Camera> list) {
        MacsRegion region = getRegion(null);

        //匹配所在区域
        List<MacsRegion> lowerRegion = getLowerRegion(region.getId());

        if(!CollectionUtils.isEmpty(lowerRegion)){
            lowerRegion.stream().forEach(f -> {
                list.stream().forEach(h -> {
                    if (h.getRegion() != null && h.getRegion().toString().equals(f.getId())) {
                        h.setRegionName(f.getName());
                    }
                });
            });
        }

    }

    @Override
    public AjaxResult selectMacsConfigById(Long id) {
        return macsFeignClient.selectMacsConfigById(id, MACS_TOKEN);
    }

    @Override
    public JSONObject selectMacsConfigList(MacsConfig macsConfig) {
        return macsFeignClient.getMacsConfigList(macsConfig, MACS_TOKEN);
    }

    @Override
    public AjaxResult insertMacsConfig(MacsConfig macsConfig) {
        return macsFeignClient.addMacsConfig(macsConfig, MACS_TOKEN);
    }

    @Override
    public AjaxResult updateMacsConfig(MacsConfig macsConfig) {
        return macsFeignClient.editMacsConfig(macsConfig, MACS_TOKEN);
    }

    @Override
    public AjaxResult deleteMacsConfigByIds(Long[] ids) {
        return macsFeignClient.removeMacsConfig(ids, MACS_TOKEN);
    }



    /**
     * 读取本地配置文件配置
     * @param macsKey 配置项
     * @return
     * @throws ConfigFileException 配置文件异常
     */
    private List<MacsConfig> getLocationConfig(String macsKey) throws ConfigFileException {

        JSONObject fileContent = FileUtils.readFile(Paths.get(apiConfig));

        if(CollectionUtils.isEmpty(fileContent)){
            throw new ConfigNullException();
        }

        JSONArray jsonArray = fileContent.getJSONArray(macsKey);

        if(CollectionUtils.isEmpty(jsonArray)){
            throw new ConfigParseException(macsKey);
        }

        List<MacsConfig> config = jsonArray.toJavaList(MacsConfig.class);

        //读取成功后将配置放入缓存
        MacsMap.put(macsKey,config);

        return config;
    }

    @Override
    public AjaxResult operate(String instance, String operation, String serverName) {
        return macsFeignClient.operate(instance, operation, serverName, MACS_TOKEN);
    }

}
