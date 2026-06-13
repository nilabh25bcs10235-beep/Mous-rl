package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SimulationState(
    val maze: Array<IntArray> = defaultMazeGrid,
    val mousePosition: Point = Point(0, 0),
    val startPosition: Point = Point(0, 0),
    val exitPosition: Point = Point(5, 5),
    val episode: Int = 1,
    val steps: Int = 0,
    val totalReward: Float = 0f,
    val isRunning: Boolean = false,
    val aiThoughts: String = "",
    val isThinking: Boolean = false,
    val pathTaken: List<Point> = emptyList(),
    val showPolicy: Boolean = false,
    val qTableCopy: Array<FloatArray> = emptyArray()
)

class SimulationViewModel : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private var agent: QLearningAgent
    private var startP = Point(0, 0)
    private var exitP = Point(5, 5)
    
    private var originalMaze: Array<IntArray> = defaultMazeGrid

    private var simulationJob: Job? = null
    var simulationSpeedMs: Long = 50L

    init {
        agent = QLearningAgent(defaultMazeGrid[0].size, defaultMazeGrid.size)
        // Ensure default maze is properly initialized if no generation is done
        initMaze(defaultMazeGrid)
    }

    fun initMaze(newMaze: Array<IntArray>) {
        val h = newMaze.size
        val w = newMaze[0].size
        agent = QLearningAgent(w, h)
        
        originalMaze = Array(h) { y -> IntArray(w) { x -> newMaze[y][x] } }

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (newMaze[y][x] == 2) startP = Point(x, y)
                if (newMaze[y][x] == 3) exitP = Point(x, y)
            }
        }
        _state.value = _state.value.copy(
            maze = originalMaze.map { it.clone() }.toTypedArray(),
            startPosition = startP,
            exitPosition = exitP,
            mousePosition = startP,
            episode = 1,
            steps = 0,
            totalReward = 0f,
            isRunning = false,
            pathTaken = listOf(startP),
            qTableCopy = agent.qTable.map { it.clone() }.toTypedArray()
        )
        simulationJob?.cancel()
    }

    fun generateNewMaze(size: Int) {
        val newMaze = MazeGenerator.generate(size, size)
        initMaze(newMaze)
    }

    fun togglePolicy() {
        _state.value = _state.value.copy(showPolicy = !_state.value.showPolicy)
    }

    fun toggleSimulation() {
        if (_state.value.isRunning) {
            simulationJob?.cancel()
            _state.value = _state.value.copy(isRunning = false)
        } else {
            _state.value = _state.value.copy(isRunning = true)
            simulationJob = viewModelScope.launch(Dispatchers.Default) {
                while (isActive) {
                    stepSimulation()
                    delay(simulationSpeedMs)
                }
            }
        }
    }

    private suspend fun stepSimulation() {
        val currentState = agent.getState(_state.value.mousePosition)
        val action = agent.chooseAction(currentState)
        
        var nextP = _state.value.mousePosition
        val (dx, dy) = when (action) {
            0 -> Pair(0, -1) // Up
            1 -> Pair(1, 0)  // Right
            2 -> Pair(0, 1)  // Down
            3 -> Pair(-1, 0) // Left
            else -> Pair(0, 0)
        }
        
        val targetX = nextP.x + dx
        val targetY = nextP.y + dy
        
        var reward = 0f
        var hitExit = false
        val currentMaze = _state.value.maze
        
        if (targetX < 0 || targetX >= agent.width || targetY < 0 || targetY >= agent.height) {
            reward = -10f
        } else {
            val cell = currentMaze[targetY][targetX]
            if (cell == 1) { // Wall
                reward = -10f
            } else if (cell == 5) { // Temporary Blockade
                reward = -15f
            } else if (cell == 3) { // Exit
                reward = 100f
                hitExit = true
                nextP = Point(targetX, targetY)
            } else if (cell == 4) { // Reward Point
                reward = 15f
                nextP = Point(targetX, targetY)
                currentMaze[targetY][targetX] = 0 // Consume it
            } else { // Empty space or start
                reward = -1f
                nextP = Point(targetX, targetY)
            }
        }
        
        val nextState = agent.getState(nextP)
        agent.updateQValue(currentState, action, reward, nextState)
        
        withContext(Dispatchers.Main) {
            val s = _state.value
            val newPath = s.pathTaken + nextP
            val newQTable = if (s.steps % 5 == 0) agent.qTable.map { it.clone() }.toTypedArray() else s.qTableCopy
            
            if (hitExit) {
                agent.decayEpsilon()
                val resetMaze = originalMaze.map { it.clone() }.toTypedArray()
                
                _state.value = s.copy(
                    maze = resetMaze,
                    mousePosition = startP,
                    episode = s.episode + 1,
                    steps = 0,
                    totalReward = 0f,
                    pathTaken = listOf(startP),
                    qTableCopy = agent.qTable.map { it.clone() }.toTypedArray()
                )
                delay(200)
            } else {
                _state.value = s.copy(
                    maze = currentMaze,
                    mousePosition = nextP,
                    steps = s.steps + 1,
                    totalReward = s.totalReward + reward,
                    pathTaken = newPath,
                    qTableCopy = newQTable
                )
            }
        }
    }

    fun askAiAboutLearning() {
        _state.value = _state.value.copy(isThinking = true, aiThoughts = "")
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                The Q-learning mouse is currently on episode ${"$"}{_state.value.episode}.
                It has taken ${"$"}{_state.value.steps} steps in this episode with a total reward of ${"$"}{_state.value.totalReward}.
                Epsilon (exploration rate) is currently ${"$"}{agent.epsilon}.
                Given it started with 0 pretrained data, explain how Q-learning is helping it find the exit, step-by-step.
                Use 'ThinkingLevel.HIGH' logic to provide a deep insight into how its understanding of the maze is evolving, considering it can encounter walls, blockades (high penalty), and extra rewards (bonus).
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "high")
                )
            )

            try {
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "The AI didn't provide any thoughts."
                
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isThinking = false, aiThoughts = text)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isThinking = false, aiThoughts = "Error generating response: ${"$"}{e.localizedMessage}\nMake sure API key is configured in settings.")
                }
            }
        }
    }
}
