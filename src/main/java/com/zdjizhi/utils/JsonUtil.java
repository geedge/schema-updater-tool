package com.zdjizhi.utils;

/**
 * @author qidaijie
 * @Package com.zdjizhi.utils
 * @Description:
 * @date 2022/7/2615:32
 */
public class JsonUtil {

    /**
     * 截取文件的后缀名，判断文件类型是否处理
     *
     * @param fileName 要截取的文件
     * @return 文件拓展名
     */
    public static String lastName(String fileName) {
        if (StringUtil.isNotBlank(fileName)) {
            String[] split = fileName.split("\\.");
            if (split.length > 1) {
                return split[split.length - 1];
            } else {
                return "";
            }
        }
        return "";
    }

}
