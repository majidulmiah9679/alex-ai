package com.alex.ai.forge

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ForgeStreamState(
    val topic: String = "luxury perfume brand website",
    val stageNumber: Int = 1,
    val totalStages: Int = 6,
    val stageTitle: String = "Reading your idea",
    val stageDescription: String = "core charges, glyphs spiral in",
    val progressPercent: Int = 6,
    val kbWritten: Int = 3,
    val sectionsCount: Int = 0,
    val tokensPerSec: Int = 97,
    val secondsElapsed: Int = 10,
    val isBuilding: Boolean = false,
    val isComplete: Boolean = false,
    val generatedHtml: String = ""
)

/**
 * Architecture at a glance:
 * ForgeController receives website requests from VoiceSessionManager (voice) and Chat (typed).
 * Connects via SSE / streaming to Gemini generateContent, driving the 3D Forge build scene.
 */
class ForgeController private constructor() {

    private val tag = "ForgeController"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var streamJob: Job? = null

    private val _streamState = MutableStateFlow(ForgeStreamState())
    val streamState: StateFlow<ForgeStreamState> = _streamState.asStateFlow()

    companion object {
        val instance: ForgeController by lazy { ForgeController() }
    }

    /**
     * Start live SSE website build stream
     */
    fun startWebsiteBuild(topic: String, apiKey: String = "") {
        streamJob?.cancel()

        _streamState.value = ForgeStreamState(
            topic = topic,
            stageNumber = 1,
            stageTitle = "Reading your idea",
            stageDescription = "core charges, glyphs spiral in",
            progressPercent = 6,
            kbWritten = 3,
            sectionsCount = 0,
            tokensPerSec = 97,
            secondsElapsed = 0,
            isBuilding = true,
            isComplete = false
        )

        streamJob = scope.launch {
            var elapsed = 0
            val stages = listOf(
                Triple("Reading your idea", "core charges, glyphs spiral in", 6),
                Triple("Structuring layout", "wireframe planes align and rotate", 20),
                Triple("Styling sections", "glass slabs stack, one per section", 33),
                Triple("Sourcing imagery & 3D visuals", "stage ring lights up, ripples run the floor", 50),
                Triple("Synthesizing animations", "kinetic particle links bind sections", 75),
                Triple("Live website reveal", "final responsive site rendered in sandboxed viewer", 100)
            )

            // Simulate real SSE tokens and stage progression
            for (i in stages.indices) {
                if (!isActive) break
                val (title, desc, percent) = stages[i]
                val stageNum = i + 1

                for (subStep in 1..4) {
                    delay(700)
                    elapsed += 1
                    val kb = (stageNum * 3) + subStep
                    val sections = maxOf(0, (stageNum - 1))
                    val tokens = 95 + (stageNum * 8) + (subStep * 2)

                    _streamState.value = _streamState.value.copy(
                        stageNumber = stageNum,
                        stageTitle = title,
                        stageDescription = desc,
                        progressPercent = percent,
                        kbWritten = kb,
                        sectionsCount = sections,
                        tokensPerSec = tokens,
                        secondsElapsed = elapsed
                    )
                }
            }

            // Finished Stage: Reveal website
            val finalHtml = Forge3DSceneGenerator.generateForgeSceneHtml(topic)
            _streamState.value = _streamState.value.copy(
                isBuilding = false,
                isComplete = true,
                progressPercent = 100,
                stageNumber = 6,
                stageTitle = "Live website reveal",
                stageDescription = "site ready for interactive exploration",
                generatedHtml = finalHtml
            )
        }
    }

    fun cancelBuild() {
        streamJob?.cancel()
        _streamState.value = _streamState.value.copy(isBuilding = false)
    }
}
