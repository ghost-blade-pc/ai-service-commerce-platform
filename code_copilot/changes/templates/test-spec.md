# 测试 Spec — 需求名称
> status: propose | apply | done
> created: YYYY-MM-DD

## 1. 测试目标

- 验证的业务规则：
- 验证的风险点：
- 不覆盖的内容及原因：

## 2. 当前测试框架

| 项目 | 测试目录 | 参考测试 | 说明 |
| --- | --- | --- | --- |
| group-buy-market | `group-buy-market-lpc-app/src/test/java/top/licodetech/market/test/` | TODO | SpringBoot 测试与领域/DAO/Controller 测试 |
| s-pay-mall-ddd | `s-pay-mall-ddd-lpc-app/src/test/java/top/licodetech/mall/test/` | TODO | SpringBoot 测试与领域/Listener 测试 |

## 3. 测试范围

### P0 — 领域规则

| 类/方法 | 场景 | 输入 | Mock/准备数据 | 预期 |
| --- | --- | --- | --- | --- |

### P1 — 基础设施适配

| 对象 | 场景 | 类型 | 验证点 |
| --- | --- | --- | --- |

### P2 — 入口层与联调

| 对象 | 场景 | 类型 | 验证点 |
| --- | --- | --- | --- |

## 4. 执行计划

- [ ] 先运行现有相关测试，确认基线
- [ ] 添加或调整 P0 测试
- [ ] 按风险补充 P1/P2 测试
- [ ] 运行目标模块测试
- [ ] 记录命令、结果和失败原因

## 5. 建议命令

```bash
cd group-buy-market && mvn -pl group-buy-market-lpc-app -DskipTests=false test
cd s-pay-mall-ddd && mvn -pl s-pay-mall-ddd-lpc-app -DskipTests=false test
```

## 6. 测试结论

| 命令 | 结果 | 备注 |
| --- | --- | --- |
