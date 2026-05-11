# 仓库指南

## 项目结构与模块组织

当前工作区关联两个 Java DDD 项目：`group-buy-market/` 和 `s-pay-mall-ddd/`。两者都是基于 Java 17、Spring Boot 2.7.12 的 Maven 多模块项目。常见模块包括：`*-lpc-api` 存放接口与 DTO，`*-lpc-app` 存放启动应用与测试，`*-lpc-domain` 存放业务规则，`*-lpc-infrastructure` 存放 MyBatis、Redis、MQ 和外部系统适配器，`*-lpc-trigger` 存放 Controller 与监听器，`*-lpc-types` 存放公共常量、枚举和异常。部署与本地服务配置位于 `docs/dev-ops/`，运行日志写入 `data/log/`。

## 构建、测试与开发命令

在要修改的项目目录下执行命令，例如 `cd group-buy-market` 或 `cd s-pay-mall-ddd`。

- `mvn clean package`：构建完整 Maven reactor。
- `mvn test`：在 Surefire 未跳过的模块中运行测试。
- `mvn -pl group-buy-market-lpc-app -am test`：测试指定模块及其依赖；商城项目请替换为对应模块名。
- `sh <project>-lpc-app/build.sh`：存在构建脚本时，生成应用 Docker 构建产物。
- `docker compose -f docs/dev-ops/docker-compose-environment.yml up -d`：启动 MySQL、Redis、RabbitMQ 等本地基础设施。

## 编码风格与命名规范

使用 Java 17、UTF-8 和四空格缩进。Controller 保持轻量，业务规则放入 `domain`，适配器细节放入 `infrastructure`。类名使用大驼峰，方法和字段使用小驼峰，常量使用 `UPPER_SNAKE_CASE`。接口通常以 `I` 开头，例如 `IOrderService`；API DTO 使用 `*RequestDTO` 和 `*ResponseDTO`；领域对象使用 `*Entity`、`*VO` 或 `*Aggregate`；持久化对象放在 `po` 包下。避免拼音命名和中英混合命名。

## 测试指南

测试主要位于各项目的 `*-lpc-app/src/test/java/.../test/` 目录，采用 JUnit 与 Spring Boot 测试模式。测试类按被测类或行为命名，并以 `Test` 结尾，例如 `OrderServiceRefundTest`。领域规则、MQ 监听器、Controller 和 DAO 变更应补充有针对性的测试。若 app 模块的 Maven 配置跳过测试，合并前应通过 IDE 运行指定测试类，或在本地覆盖 Surefire 配置后执行。

## 提交与 Pull Request 规范

近期提交使用 `feat:`、`fix:`、`docs:` 等约定式前缀。提交信息应简洁并说明范围，例如 `fix: correct refund price validation`。Pull Request 应说明变更模块、行为变化、验证命令，并明确标注数据库结构、MQ topic、支付/退款逻辑或公开 API 的影响。涉及用户可见 API 或界面变化时，附上截图或请求/响应示例。

## Agent 专用说明

本工作区使用根级 `code_copilot/` 作为 AI 协作规范目录。处理业务代码、跨项目联调、架构边界、数据契约、MQ、外部接口或多文件协调变更前，先阅读：

- `code_copilot/README.md`
- `code_copilot/rules/project-context.md`
- `code_copilot/rules/domain-rules.md`
- `code_copilot/rules/coding-style.md`
- `code_copilot/rules/security.md`
- `code_copilot/knowledge/index.md`

若变更只涉及某个子项目，还应读取该子项目的 `AGENTS.md`；若涉及 `s-pay-mall-ddd`，必须优先读取 `s-pay-mall-ddd/code_copilot/` 下更具体的规则与变更文档。规则冲突时，子项目级规则优先于根级规则，但必须在回复中说明冲突点。

## 语言策略

生成 `AGENTS.md`、`README.md`、贡献者指南、开发规范等仓库文档时，默认使用简体中文；只有当用户明确要求英文或其他语言时才切换。
