import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser
import kotlin.io.path.name

var projectPath: Path = Paths.get("")
var dataSamples = mutableListOf<DataSample>()

@Composable
@Preview
fun App() {
    var shiftPressed = false
    var selectedIndex by remember { mutableStateOf(-1) }
    var selectedLabel by remember { mutableStateOf("0") }
    var labeledCount by remember { mutableStateOf(0) }
    val dataLabels = remember { mutableStateListOf<String>("0", "1", "2") }
    val uiSamples = remember { mutableStateListOf<DataSample>() }

    MaterialTheme {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .onKeyEvent { event ->
                    shiftPressed = event.isShiftPressed
                    true
                },
            ) {
                var text by remember { mutableStateOf("") }

                TextButton(
                    content = { Text("打开项目") },
                    onClick = {
                        loadDataFromPath()
                        uiSamples.clear()
                        uiSamples.addAll(dataSamples)
                    }
                )

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = text,
                        placeholder = { Text("请输入标签") },
                        singleLine = true,
                        onValueChange = {
                            text = it
                        }
                    )
                    TextButton(
                        content = { Text("添加标签") },
                        onClick = {
                            if (text.isNotEmpty()) {
                                dataLabels.add(text)
                                text = ""
                            }
                        }
                    )

                    TextButton(
                        content = { Text("清除标签") },
                        onClick = {
                            dataLabels.clear()
                        }
                    )
                }
                LazyRow {
                    items(dataLabels.toList()) {label ->
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

                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text("进度：${labeledCount}/${dataSamples.count()}")
                    Row(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        val listState = rememberLazyListState()
                        LazyColumn (
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            itemsIndexed(uiSamples) { index, sample ->
                                ItemSample(index = index, sample = sample, onClick = { isLabel ->
                                    if (isLabel) {
                                        sample.label = selectedLabel
                                        uiSamples.clear()
                                        uiSamples.addAll(dataSamples)
                                    } else {
                                        if (shiftPressed && selectedIndex != -1) {
                                            for (i in selectedIndex.coerceAtMost(index) .. selectedIndex.coerceAtLeast(index)) {
                                                dataSamples[i].label = selectedLabel
                                            }
                                            uiSamples.clear()
                                            uiSamples.addAll(dataSamples)
                                        } else {
                                            selectedIndex = index
                                        }
                                    }
                                    labeledCount = 0
                                    dataSamples.forEach {
                                        if (it.isLabeled()) labeledCount++
                                    }
                                }, selected = selectedIndex == index)
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState)
                        )
                    }

                    Button(
                        onClick = {
                            exportLabels()
                            scope.launch {
                                snackbarHostState.showSnackbar("导出完成！")
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End),
                        enabled = labeledCount != 0 && labeledCount == dataSamples.size,
                        content = { Text("导出标签") }
                    )
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
            backgroundColor = if (selected) MaterialTheme.colors.primary else Color.Gray
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
        .fillMaxWidth()
        .height(240.dp),
        elevation = if (selected) 8.dp else 0.dp,
        onClick = {
            onClick(false)
        }) {
        Row (
            modifier = Modifier.fillMaxSize()
        ) {
            Box {
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
                    color = if (sample.isLabeled()) Color.Green else Color.Red
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(end = 18.dp)
            ) {
                OutlinedButton(
                    content = { Text("打标签")},
                    onClick = {
                        onClick(true)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                )
            }
        }
    }
}

fun exportLabels() {
    val exportPath = projectPath.resolve("${projectPath.name}_已标注完成")
    if (!exportPath.toFile().mkdir()) {
        throw IOException("无法创建导出目录 $exportPath")
    }
    dataSamples.forEach {
        val labelPath = exportPath.resolve(it.label)
        if (!labelPath.toFile().exists() && !labelPath.toFile().mkdir()) {
            throw IOException("无法创建标签目录 $labelPath")
        }
        Files.copy(it.imgPath, labelPath.resolve(it.imgPath.name))
        Files.copy(it.dataPath, labelPath.resolve(it.dataPath.name))
    }
}

fun loadDataFromPath() {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    dataSamples.clear()
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        Files.list(Paths.get(chooser.selectedFile.absolutePath))
            .forEach {imgPath ->
                if (imgPath.name.endsWith(".jpg")) {
                    val dataPath = imgPath.parent.resolve(imgPath.name.replace(".jpg", ".txt"))
                    if (dataPath.toFile().exists()) {
                        dataSamples.add(DataSample(imgPath, dataPath))
                    }
                }
            }
    }
    projectPath = chooser.selectedFile.toPath()
    dataSamples.sortBy { it.imgPath }
}

data class DataSample(val imgPath: Path, val dataPath: Path, var label: String = "") {
    fun isLabeled(): Boolean = label.isNotEmpty()
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Quick Label"
    ) {
        App()
    }
}
