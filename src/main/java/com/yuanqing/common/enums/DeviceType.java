package com.yuanqing.common.enums;

import com.yuanqing.framework.web.enums.BaseEnum;

/**
 * 设备类型
 *»
 * @author xucan
 **/
public enum DeviceType implements BaseEnum {

    OTHER("其他", 0),
    CAMERA("摄像头", 1),
    CENTER_CONTROL_SERVER("中心信令控制服务器", 2),
    PROXY_SERVER("代理服务器", 3),
    MEDIA_SERVER("媒体服务器", 4),
    USER("用户类型", 5);


    String label;

    Integer value;

    DeviceType(String label, Integer value) {
        this.label = label;
        this.value = value;
    }

    public static String getLabelByValue(String value){
        int v = Integer.parseInt(value);
        for (DeviceType deviceType : DeviceType.values()) {
            if(deviceType.value.equals(v)){
                return deviceType.label;
            }
        }
        return "未知类型";
    }

    public static String getLabel(String value){
        String valueStr = value + "";
        for (DeviceType v : DeviceType.values()) {
            if(v.getValue().equals(valueStr)){
                return v.getLabel();
            }
        }
        return null;
    }

}
