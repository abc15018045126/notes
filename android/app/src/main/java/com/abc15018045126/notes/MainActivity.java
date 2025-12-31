package com.abc15018045126.notes;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 在 super 之前注册插件，确保初始化时已被 Bridge 识别
        registerPlugin(OpenFolderPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
