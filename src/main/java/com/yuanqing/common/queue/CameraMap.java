package com.yuanqing.common.queue;

import com.yuanqing.project.tiansu.domain.Camera;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xucan
 */
public class CameraMap {

    private static final Map<String, Camera> map = new HashMap<>();

    /**
     * 获取当前队列大小
     *
     * @return 当前map大小
     */
    public static int size() {
        return map.size();
    }

    /**
     * put到map中
     *
     * @param id
     * @param data
     */
    public static void put(String id, Camera data) {
        synchronized (map) {
            map.put(id, data);
        }
    }

    /**
     * get
     */
    public static Camera get(String key) {
        synchronized (map) {
            return map.get(key);
        }
    }

    /**
     * 删除数据
     *
     * @param id
     */
    public static void remove(String id) {
        synchronized (map) {
            map.remove(id);
        }
    }

    /**
     *
     * 查看是否包含
     *
     * @param id
     */

    public static boolean contain(String id){
        synchronized (map){
            return map.containsKey(id);
        }
    }

    public static Collection<Camera> valueList(){
        synchronized (map){
            return new ArrayList<>(map.values());
        }
    }


}
