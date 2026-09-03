# Security Policy

## Supported Versions

| Version     | Supported          |
|-------------|--------------------|
| 0.0.x       | :white_check_mark: |

## Security Notes（部署前必读）

Voyager1 内置了若干**仅用于开发环境的默认密钥/口令**。**在任何生产或公网部署前，你必须更换它们**，否则使用默认值部署的实例将处于已知密钥风险中。

| 项 | 位置 | 覆盖方式 |
|---|---|---|
| Server Token | `.env` / `env-*.env` 中的 `SERVER_TOKEN` | 部署时改为随机值（如 `uuidgen`） |
| AES 加密密钥 | `modules/sub-plugin/encrypt/.../AESEncryptor.java` 的默认值 `Djnn3runZBzdv9Nv` | 环境变量 `VOYAGER1_ENCRYPT_AES_KEY` |
| JWT 签名密钥 | `modules/server/.../configuration/UserConfig.java` 的默认值 `KZQfFBJTW2v6obS1` | 配置 `web.token-jwt-key`（`application.yml`） |

> 默认值仅用于让项目“开箱即跑”，不代表可用于生产。更换密钥后，之前用旧密钥加密的数据（如云账号 SecretKey）与已签发的登录 token 将失效，请一并评估。

## Reporting a Vulnerability

请通过 **GitHub 私有安全通告**（Security → Report a vulnerability）报告安全漏洞，**不要**在公开 issue 中披露。

我们会：
- 在 3 个工作日内确认收到并评估；
- 在确认后尽快提供修复版本，并在修复发布后公开披露（默认遵循 90 天协调披露期）；
- 若你希望在特定时间/版本披露，请在报告中说明。

严重性低、不影响安全性的普通问题，可直接提公开 issue。
