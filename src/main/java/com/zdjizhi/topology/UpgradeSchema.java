package com.zdjizhi.topology;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.zdjizhi.common.SchemaConfig;
import com.zdjizhi.utils.JsonUtil;
import com.zdjizhi.utils.StringUtil;
import com.zdjizhi.utils.nacos.ApiUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class UpgradeSchema {
    private static final Log logger = LogFactory.get();

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try {
            String path = args[0];
            String nacos_server = args[1];
            String namespace = args[2];
            File file = new File(path);
            File[] fileNames = file.listFiles();
            if (fileNames != null) {
                for (File fileName : fileNames) {
                    if (!fileName.isDirectory()) {
                        String dataId = fileName.getName();
                        if (!SchemaConfig.NON_SCHEMA_TABLES.contains(dataId)) {
                            String oldSchema = ApiUtil.getConfiguration(nacos_server, dataId, namespace);
                            if (StringUtil.isNotBlank(oldSchema)) {
                                String tmpSchema = FileUtils.readFileToString(fileName, "UTF-8");
                                if ("json".equals(JsonUtil.lastName(dataId)) && documentTypeJudgment(dataId, tmpSchema) && StringUtil.isNotBlank(tmpSchema)) {
                                    logger.info("================开始对{}表进行更新操作================", dataId);
                                    Map<String, Integer> ttlMap = new HashMap<>(16);
                                    ttlMap.putAll(getOldTTL(oldSchema));
                                    Map<String, String> visibilityMap = new HashMap<>(16);
                                    visibilityMap.putAll(getOldVisibility(oldSchema));

                                    JSONObject json = new JSONObject(tmpSchema, false, true);
                                    changeTableTTL(oldSchema, json);
                                    String newSchema = upgradeSchema(json, ttlMap, visibilityMap);

                                    boolean status = ApiUtil.pushConfiguration(nacos_server, dataId, newSchema, namespace);
                                    outMessage(status, dataId);

                                } else {
                                    boolean status = ApiUtil.pushConfiguration(nacos_server, dataId, tmpSchema, namespace);
                                    outMessage(status, dataId);
                                }
                            } else {
                                logger.warn("Nacos未查询到{}，判断为新文件，直接推送至Nacos", dataId);
                                boolean status = ApiUtil.pushConfiguration(nacos_server, dataId, FileUtils.readFileToString(fileName, "UTF-8"), namespace);
                                outMessage(status, dataId);
                            }
                        } else {
                            logger.warn("{}非Schema表，直接推送至Nacos", dataId);
                            boolean status = ApiUtil.pushConfiguration(nacos_server, dataId, FileUtils.readFileToString(fileName, "UTF-8"), namespace);
                            outMessage(status, dataId);
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            logger.error("Please enter parameters!");
            logger.error("example: java -jar schema-updater-tool-{version}.jar {schema folder} {nacos address} {nacos namespcae}");
            logger.error("example: java -jar schema-updater-tool-3.1.jar schema/ 192.168.44.12 test");
            ae.printStackTrace();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 获取nacos内已有schema的Visibility信息
     *
     * @param schema schema内容
     * @return Visibility合集{name:Visibility}
     */
    private static Map<String, String> getOldVisibility(String schema) {
        Map<String, String> tmpMap = new HashMap<>(16);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(schema);
        ArrayList<Object> oldFields = JsonPath.read(document, "$.fields");

        for (Object oldField : oldFields) {
            try {
                if (oldField.toString().contains("visibility")) {
                    String fieldName = JsonPath.read(oldField, "$.name");
                    String fieldVisbility = JsonPath.read(oldField, "$.doc.visibility");
                    tmpMap.put(fieldName, fieldVisbility);
                }
            } catch (Exception e) {
                logger.error("解析visibility信息异常" + e.getMessage());
            }
        }
        return tmpMap;
    }

    /**
     * 获取nacos内已有schema的TTL信息
     *
     * @param schema schema内容
     * @return TTL合集{name:ttl}
     */
    private static Map<String, Integer> getOldTTL(String schema) {
        Map<String, Integer> tmpMap = new HashMap<>(16);
        try {
            JSONObject json = new JSONObject(schema, false, true);
            if (json.containsKey("fields")) {
                JSONArray oldFields = json.getJSONArray("fields");
                for (Object oldField : oldFields) {
                    JSONObject jsonTmp = new JSONObject(oldField.toString(), false, true);
                    String fieldName = jsonTmp.getStr("name");
                    JSONObject doc = new JSONObject(jsonTmp.getStr("doc"), false, true);
                    if (doc.containsKey("ttl")) {
                        tmpMap.put(fieldName, doc.getInt("ttl"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析ttl信息异常" + e.getMessage());
        }
        return tmpMap;
    }

    /**
     * 获取nacos内已有schema表TTL，修改新Schema表TTL
     *
     * @param schema 旧schema内容
     * @param json   新schema-json
     */
    private static void changeTableTTL(String schema, JSONObject json) {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(schema);
        try {
            Integer oldTTL = JsonPath.read(document, "$.doc.ttl");
            if (oldTTL != null) {
                json.getJSONObject("doc").set("ttl", oldTTL);

            }
        } catch (RuntimeException e) {
            logger.warn("该表不包含表TTL信息!");
        }
    }

    /**
     * 更新schema
     *
     * @param json   schema
     * @param ttlMap ttl数据
     * @return 同步ttl后的schema
     */
    private static String upgradeSchema(JSONObject json, Map<String, Integer> ttlMap, Map<String, String> visibilityMap) {
        JSONArray newFields = json.getJSONArray("fields");
        JSONArray tmpFields = new JSONArray();
        for (Object fields : newFields) {
            JSONObject jsonTmp = new JSONObject(fields.toString(), false, true);
            String name = jsonTmp.getStr("name");
            JSONObject doc = new JSONObject(jsonTmp.getStr("doc"), false, true);
            if (ttlMap.containsKey(name)) {
                Integer ttlValue = ttlMap.get(name);
                if (ttlValue != null) {
                    doc.set("ttl", ttlMap.get(name));
                    logger.info("字段:{}的TTL值变更为：{}", name, ttlMap.get(name));
                }
            }

            if (visibilityMap.containsKey(name)) {
                String nowType = doc.get("visibility").toString();
                String oldType = visibilityMap.get(name);
                if (!oldType.equals(nowType)) {
                    if (!"hidden".equals(oldType)) {
                        if ("enabled".equals(nowType) || "disabled".equals(nowType)) {
                            doc.set("visibility", visibilityMap.get(name));
                            logger.info("字段:{}的visibility值变更为：{}", name, visibilityMap.get(name));
                        }
                    }
                }
            }
            jsonTmp.set("doc", doc);
            tmpFields.add(jsonTmp);
        }
        json.set("fields", tmpFields);

        return json.toStringPretty();
    }


    /**
     * 读取nacos内schema中内容，判断是否有需要处理的ttl字段和visibility字段
     *
     * @param schema schema内容
     * @return 是否需要处理当前表
     */
    private static boolean documentTypeJudgment(String filesName, String schema) {
        try {
            JSONObject json = new JSONObject(schema, false, true);
            if (json.containsKey("fields")) {
                JSONArray fields = json.getJSONArray("fields");
                for (Object field : fields) {
                    JSONObject jsonTmp = new JSONObject(field.toString(), false, true);
                    if (jsonTmp.containsKey("doc")) {
                        JSONObject doc = new JSONObject(jsonTmp.getStr("doc"), false, true);
                        if (doc.containsKey("ttl") || doc.containsKey("visibility")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(filesName + "解析JSON异常！异常信息为：" + e.getMessage());
        }
        return false;
    }


    private static void outMessage(boolean status, String dataId) {
        if (status) {
            logger.info("配置{}已成功推送至Nacos！", dataId);
        } else {
            logger.error("配置{}推送Nacos失败！", dataId);
        }
    }
}
