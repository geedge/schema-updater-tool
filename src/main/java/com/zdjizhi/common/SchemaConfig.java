package com.zdjizhi.common;


import com.zdjizhi.utils.system.SchemaConfigurations;

/**
 * @author Administrator
 */
public class SchemaConfig {

    /**
     * Nacos
     */
    public static final String NACOS_PIN = SchemaConfigurations.getStringProperty(0, "nacos.pin");
    public static final String NACOS_GROUP = SchemaConfigurations.getStringProperty(0, "nacos.group");
    public static final String NACOS_USERNAME = SchemaConfigurations.getStringProperty(0, "nacos.username");
    public static final String NON_SCHEMA_TABLES = SchemaConfigurations.getStringProperty(0, "non.schema.tables");

}