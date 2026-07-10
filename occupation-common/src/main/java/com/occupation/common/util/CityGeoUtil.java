package com.occupation.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 中国城市名称 → 经纬度（WGS84 近似，供 3D 地图热力点定位）
 */
public final class CityGeoUtil {

    private static final Map<String, double[]> CITY_COORDS;

    static {
        Map<String, double[]> map = new HashMap<>();
        put(map, "北京", 116.4074, 39.9042);
        put(map, "上海", 121.4737, 31.2304);
        put(map, "天津", 117.2008, 39.0842);
        put(map, "重庆", 106.5516, 29.5630);
        put(map, "广州", 113.2644, 23.1291);
        put(map, "深圳", 114.0579, 22.5431);
        put(map, "杭州", 120.1551, 30.2741);
        put(map, "南京", 118.7969, 32.0603);
        put(map, "苏州", 120.5853, 31.2989);
        put(map, "成都", 104.0665, 30.5723);
        put(map, "武汉", 114.3055, 30.5928);
        put(map, "西安", 108.9398, 34.3416);
        put(map, "郑州", 113.6254, 34.7466);
        put(map, "长沙", 112.9388, 28.2282);
        put(map, "青岛", 120.3826, 36.0671);
        put(map, "济南", 117.1205, 36.6519);
        put(map, "合肥", 117.2272, 31.8206);
        put(map, "福州", 119.2965, 26.0745);
        put(map, "厦门", 118.0894, 24.4798);
        put(map, "昆明", 102.8329, 24.8801);
        put(map, "贵阳", 106.6302, 26.6477);
        put(map, "南宁", 108.3661, 22.8172);
        put(map, "海口", 110.1999, 20.0444);
        put(map, "沈阳", 123.4328, 41.8045);
        put(map, "大连", 121.6147, 38.9140);
        put(map, "长春", 125.3235, 43.8171);
        put(map, "哈尔滨", 126.5349, 45.8038);
        put(map, "石家庄", 114.5149, 38.0428);
        put(map, "太原", 112.5489, 37.8706);
        put(map, "呼和浩特", 111.6708, 40.8183);
        put(map, "南昌", 115.8581, 28.6832);
        put(map, "兰州", 103.8343, 36.0611);
        put(map, "银川", 106.2309, 38.4872);
        put(map, "西宁", 101.7782, 36.6171);
        put(map, "乌鲁木齐", 87.6168, 43.8256);
        put(map, "拉萨", 91.1320, 29.6604);
        put(map, "宁波", 121.5503, 29.8746);
        put(map, "无锡", 120.3119, 31.4912);
        put(map, "佛山", 113.1214, 23.0215);
        put(map, "东莞", 113.7518, 23.0207);
        put(map, "珠海", 113.5765, 22.2707);
        put(map, "温州", 120.6994, 27.9949);
        put(map, "常州", 119.9740, 31.8107);
        put(map, "徐州", 117.2841, 34.2058);
        put(map, "烟台", 121.4479, 37.4638);
        put(map, "洛阳", 112.4540, 34.6197);
        put(map, "唐山", 118.1802, 39.6309);
        CITY_COORDS = Collections.unmodifiableMap(map);
    }

    private CityGeoUtil() {
    }

    private static void put(Map<String, double[]> map, String city, double lng, double lat) {
        map.put(city, new double[]{lng, lat});
        map.put(city + "市", new double[]{lng, lat});
    }

    /**
     * 按城市名查找坐标，支持「成都」「成都市」及去掉后缀的模糊匹配
     */
    public static Optional<double[]> resolve(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return Optional.empty();
        }
        String trimmed = cityName.trim();
        if (CITY_COORDS.containsKey(trimmed)) {
            return Optional.of(CITY_COORDS.get(trimmed));
        }
        String normalized = trimmed.replace("市", "").replace("地区", "");
        for (Map.Entry<String, double[]> e : CITY_COORDS.entrySet()) {
            String key = e.getKey().replace("市", "");
            if (key.equals(normalized) || normalized.contains(key) || key.contains(normalized)) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }
}
