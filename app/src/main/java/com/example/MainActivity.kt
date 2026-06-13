package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MazeSimulationScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun MazeSimulationScreen(modifier: Modifier = Modifier, viewModel: SimulationViewModel = viewModel()) {
  val state by viewModel.state.collectAsState()

  // Colors from theme
  val bgLight = Color(0xFFFEF7FF)
  val textPrimary = Color(0xFF1D1B20)
  val textSecondary = Color(0xFF49454F)
  val primaryColor = Color(0xFF6750A4)
  val primaryContainer = Color(0xFFEADDFF)
  val onPrimaryContainer = Color(0xFF21005D)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(bgLight)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        IconButton(onClick = { viewModel.generateNewMaze(11) }) {
          Text("🎲", fontSize = 24.sp, color = textSecondary)
        }
        Text(
          text = "MazeML Simulator",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Medium,
          color = textPrimary
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = { viewModel.togglePolicy() }) {
          Text(if(state.showPolicy) "👁" else "🙈", fontSize = 24.sp, color = textSecondary)
        }
      }
    }

    // Scrollable Content
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Stats Card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "ACTIVE LEARNING SESSION",
              style = MaterialTheme.typography.labelSmall,
              color = primaryColor,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 1.sp
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(modifier = Modifier.size(8.dp).background(Color(0xFFB3261E), androidx.compose.foundation.shape.CircleShape))
              Text("Live Training", style = MaterialTheme.typography.labelSmall, color = textPrimary)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            StatsColumn(value = state.episode.toString(), label = "EPISODE")
            StatsColumn(value = state.steps.toString(), label = "STEPS")
            StatsColumn(value = state.totalReward.toInt().toString(), label = "REWARD")
          }
        }
      }

      // Maze Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f) // Ensure it's square
          .background(Color(0xFFE7E0EC), RoundedCornerShape(24.dp))
          .border(2.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp))
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        MazeCanvas(
          state = state,
          modifier = Modifier.fillMaxSize()
        )

        // Legend Overlay
        Row(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = (-8).dp, y = (-8).dp)
            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(Modifier.size(8.dp).background(Color(0xFF6750A4), androidx.compose.foundation.shape.CircleShape))
            Text("Agent", fontSize = 10.sp, fontWeight = FontWeight.Medium)
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(Modifier.size(8.dp).background(Color(0xFFFFD700), androidx.compose.foundation.shape.CircleShape))
            Text("Reward", fontSize = 10.sp, fontWeight = FontWeight.Medium)
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(Modifier.size(8.dp).background(Color(0xFF34C759), androidx.compose.foundation.shape.CircleShape))
            Text("Exit", fontSize = 10.sp, fontWeight = FontWeight.Medium)
          }
        }
      }

      // AI Insights Sheet
      AnimatedVisibility(visible = state.isThinking || state.aiThoughts.isNotEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFECE6F0)),
          shape = RoundedCornerShape(24.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("✨", fontSize = 20.sp)
              Text(
                text = "AI Insights",
                style = MaterialTheme.typography.titleMedium,
                color = textPrimary,
                fontWeight = FontWeight.Medium
              )
            }
            Spacer(Modifier.height(12.dp))
            if (state.isThinking) {
              CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = primaryColor
              )
              Spacer(Modifier.height(8.dp))
              Text(
                "Analyzing deeply...",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = textSecondary
              )
            } else {
              Text(
                text = state.aiThoughts,
                style = MaterialTheme.typography.bodyMedium,
                color = textPrimary,
                lineHeight = 24.sp
              )
            }
          }
        }
      }
    }

    // Bottom Control Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(bgLight)
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left Button: Ask AI
      Button(
        onClick = { viewModel.askAiAboutLearning() },
        modifier = Modifier.weight(1f).height(48.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = primaryContainer,
          contentColor = onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp)
      ) {
        Text("✨ Ask AI", fontWeight = FontWeight.Medium)
      }

      Spacer(Modifier.width(16.dp))

      // Center Action: Play/Pause
      Button(
        onClick = { viewModel.toggleSimulation() },
        modifier = Modifier.size(64.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = primaryColor,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
      ) {
        Text(
          if (state.isRunning) "⏸" else "▶", // Double vertical bars for pause or triangle for play
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(Modifier.width(16.dp))

      // Right Button: Speed
      Button(
        onClick = {
            viewModel.simulationSpeedMs = if (viewModel.simulationSpeedMs == 100L) 10L else 100L
        },
        modifier = Modifier.weight(1f).height(48.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = primaryContainer,
          contentColor = onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp)
      ) {
        Text(
          if (viewModel.simulationSpeedMs == 100L) "1.0x" else "10.0x",
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun StatsColumn(value: String, label: String) {
  Column {
    Text(
      text = value,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Light,
      color = Color(0xFF1D1B20)
    )
    Text(
      text = label,
      fontSize = 10.sp,
      color = Color(0xFF49454F),
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.5.sp
    )
  }
}

@Composable
fun MazeCanvas(state: SimulationState, modifier: Modifier = Modifier) {
  val maze = state.maze
  val mousePos = state.mousePosition
  val pathTaken = state.pathTaken
  val qTable = state.qTableCopy
  val showPolicy = state.showPolicy

  val wallColor = Color(0xFF1D1B20)
  val emptyColor = Color(0x80FFFFFF) // white/50
  val goalColor = Color(0xFF34C759)
  val mouseColor = Color(0xFF6750A4)
  val pathColor = Color(0x40D0BCFF)
  val rewardColor = Color(0xFFFFD700)
  val blockadeColor = Color(0xFFB3261E)
  val mouseBorderColor = Color.White

  Canvas(modifier = modifier) {
    val rows = maze.size
    val cols = maze[0].size
    // Use spacing (grid gap)
    val gap = 4.dp.toPx()
    val totalHGap = gap * (cols - 1)
    val totalVGap = gap * (rows - 1)
    
    val cellWidth = (size.width - totalHGap) / cols
    val cellHeight = (size.height - totalVGap) / rows
    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())

    // Draw path
    for (p in pathTaken) {
        val left = p.x * (cellWidth + gap)
        val top = p.y * (cellHeight + gap)
        drawRoundRect(
            color = pathColor,
            topLeft = Offset(left, top),
            size = Size(cellWidth, cellHeight),
            cornerRadius = cornerRadius
        )
    }

    for (y in 0 until rows) {
      for (x in 0 until cols) {
        val type = maze[y][x]
        
        val color = when (type) {
            1 -> wallColor
            3 -> goalColor
            4 -> rewardColor
            5 -> blockadeColor
            else -> emptyColor
        }
        
        val left = x * (cellWidth + gap)
        val top = y * (cellHeight + gap)
        
        // Draw the cell base if it's not path color (we already drew path)
        if (type != 0 || !pathTaken.contains(Point(x, y))) {
            drawRoundRect(
              color = color,
              topLeft = Offset(left, top),
              size = Size(cellWidth, cellHeight),
              cornerRadius = cornerRadius
            )
        }

        if (type == 4) {
            drawCircle(
                color = Color.White,
                radius = minOf(cellWidth, cellHeight) / 4f,
                center = Offset(left + cellWidth / 2f, top + cellHeight / 2f)
            )
        }

        if (showPolicy && type != 1 && type != 3 && type != 5 && qTable.isNotEmpty()) {
            val stateIdx = y * cols + x
            if (stateIdx < qTable.size) {
                val actions = qTable[stateIdx]
                val maxQ = actions.maxOrNull() ?: 0f
                if (maxQ > 0f) {
                    val bestAction = actions.indexOfFirst { it == maxQ }
                    val cx = left + cellWidth / 2f
                    val cy = top + cellHeight / 2f
                    val arrowLen = minOf(cellWidth, cellHeight) / 3f
                    val dx = when(bestAction) { 1 -> arrowLen; 3 -> -arrowLen; else -> 0f }
                    val dy = when(bestAction) { 0 -> -arrowLen; 2 -> arrowLen; else -> 0f }
                    
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = Offset(cx, cy),
                        end = Offset(cx + dx, cy + dy),
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.5f),
                        radius = 3f,
                        center = Offset(cx + dx, cy + dy)
                    )
                }
            }
        }
      }
    }

    // Draw mouse
    val mLeft = mousePos.x * (cellWidth + gap)
    val mTop = mousePos.y * (cellHeight + gap)
    val mCenterX = mLeft + cellWidth / 2f
    val mCenterY = mTop + cellHeight / 2f
    val mRadius = minOf(cellWidth, cellHeight) / 2f * 1.1f // scaled up

    drawCircle(
      color = mouseColor,
      radius = mRadius,
      center = Offset(mCenterX, mCenterY),
      style = androidx.compose.ui.graphics.drawscope.Fill
    )
    drawCircle(
      color = mouseBorderColor,
      radius = mRadius,
      center = Offset(mCenterX, mCenterY),
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )
    
    // Draw mouse nose marker
    drawCircle(
      color = Color.White,
      radius = mRadius * 0.2f,
      center = Offset(mCenterX + mRadius * 0.4f, mCenterY),
      style = androidx.compose.ui.graphics.drawscope.Fill
    )
  }
}
