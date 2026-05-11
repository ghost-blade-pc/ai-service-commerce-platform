# 知识索引

本目录沉淀跨 change 可复用的项目知识。只有经过验证、可复用、不会误导后续开发的内容才应写入。

## 已确认知识

### 全局知识地图

- [platform-knowledge.md](platform-knowledge.md)：覆盖根工作区、`group-buy-market/`、`s-pay-mall-ddd/` 的项目结构、DDD 分层、跨项目调用、MQ 链路、补偿任务、配置、风险点和 Research 入口。

### 工作区结构

- 根目录是工作区，不是 Maven reactor；`group-buy-market/` 与 `s-pay-mall-ddd/` 是符号链接指向的独立 Maven 多模块项目。
- 两个子项目均采用 `api/app/domain/trigger/infrastructure/types` 的 DDD 分层。
- `s-pay-mall-ddd/` 已有子项目级 `code_copilot/`，处理该项目内部变更时应优先读取。

### 常见验证命令

```bash
cd group-buy-market && mvn clean package
cd s-pay-mall-ddd && mvn clean package
cd group-buy-market && mvn -pl group-buy-market-lpc-app -DskipTests=false test
cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test
```

部分 app 模块可能在 Maven Surefire 配置中默认跳过测试，执行测试时需按实际 POM 显式覆盖。

## 待沉淀

- TODO: 涉及具体 Mapper 或 Repository 改动时，按目标表重新校验字段映射。
- TODO: 涉及 MQ 可靠投递改造时，补充确认模式、重试、死信和消费幂等设计。
