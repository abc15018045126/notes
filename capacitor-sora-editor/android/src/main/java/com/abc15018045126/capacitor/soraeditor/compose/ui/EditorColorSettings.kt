package com.abc15018045126.capacitor.soraeditor.compose.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abc15018045126.capacitor.soraeditor.compose.EditorUiState
import com.abc15018045126.capacitor.soraeditor.compose.EditorViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorColorSettings(
    uiState: EditorUiState,
    viewModel: EditorViewModel,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("主题颜色自定义 (也可在下面 JSON 自由配置)", style = MaterialTheme.typography.titleMedium)
        val colors = listOf("#FFFFFF" to "白", "#F5F5F5" to "灰", "#E0E0E0" to "深灰", "#FFF8DC" to "米", "#E8F5E9" to "绿", "#E3F2FD" to "蓝", "#000000" to "黑")
        
        Column {
            Text("编辑器背景 (Editor)", fontSize = 12.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.backgroundColor == c) { viewModel.setBackgroundColor(context, c) }
                }
            }
        }

        Column {
            Text("编辑器字体颜色 (Text Color)", fontSize = 12.sp, color = Color.Gray)
            val textColors = listOf("auto" to "自动", "#FF000000" to "黑", "#FFFFFFFF" to "白", "#FF888888" to "灰", "#FF0000FF" to "蓝", "#FFFF0000" to "红", "#FF00FF00" to "绿")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                textColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.editorTextColor == c) { viewModel.setEditorTextColor(context, c) }
                }
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("应用 UI 颜色 (Toolbar/Bottom)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    viewModel.setTocColor(context, uiState.uiColor)
                    viewModel.setSearchColor(context, uiState.uiColor)
                    viewModel.setMenuColor(context, uiState.uiColor)
                }) {
                    Text("同步到所有面板", fontSize = 10.sp)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.uiColor == c) { viewModel.setUiColor(context, c) }
                }
            }
        }

        Column {
            Text("更多菜单颜色 (More Menu)", fontSize = 12.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.menuColor == c) { viewModel.setMenuColor(context, c) }
                }
            }
        }

        Column {
            Text("目录面板颜色 (TOC)", fontSize = 12.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.tocColor == c) { viewModel.setTocColor(context, c) }
                }
            }
        }

        Column {
            Text("搜索面板颜色 (Search)", fontSize = 12.sp, color = Color.Gray)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.searchColor == c) { viewModel.setSearchColor(context, c) }
                }
            }
        }

        Column {
            Text("搜索匹配高亮颜色 (Search Match highlight)", fontSize = 12.sp, color = Color.Gray)
            val matchColors = listOf(
                "#FFF59D" to "淡黄", 
                "#C8E6C9" to "淡绿", 
                "#FFCDD2" to "淡红", 
                "#B2EBF2" to "淡蓝", 
                "#E1BEE7" to "淡紫", 
                "#FFE0B2" to "淡橙", 
                "#BBDEFB" to "灰蓝"
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                matchColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.searchMatchBackgroundColor == c) { viewModel.setSearchMatchBackgroundColor(context, c) }
                }
            }
        }

        Column {
            Text("当前行高亮颜色 (Current Line)", fontSize = 12.sp, color = Color.Gray)
            val highlightColors = listOf(
                "#00000000" to "无",
                "#10000000" to "淡黑",
                "#10888888" to "淡灰",
                "#100000FF" to "淡蓝",
                "#10FF0000" to "淡红",
                "#1000FF00" to "淡绿",
                "#20FFEB3B" to "浅黄"
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                highlightColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.currentLineBackgroundColor == c) { viewModel.setCurrentLineBackgroundColor(context, c) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("光标颜色 (Cursor Color)", fontSize = 12.sp, color = Color.Gray)
            val cursorColors = listOf("#FF000000" to "黑", "#FF888888" to "灰", "#FF0000FF" to "蓝", "#FFFF0000" to "红", "#FF00FF00" to "绿", "#FFFB8C00" to "橙", "#FF1976D2" to "深蓝")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cursorColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.cursorColor == c) { viewModel.setCursorColor(context, c) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("光标提手颜色 (Handle Color)", fontSize = 12.sp, color = Color.Gray)
            val handleColors = listOf("#FF000000" to "黑", "#FF888888" to "灰", "#FF0000FF" to "蓝", "#FFFF0000" to "红", "#FF00FF00" to "绿", "#FFFB8C00" to "橙", "#FF1976D2" to "深蓝")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                handleColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.handleColor == c) { viewModel.setHandleColor(context, c) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("行号颜色", fontSize = 12.sp, color = Color.Gray)
            val lnColors = listOf("#FF000000" to "黑", "#FF888888" to "灰", "#A0888888" to "浅灰", "#FF0000FF" to "蓝", "#FFFF0000" to "红")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lnColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.lineNumberColor == c) { viewModel.setLineNumberColor(context, c) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("分隔线颜色", fontSize = 12.sp, color = Color.Gray)
            val divColors = listOf("#A0888888" to "默认灰", "#A0000000" to "黑", "#A0FF0000" to "红", "#A000FF00" to "绿", "#A00000FF" to "蓝")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                divColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.lineDividerColor == c) { viewModel.setLineDividerColor(context, c) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("滚动条颜色 (Scrollbar Color)", fontSize = 12.sp, color = Color.Gray)
            val scrollbarColors = listOf("#A0888888" to "默认灰", "#A0000000" to "黑", "#A0FF0000" to "红", "#A000FF00" to "绿", "#A00000FF" to "蓝", "#A0FB8C00" to "橙", "#A01976D2" to "深蓝")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                scrollbarColors.forEach { (c, l) -> 
                    ColorOption(c, l, uiState.scrollbarColor == c) { viewModel.setScrollbarColor(context, c) }
                }
            }
        }
    }
}

@Composable
fun ColorOption(color: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable { onClick() }) {
        Box(Modifier.size(40.dp).background(try { Color(android.graphics.Color.parseColor(color)) } catch(e:Exception) { Color.Gray }, RoundedCornerShape(20.dp)).border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(20.dp)))
        Text(label, fontSize = 10.sp)
    }
}
