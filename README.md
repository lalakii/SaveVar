# SaveVarEnhanced

[![Maven Central](https://img.shields.io/maven-central/v/cn.lalaki/SaveVarEnhanced.svg?label=Maven%20Central&logo=sonatype)](https://central.sonatype.com/artifact/cn.lalaki/SaveVarEnhanced/)
![API: 26+ (shields.io)](https://img.shields.io/badge/API-26+-2f9b45?logo=android)
[![License](https://img.shields.io/badge/License-MIT-0069D9?logo=github)](./LICENSE)

**将变量存储至配置文件的类实现，iceDesk配置文件类实现**

### Gradle
```gradle
implementation("cn.lalaki:SaveVarEnhanced:$version")
```
### API

```kotlin
// init

if (SaveVarManager.init(configPath, TEST_AES_KEY_ARRAY)) {
    // load config success
} else {
           
}

// Save vars
SaveVarManager.addToOrderedStringSet("linkedHashSet","1")
SaveVarManager.addToOrderedStringSet("linkedHashSet","2")
SaveVarManager.addToOrderedStringSet("linkedHashSet","3")

// or
SaveVarManager.addToOrderedStringSet("linkedHashSet",listOf<String>())

// Get vars
SaveVarManager.getOrderedStringSet("linkedHashSet")
SaveVarManager.getStringList("list")
SaveVarManager.get("a_String")
```

### by lalaki.cn
