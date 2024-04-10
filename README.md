# Schema Updater Tool



## 简介

Galaxy-qgw-service Schema版本更新工具包，用于不同版本之间的Schema更新，保留用户已配置的TTL和字段状态信息。

对ttl和visibility进行远程配置的拉取，进行对应规则的修改，并保存本地配置文件和推送修改后的文件到nacos

## 规则

- TTL状态均以线上为准，更新本地Schema属性。  
- Visibility状态共三种：hidden、enabled、disabled  
  - 若线上Schema内为hidden状态，则以本地Schema状态为准不予处理。  
  - 若本地Schema内为enabled或disabled状态，则以线上为准，更新本地Schema状态属性。  
- 在common_config.properties配置文件中non.schema.tables指定了qgw中不属于schema的文件列表（逗号分割），该类文件会直接推送至nacos。

## 启动方式

```
java -jar schema-updater-tool-{version}.jar {schema folder} {nacos address} {nacos namespcae}
```

样例：
```
java -jar schema-updater-tool-3.1.jar schema/ 192.168.44.12 test
```

## 日志信息
涉及TTL变更的Schema输出操作操作日志样例：
```
<================开始对monitor_event.json表进行更新操作================>
<字段:log_id的TTL值变更为：2592000>
<字段:session_id的TTL值变更为：2592000>
<字段:start_timestamp_ms的TTL值变更为：2592000>
<配置monitor_event.json已成功推送至Nacos！>
```
不包含TTL的Schema输出日志样例：
```
<================开始对application_protocol_stat.json表进行更新操作================>
<该表不包含表TTL信息!>
<配置application_protocol_stat.json已成功推送至Nacos！>
```
non.schema.tables指定的文件和Nacos中不存在的文件会直接推送：
```
<public_code_info.json非Schema表，直接推送至Nacos>
<配置public_code_info.json已成功推送至Nacos！>

<Nacos未查询到top_server_countries.json，判断为新文件，直接推送至Nacos>
<配置top_server_countries.json已成功推送至Nacos！>
```
异常则会有相关的ERROR日志。
