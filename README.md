# wechat-bot-java

[![](https://img.shields.io/badge/Language-Java-ff96b4.svg)](https://github.com/masteranthoneyd/wechat-bot-java) [![](https://img.shields.io/badge/license-MIT-FF0080.svg)](https://github.com/masteranthoneyd/wechat-bot-java/blob/master/LICENSE) [![](https://img.shields.io/github/stars/masteranthoneyd/wechat-bot-java.svg?style=social)](https://github.com/masteranthoneyd/wechat-bot-java) [![](https://img.shields.io/github/followers/masteranthoneyd.svg?label=Follow%20Me&style=social)](https://github.com/masteranthoneyd)

`wechat-bot-java` 灵感来源于 *[EverydayWechat](https://github.com/sfyc23/EverydayWechat)*


## Feature

- [x] 每天给女朋友发送土味情话
- [x] 自动拉人进群
- [x] 群新增成员监听并发送欢迎语
- [x] 图灵AI自动回复
- [x] 自动同意添加好友

## Show

![](image/demo-01.jpg)

<div align=center><img width="50%" height="50%" src="image/demo-02.jpg"/><img width="50%" height="50%" src="image/demo-03.jpg"/></div>

## Config

请先前往图灵官网注册并获取apiKey, *[http://www.turingapi.com](http://www.turingapi.com)*

`config.yaml`:

```yml
openQrCode: true # 打开二维码, true会调用系统Desktop默认图片浏览工具打开, false在终端显示
loverPrattle: # 每天发送土味情话配置
  enable: true # 是否启动
  cron: 0 0 9 * * ? # 定时任务cron表达式
  city: 广州 # 位置, 用于获取天气
  nickName: 悦 # 女朋友昵称
  fallInLoveAt: 2014-02-15 # 相识日期, 用于计算当天天数
  sweetsWords: 来自养猪场的程序猿 # 钢铁直男的甜密后缀

autoReply: # 自动回复配置
  enable: true # 是否启用
  tulingApiKey: asdasdasdasdasd # 图灵apiKey
  nickNames: # 自动回复名单, 可配置多个, 群回复需要@
    - Bot567
  prefix: 【~(≧▽≦)~】 # 自动回复小尾巴
  
autoVerify: # 自动添加好友验证
  enable: true # 是否启用
  passMessage: # 验证信息包含以下列表时自动通过
    - Java
    - Github
```

## Quick Start

1. 将项目 Clone 下来
2. 打包 `mvn package`, 生成 `wechat-bot-java-0.0.1-SNAPSHOT.jar`
3. 在 `wechat-bot-java-0.0.1-SNAPSHOT.jar` 根目录下配置 `config.yaml`
4. 运行: `java -jar wechat-bot-java-0.0.1-SNAPSHOT.jar`
5. 终端输入 `exit` 或 `quit` 关闭程序

## TODO

- [ ] 接收队列与发送队列解耦, 实现多线程处理消息
