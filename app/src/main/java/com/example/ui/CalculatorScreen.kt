package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CalculationHistory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val realtimeResult by viewModel.realtimeResult.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var showHistory by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10)) // Rich matte dark background
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("app_title")
                )
                IconButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.testTag("history_button")
                ) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.Close else Icons.Default.History,
                        contentDescription = "History",
                        tint = if (showHistory) Color(0xFFFF9F0A) else Color.White
                    )
                }
            }

            if (isLandscape) {
                // Landscape Split Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Display on Left
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        DisplayArea(
                            expression = expression,
                            realtimeResult = realtimeResult
                        )
                    }

                    // Keypad on Right
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(8.dp)
                    ) {
                        KeypadGrid(
                            onButtonClick = viewModel::onButtonClick,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Inline History overlay over Keypad
                        HistoryPanelOverlay(
                            showHistory = showHistory,
                            historyList = historyList,
                            onItemClick = { history ->
                                viewModel.onHistoryItemClick(history)
                                showHistory = false
                            },
                            onDeleteClick = viewModel::deleteHistoryItem,
                            onClearAll = viewModel::clearHistory,
                            onClose = { showHistory = false }
                        )
                    }
                }
            } else {
                // Portrait Standard Layout
                // Display Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    DisplayArea(
                        expression = expression,
                        realtimeResult = realtimeResult
                    )
                }

                // Keypad Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    KeypadGrid(
                        onButtonClick = viewModel::onButtonClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sliding History panel overlay over Keypad
                    HistoryPanelOverlay(
                        showHistory = showHistory,
                        historyList = historyList,
                        onItemClick = { history ->
                            viewModel.onHistoryItemClick(history)
                            showHistory = false
                        },
                        onDeleteClick = viewModel::deleteHistoryItem,
                        onClearAll = viewModel::clearHistory,
                        onClose = { showHistory = false }
                    )
                }
            }
        }
    }
}

@Composable
fun DisplayArea(
    expression: String,
    realtimeResult: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active math input
        Text(
            text = expression.ifEmpty { "0" },
            color = Color.White,
            fontSize = if (expression.length > 12) 32.sp else 44.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 48.sp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expression_display")
        )

        // Dynamically evaluated preview output
        if (realtimeResult.isNotEmpty()) {
            Text(
                text = realtimeResult,
                color = Color(0xFF8E8E93), // iOS Muted Slate Gray
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_display")
            )
        }
    }
}

@Composable
fun KeypadGrid(
    onButtonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttons = listOf(
        listOf("AC", "()", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("⌫", "0", ".", "=")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.15f)
                    ) {
                        CalculatorButton(
                            symbol = char,
                            onClick = { onButtonClick(char) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOperator = symbol in listOf("÷", "×", "-", "+", "=")
    val isAction = symbol in listOf("AC", "()", "%", "⌫")

    val backgroundColor = when {
        symbol == "=" -> Color(0xFFFF9F0A) // Vibrant tactile orange
        isOperator -> Color(0xFFFF9F0A) // Vibrant operator color
        isAction -> Color(0xFF2C2D30) // Darker cool actions
        else -> Color(0xFF1E1F22) // Classic dark key
    }

    val contentColor = when {
        isOperator -> Color.White
        isAction -> Color(0xFFE5E5EA)
        else -> Color.White
    }

    val textStyle = when {
        isAction -> androidx.compose.ui.text.TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        else -> androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .testTag("button_$symbol"),
        contentAlignment = Alignment.Center
    ) {
        if (symbol == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = symbol,
                color = contentColor,
                style = textStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryPanelOverlay(
    showHistory: Boolean,
    historyList: List<CalculationHistory>,
    onItemClick: (CalculationHistory) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showHistory,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1F22) // Sophisticated slate container overlay
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Toolbar in history
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (historyList.isNotEmpty()) {
                            IconButton(
                                onClick = onClearAll,
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = "Clear All History",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "No History",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No calculations yet",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList, key = { it.id }) { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2D30))
                                    .clickable { onItemClick(history) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = history.expression,
                                        color = Color.LightGray,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "= ${history.result}",
                                        color = Color(0xFFFF9F0A),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteClick(history.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete record",
                                        tint = Color(0xFFFF453A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
