# SaveVar
[![Maven Central](https://img.shields.io/maven-central/v/cn.lalaki/save_var.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/cn.lalaki/save_var/)
![API: 26 + (shields.io)](https://img.shields.io/badge/API-26+-green)
![License: Apache-2.0 (shields.io)](https://img.shields.io/badge/License-Apache--2.0-brightgreen)

**将变量存储至配置文件的类实现，iceDesk配置文件类实现**

### Gradle
```gradle
implementation("cn.lalaki:save_var:$version")
```
### API

```kotlin
// 存储String变量至配置文件
SaveVar.INSTANCE.set("key", "value")

// 取出String变量
SaveVar.INSTANCE.get("key")

// 按照指定分隔符拼接String List为String存储至配置文件
SaveVar.INSTANCE.set("key", listOf("args0", "args1", "args2"), "&")

// 取出String变量，并按照分隔符拆分为List返回
SaveVar.INSTANCE.get("key", "&")
```

### by lalaki.cn