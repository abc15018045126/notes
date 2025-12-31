package com.abc15018045126.notes;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;

@CapacitorPlugin(name = "OpenFolder")
public class OpenFolderPlugin extends Plugin {

    @PluginMethod
    public void open(PluginCall call) {
        try {
            Context context = getContext();
            
            // 重要：获取 Capacitor Filesystem 插件使用的 Documents 根目录
            File docFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File quickNotesFolder = new File(docFolder, "QuickNotes");
            
            if (!quickNotesFolder.exists()) {
                quickNotesFolder.mkdirs();
            }

            // 获取 FileProvider URI
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", quickNotesFolder);
            
            // 尝试多种 Intent 协议以适配不同的文件管理器
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "vnd.android.document/directory");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                context.startActivity(intent);
                call.resolve();
            } catch (Exception e1) {
                // 备用方案 1
                try {
                    Intent intent2 = new Intent(Intent.ACTION_VIEW);
                    intent2.setDataAndType(contentUri, "resource/folder");
                    intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent2);
                    call.resolve();
                } catch (Exception e2) {
                    // 备用方案 2：直接弹出通用内容分发
                    Intent intent3 = new Intent(Intent.ACTION_GET_CONTENT);
                    intent3.setDataAndType(contentUri, "*/*");
                    intent3.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent3.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent3);
                    call.resolve();
                }
            }
        } catch (Exception e) {
            call.reject("无法打开管理器: " + e.getMessage());
        }
    }
}
