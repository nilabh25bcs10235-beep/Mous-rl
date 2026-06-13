package com.example

import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random

data class Point(val x: Int, val y: Int)

class QLearningAgent(val width: Int, val height: Int) {
    // 4 actions: 0=Up, 1=Right, 2=Down, 3=Left
    val numStates = width * height
    val qTable = Array(numStates) { FloatArray(4) { 0f } }

    var alpha = 0.1f
    var gamma = 0.95f
    var epsilon = 0.5f // Start more explorative
    val minEpsilon = 0.05f
    val epsilonDecay = 0.99f

    fun getState(p: Point): Int = p.y * width + p.x

    fun chooseAction(state: Int): Int {
        if (Random.nextFloat() < epsilon) {
            return Random.nextInt(4)
        }
        val actions = qTable[state]
        val maxQ = actions.maxOrNull() ?: 0f
        val bestActions = actions.indices.filter { actions[it] == maxQ }
        return bestActions.random() // break ties randomly
    }

    fun updateQValue(state: Int, action: Int, reward: Float, nextState: Int) {
        val nextMaxQ = qTable[nextState].maxOrNull() ?: 0f
        qTable[state][action] = qTable[state][action] + alpha * (reward + gamma * nextMaxQ - qTable[state][action])
    }

    fun decayEpsilon() {
        if (epsilon > minEpsilon) {
            epsilon *= epsilonDecay
        }
    }
}

object MazeGenerator {
    fun generate(width: Int, height: Int): Array<IntArray> {
        val maze = Array(height) { IntArray(width) { 1 } }
        val stack = mutableListOf<Point>()
        var current = Point(1, 1)
        maze[1][1] = 0
        stack.add(current)
        
        val dirs = listOf(Point(0,-2), Point(2,0), Point(0,2), Point(-2,0))
        
        while(stack.isNotEmpty()) {
            current = stack.last()
            val unvisited = dirs.map { Point(current.x + it.x, current.y + it.y) }
                                .filter { it.x in 1 until width-1 && it.y in 1 until height-1 && maze[it.y][it.x] == 1 }
            
            if (unvisited.isNotEmpty()) {
                val next = unvisited.random()
                maze[current.y + (next.y - current.y)/2][current.x + (next.x - current.x)/2] = 0
                maze[next.y][next.x] = 0
                stack.add(next)
            } else {
                stack.removeLast()
            }
        }
        
        maze[1][1] = 2
        maze[height-2][width-2] = 3
        
        // Randomly open some walls to create alternative routes
        for(i in 0 until (width*height)/15) {
            val x = (1 until width-1).random()
            val y = (1 until height-1).random()
            if (maze[y][x] == 1) {
                maze[y][x] = 0 // Prevent fully enclosed areas sometimes, opens pathways
            }
        }

        // Scatter rewards (4) and temporary blockades (5)
        for (i in 0 until (width*height)/20) {
            val x = (1 until width-1).random()
            val y = (1 until height-1).random()
            if (maze[y][x] == 0) maze[y][x] = 4
        }
        for (i in 0 until (width*height)/30) {
            val x = (1 until width-1).random()
            val y = (1 until height-1).random()
            if (maze[y][x] == 0) maze[y][x] = 5
        }
        
        return maze
    }
}

val defaultMazeGrid = arrayOf(
    intArrayOf(2, 0, 0, 1, 0, 0),
    intArrayOf(1, 1, 0, 1, 0, 1),
    intArrayOf(0, 0, 0, 0, 0, 0),
    intArrayOf(0, 1, 1, 1, 1, 0),
    intArrayOf(0, 0, 0, 0, 1, 0),
    intArrayOf(1, 1, 1, 0, 0, 3)
)
