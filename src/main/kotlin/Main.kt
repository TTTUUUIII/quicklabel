import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser
import kotlin.io.path.name

const val MESSAGE_DIALOG = 1
const val EDITABLE_DIALOG = 2

var projectPath: Path = Paths.get("")

@Composable
@Preview
fun App(
    uiSamples: SnapshotStateList<DataSample>,
    labels: SnapshotStateList<String>
) {
    var shiftPressed = false
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedLabel by remember { mutableStateOf("") }
    var labeledCount by remember { mutableStateOf(0) }

    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .onKeyEvent { event ->
                        shiftPressed = event.isShiftPressed
                        true
                    },
            ) {
                if (uiSamples.isNotEmpty()) {
                    LazyRow {
                        items(labels.toList()) { label ->
                            Label(
                                text = label,
                                selected = selectedLabel == label,
                                onClick = {
                                    selectedLabel = if (selectedLabel == label) {
                                        ""
                                    } else {
                                        label
                                    }
                                }
                            )
                        }
                    }

                    if (labels.isNotEmpty() && selectedLabel.isEmpty()) {
                        selectedLabel = labels.first()
                    }
                }

                if (labels.isEmpty()) {
                    selectedLabel = ""
                }

                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (uiSamples.isNotEmpty()) {
                        Row {
                            Text("位置：${selectedIndex}")
                            Spacer(modifier = Modifier.width(18.dp))
                            Text("进度：${labeledCount}/${uiSamples.count()}")
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        val listState = rememberLazyGridState()
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(320.dp),
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            itemsIndexed(uiSamples) { index, sample ->
                                ItemSample(index = index, sample = sample, onClick = { isLabel ->
                                    if (isLabel) {
                                        sample.label = selectedLabel
                                        val snapshot = uiSamples.toList()
                                        uiSamples.clear()
                                        uiSamples.addAll(snapshot)
                                    } else {
                                        if (shiftPressed && selectedIndex != -1) {
                                            for (i in selectedIndex.coerceAtMost(index)..selectedIndex.coerceAtLeast(
                                                index
                                            )) {
                                                uiSamples[i].label = selectedLabel
                                            }
                                            val snapshot = uiSamples.toList()
                                            uiSamples.clear()
                                            uiSamples.addAll(snapshot)
                                        } else {
                                            selectedIndex = index
                                        }
                                    }
                                    labeledCount = 0
                                    uiSamples.forEach {
                                        if (it.isLabeled()) labeledCount++
                                    }
                                }, selected = selectedIndex == index)
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            style = ScrollbarStyle(
                                minimalHeight = 48.dp,
                                thickness = 8.dp,
                                shape = RoundedCornerShape(8.dp),
                                hoverDurationMillis = 300,
                                hoverColor = MaterialTheme.colors.primary,
                                unhoverColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Label(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Chip(
        content = { Text(text) },
        shape = RoundedCornerShape(3.dp),
        onClick = onClick,
        colors = ChipDefaults.chipColors(
            backgroundColor = if (selected) MaterialTheme.colors.primary else Color(0xFFB7B7B7)
        )
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemSample(
    index: Int,
    sample: DataSample,
    onClick: (Boolean) -> Unit,
    selected: Boolean
) {
    val bitmap = remember { loadImageBitmap(FileInputStream(sample.imgPath.toFile())) }
    Card(modifier = Modifier
        .width(320.dp)
        .height(240.dp),
        elevation = if (selected) 8.dp else 0.dp,
        onClick = {
            onClick(false)
        }) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = null,
            )
            Text(
                "${index + 1}#${sample.dataPath.name.removeSuffix(".txt")}",
                modifier = Modifier
                    .align(Alignment.TopStart),
                color = Color.Yellow
            )
            Text(
                if (sample.isLabeled()) "标签：${sample.label}" else "无标签",
                modifier = Modifier
                    .align(Alignment.TopEnd),
                color = if (sample.isLabeled()) Color.Green else Color.Red,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                content = { Text("标注") },
                onClick = {
                    onClick(true)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

fun exportLabels(samples: List<DataSample>) {
    val exportPath = projectPath.resolve("${projectPath.name}（已标注）")
    if (!exportPath.toFile().mkdir()) {
        throw IOException("无法创建导出目录 $exportPath")
    }
    samples.forEach {
        val labelPath = exportPath.resolve(it.label)
        if (!labelPath.toFile().exists() && !labelPath.toFile().mkdir()) {
            throw IOException("无法创建标签目录 $labelPath")
        }
        Files.copy(it.imgPath, labelPath.resolve(it.imgPath.name))
        Files.copy(it.dataPath, labelPath.resolve(it.dataPath.name))
    }
}

fun loadDataFromPath(): List<DataSample> {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val samples = mutableListOf<DataSample>()
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        Files.list(Paths.get(chooser.selectedFile.absolutePath))
            .forEach { imgPath ->
                if (imgPath.name.endsWith(".jpg")) {
                    val dataPath = imgPath.parent.resolve(imgPath.name.replace(".jpg", ".txt"))
                    if (dataPath.toFile().exists()) {
                        samples.add(DataSample(imgPath, dataPath))
                    }
                }
            }
    }
    projectPath = chooser.selectedFile.toPath()
    samples.sortBy { it.imgPath }
    return samples
}

data class DataSample(val imgPath: Path, val dataPath: Path, var label: String = "") {
    fun isLabeled(): Boolean = label.isNotEmpty()
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Quick Label"
    ) {
        val samples = remember { mutableStateListOf<DataSample>() }
        val labels = remember { mutableStateListOf<String>("0", "1", "2") }
        var dialogOpen by remember { mutableStateOf(false) }
        var dialogType by remember { mutableStateOf(MESSAGE_DIALOG) }
        var dialogTitle by remember { mutableStateOf("") }
        var dialogContent by remember { mutableStateOf("") }
        var dialogPlaceholder by remember { mutableStateOf("") }
        var dialogDismissCallback by remember { mutableStateOf<() -> Unit>({}) }
        MenuBar {
            Menu("文件") {
                Item("打开", onClick = {
                    samples.clear()
                    samples.addAll(loadDataFromPath())
                })
                Item("导出", onClick = {
                    if (samples.isNotEmpty() && samples.find { !it.isLabeled() } == null) {
                        exportLabels(samples)
                        dialogType = MESSAGE_DIALOG
                        dialogTitle = "提示"
                        dialogContent = "导出完成！"
                        dialogOpen = true
                    } else {
                        dialogType = MESSAGE_DIALOG
                        dialogTitle = "注意"
                        dialogContent = if (samples.isEmpty()) "无项目！" else "部分数据缺少标签，请检查！"
                        dialogOpen = true
                    }
                })
            }
            Menu("标签") {
                Item("新增", onClick = {
                    dialogTitle = "新增标签"
                    dialogContent = ""
                    dialogPlaceholder = "请输入标签名"
                    dialogType = EDITABLE_DIALOG
                    dialogDismissCallback = {
                        if (dialogContent.isNotEmpty() && !labels.contains(dialogContent)) {
                            labels.add(dialogContent)
                        }
                        println(dialogContent)
                    }
                    dialogOpen = true
                })
                Item(
                    "删除", onClick = {
                    labels.clear()
                })
            }
        }
        if (dialogOpen) {
            if (dialogType == MESSAGE_DIALOG) {
                DialogWindow(
                    onCloseRequest = { dialogOpen = false },
                    title = dialogTitle
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dialogContent)
                        }
                    }
                }
            } else if (dialogType == EDITABLE_DIALOG) {
                DialogWindow(
                    onCloseRequest = { dialogOpen = false },
                    title = dialogTitle
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            TextField(
                                value = dialogContent,
                                onValueChange = {
                                    dialogContent = it
                                },
                                placeholder = {
                                    Text(dialogPlaceholder)
                                }
                            )
                        }
                        TextButton(
                            onClick = {
                                dialogOpen = false
                                dialogDismissCallback()
                            },
                            content = { Text("确定") },
                            modifier = Modifier
                                .align(Alignment.End)
                        )
                    }
                }
            }
        }
        App(
            uiSamples = samples,
            labels = labels
        )
    }
}
