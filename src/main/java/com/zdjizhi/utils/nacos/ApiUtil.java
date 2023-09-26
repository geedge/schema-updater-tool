package com.zdjizhi.utils.nacos;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.zdjizhi.common.SchemaConfig;

import java.util.Properties;


public class ApiUtil {
    private static final Log logger = LogFactory.get();


    /**
     * 获取nacos上的配置文件
     *
     * @param dataId 配置文件名称
     * @return 配置内容
     */
    public static String getConfiguration(String nacos_server, String dataId, String namespace) {
        String content = null;
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacos_server);
        properties.setProperty(PropertyKeyConst.NAMESPACE, namespace);
        properties.setProperty(PropertyKeyConst.USERNAME, SchemaConfig.NACOS_USERNAME);
        properties.setProperty(PropertyKeyConst.PASSWORD, SchemaConfig.NACOS_PIN);
        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
            content = configService.getConfig(dataId, SchemaConfig.NACOS_GROUP, 5000);
        } catch (NacosException e) {
            logger.error("Failed to get configuration content from NACOS! The exception message is:" + e.getMessage());
            e.printStackTrace();
        }
        return content;
    }

    /**
     * 推送配置文件到nacos
     *
     * @param dataId 配置文件名称
     * @param schema 配置内容
     * @return 推送状态
     */
    public static boolean pushConfiguration(String nacos_server, String dataId, String schema, String namespace) {
        boolean pushStatus = false;
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacos_server);
        properties.setProperty(PropertyKeyConst.NAMESPACE, namespace);
        properties.setProperty(PropertyKeyConst.USERNAME, SchemaConfig.NACOS_USERNAME);
        properties.setProperty(PropertyKeyConst.PASSWORD, SchemaConfig.NACOS_PIN);
        try {
            String[] split = dataId.split("\\.");
            String type = split[split.length - 1];
            ConfigService configService = NacosFactory.createConfigService(properties);
            pushStatus = configService.publishConfig(dataId, SchemaConfig.NACOS_GROUP, schema, type);

        } catch (NacosException e) {
            logger.error("Failed to push configuration to NACOS! The exception message is:" + e.getMessage());
            e.printStackTrace();
        }
        return pushStatus;
    }

}
