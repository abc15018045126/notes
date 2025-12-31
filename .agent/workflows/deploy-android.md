---
description: 一键编译、同步并安装到安卓设备 (Automatic build and deploy to Android)
---

This workflow automates the process of copying your web assets to the Android project and installing the APK on your connected device.

1. Ensure your phone is connected via USB and USB Debugging is enabled.
2. Run the following command in the project root:

# // turbo
```bash
npm run run
```

Alternatively, if you want a more "raw" installation:

# // turbo
```bash
npx cap copy android && cd android && ./gradlew.bat installDebug
```

*Note: The `run` command will ask you to select a device if multiple are connected.*
