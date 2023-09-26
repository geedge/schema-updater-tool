# Schema Updater Tool



## 简介

Galaxy-qgw-service Schema版本更新工具包，用于不同版本之间的Schema更新，保留用户已配置的TTL和字段状态信息。

对ttl和visibility进行远程配置的拉取，进行对应规则的修改，并保存本地配置文件和推送修改后的文件到nacos

## 启动方式

```
java -jar schema-updater-tool-{version}.jar {schema folder} {nacos address} {nacos namespcae}
```

样例：
```
java -jar schema-updater-tool-3.1.jar schema/ 192.168.44.12 test
```

