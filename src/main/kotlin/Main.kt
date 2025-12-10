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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.sun.tools.javac.Main
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser
import kotlin.io.path.name
import kotlin.io.path.outputStream

const val MESSAGE_DIALOG = 1
const val EDITABLE_DIALOG = 2

private var projectPath: Path? = null
private var savePath: Path? = null

@Composable
@Preview
fun App(
    projectName: String,
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
                            Text("项目：${projectName}", color = Color.Blue)
                            Spacer(modifier = Modifier.width(18.dp))
                            Text("位置：${selectedIndex}")
                            Spacer(modifier = Modifier.width(18.dp))
                            Text("进度：${labeledCount}/${uiSamples.count()}")
                            Spacer(modifier = Modifier.width(24.dp))
                            labels.forEach { label ->
                                Text("标签$label：${uiSamples.count { it.label == label }}个")
                                Spacer(modifier = Modifier.width(8.dp))
                            }
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
    val imagePainter = remember {
        try {
            BitmapPainter(loadImageBitmap(FileInputStream(sample.imgPath.toFile())))
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            loadSvgPainterFromResource("ic_broken_image", Density(10f))
        }
    }
    Card(modifier = Modifier
        .width(320.dp)
        .height(240.dp),
        backgroundColor = if (selected) Color.Gray else MaterialTheme.colors.surface,
        onClick = {
            onClick(false)
        }) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = imagePainter,
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

fun exportLabeledData(samples: List<DataSample>, path: Path? = null) {
    val exportPath = path?.resolve("${projectPath!!.name}（已标注）")
        ?: projectPath!!.resolve("${projectPath!!.name}（已标注）")
    if (!exportPath.toFile().mkdir()) {
        throw IOException("无法创建导出目录 $exportPath")
    }
    for (it in samples) {
        if (it.label.lowercase() == "trash") {
            continue
        }
        val labelPath = exportPath.resolve(it.label)
        if (!labelPath.toFile().exists() && !labelPath.toFile().mkdir()) {
            throw IOException("无法创建标签目录 $labelPath")
        }
        Files.copy(it.imgPath, labelPath.resolve(it.imgPath.name))
        Files.copy(it.dataPath, labelPath.resolve(it.dataPath.name))
    }
}

fun chooseDirectory(base: File? = null): File? {
    val chooser = JFileChooser(base)
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        return chooser.selectedFile
    }
    return null
}

fun loadDataFromPath(): List<DataSample> {
    val samples = mutableListOf<DataSample>()
    chooseDirectory(projectPath?.parent?.toFile())?.let {
        Files.list(Paths.get(it.absolutePath))
            .forEach { imgPath ->
                if (imgPath.name.endsWith(".jpg")) {
                    val dataPath = imgPath.parent.resolve(imgPath.name.replace(".jpg", ".txt"))
                    if (dataPath.toFile().exists()) {
                        samples.add(DataSample(imgPath, dataPath))
                    }
                }
            }
        projectPath = it.toPath()
        samples.sortBy { it.imgPath }
    }
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
        var projectName by remember { mutableStateOf("") }
        val samples = remember { mutableStateListOf<DataSample>() }
        val labels = remember { mutableStateListOf<String>("0", "1", "2", "Trash") }
        var dialogOpen by remember { mutableStateOf(false) }
        var dialogType by remember { mutableStateOf(MESSAGE_DIALOG) }
        var dialogTitle by remember { mutableStateOf("") }
        var dialogContent by remember { mutableStateOf("") }
        var dialogPlaceholder by remember { mutableStateOf("") }
        var dialogDismissCallback by remember { mutableStateOf<() -> Unit>({}) }
        MenuBar {
            val openIcon = remember {
                loadSvgPainterFromResource("ic_folder_open")
            }
            val saveIcon = remember {
                loadSvgPainterFromResource("ic_save")
            }
            val saveAsIcon = remember {
                loadSvgPainterFromResource("ic_save_as")
            }
            val addIcon = rememberVectorPainter(Icons.Outlined.Add)
            val delIcon = rememberVectorPainter(Icons.Outlined.Delete)
            Menu("文件") {
                Item("打开", icon = openIcon, onClick = {
                    val newSamples = loadDataFromPath()
                    if (newSamples.isNotEmpty()) {
                        projectName = projectPath!!.name
                        samples.clear()
                        samples.addAll(newSamples)
                    }
                })
                Item("保存", icon = saveIcon, onClick = {
                    val needLabelSampleNames = samples
                        .filter { !it.isLabeled() }
                        .map { it.dataPath.name }
                    if (samples.isNotEmpty() && needLabelSampleNames.isEmpty()) {
                        exportLabeledData(samples)
                        dialogType = MESSAGE_DIALOG
                        dialogTitle = "提示"
                        dialogContent = "保存完成！"
                        dialogOpen = true
                    } else {
                        dialogType = MESSAGE_DIALOG
                        dialogTitle = "注意"
                        dialogContent = if (samples.isEmpty()) "无项目！" else "数据${needLabelSampleNames}缺少标签，请检查！"
                        dialogOpen = true
                    }
                })
                Item("保存至", icon = saveAsIcon, onClick = {
                    val needLabelSampleNames = samples
                        .filter { !it.isLabeled() }
                        .map { it.dataPath.name }
                    if (samples.isNotEmpty() && needLabelSampleNames.isEmpty()) {
                        chooseDirectory(savePath?.toFile())?.let {
                            exportLabeledData(samples, it.toPath())
                            dialogType = MESSAGE_DIALOG
                            dialogTitle = "提示"
                            dialogContent = "保存完成！"
                            dialogOpen = true
                            savePath = it.toPath()
                        }
                    } else {
                        dialogType = MESSAGE_DIALOG
                        dialogTitle = "注意"
                        dialogContent = if (samples.isEmpty()) "无项目！" else "数据${needLabelSampleNames}缺少标签，请检查！"
                        dialogOpen = true
                    }
                })
            }
            Menu("标签") {
                Item("新增", icon = addIcon, onClick = {
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
                    "删除", icon = delIcon, onClick = {
                    labels.clear()
                })
            }
        }
        if (dialogOpen) {
            if (dialogType == MESSAGE_DIALOG) {
                DialogWindow(
                    onCloseRequest = { dialogOpen = false },
                    title = dialogTitle,
                    state = DialogState(width = 280.dp, height = 210.dp)
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
                            Text(dialogContent, fontSize = TextUnit(16f, TextUnitType.Sp))
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
            projectName = projectName,
            uiSamples = samples,
            labels = labels
        )
    }
}

fun loadSvgPainterFromResource(resName: String): Painter = loadSvgPainterFromResource(resName,
    Density(1.0f))

fun loadSvgPainterFromResource(resName: String, density: Density): Painter = if (resName.lowercase().endsWith(".svg")) {
    loadSvgPainter(Main::class.java.classLoader.getResourceAsStream(resName)!!, density)
} else {
    loadSvgPainter(Main::class.java.classLoader.getResourceAsStream("${resName}.svg")!!, density)
}