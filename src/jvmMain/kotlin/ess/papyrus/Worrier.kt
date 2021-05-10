/*
 * Copyright 2020 Mark.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ess.papyrus

import ess.ESS
import ess.WStringElement
import resaver.ResaverFormatting.makeHTMLList
import java.nio.file.FileSystems
import java.util.*
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 *
 * @author Mark
 */
class Worrier {
    private fun checkFatal(result: ESS.Result): CharSequence {
        val BUF = StringBuilder()
        val ESS = result.ESS
        val PAPYRUS = ESS.papyrus

        // Check the first fatal condition -- truncation.
        if (ESS.isTruncated) {
            BUF.append("<p><em>THIS FILE IS TRUNCATED.</em> It is corrupted and can never be recovered, not even by scrubbing it with baking soda and vinegar.")
            if (PAPYRUS!!.stringTable.isTruncated) {
                val missing = PAPYRUS.stringTable.missingCount
                BUF.append("<br/><strong>TRUNCATED STRING TABLE.</strong> ")
                    .append(missing)
                    .append(" strings missing. The cause of this is unknown, but sometimes involves the scripts that append to strings in a loop.")
            } else if (PAPYRUS.isTruncated) {
                BUF.append("<br/><strong>TRUNCATED PAPYRUS BLOCK.</strong> This is usually caused by too many scripts running at once, or recursive scripts without proper boundary conditions.")
            }
            var b = false
            for (i in ESS.formIDs) {
                if (i == 0) {
                    b = true
                    break
                }
            }
            if (b) {
                var present = 0
                while (present < ESS.formIDs.size && ESS.formIDs[present] != 0) {
                    present++
                }
                BUF.append("<br/><strong>TRUNCATED FORMID ARRAY</strong>. ")
                    .append(present).append('/').append(ESS.formIDs.size)
                    .append(" formIDs read. This is sometimes caused by updating mods without following their proper updating procedure.")
            }
            BUF.append("</p>")
            shouldWorry = true
            disableSaving = true
        }

        // Check the second fatal condition -- the string table bug.
        if (PAPYRUS!!.stringTable.hasSTB()) {
            BUF.append("<p><em>THIS FILE HAS THE STRING-TABLE-BUG.</em> It is corrupted and can never be recovered, not even with lasers or cheetah blood.</p>")
            shouldWorry = true
            disableSaving = true
        }
        return BUF
    }

    private fun checkPerformance(result: ESS.Result): CharSequence {
        val BUF = StringBuilder()
        val time = result.TIME_S
        val size = result.SIZE_MB
        BUF.append("<p>The savefile was successfully loaded.<ul>")
        BUF.append(String.format("<li>Read %1.1f mb in %1.1f seconds.</li>", size, time))
        if (result.ESS.hasCosave()) {
            BUF.append("<li>${result.GAME!!.COSAVE_EXT.toUpperCase()} co-save was loaded.</li>")
        } else {
            BUF.append("<li>No co-save was found.</li>")
        }
        BUF.append("</ul></p>")
        return BUF
    }

    private fun checkNonFatal(result: ESS.Result): CharSequence {
        val BUF = StringBuilder()
        val unattached = result.ESS.papyrus!!.countUnattachedInstances()
        if (unattached > 0) {
            val msg = String.format("<p>There are %d unattached instances.</p>", unattached)
            BUF.append(msg)
            shouldWorry = true
        }
        val undefined = result.ESS.papyrus.countUndefinedElements()
        if (undefined[0] > 0) {
            val msg = String.format("<p>There are %d undefined elements.</p>", undefined[0])
            BUF.append(msg)
            shouldWorry = true
        }
        if (undefined[1] > 0) {
            val msg = String.format("<p>There are %d undefined threads.</p>", undefined[1])
            BUF.append(msg)
            shouldWorry = true
        }
        val numStacks = result.ESS.papyrus.activeScripts.size
        val active: Stream<Script> = result.ESS.papyrus.activeScripts.values.parallelStream()
            .filter { obj: ActiveScript -> obj.hasStack() }
            .flatMap { `as`: ActiveScript -> `as`.stackFrames.stream() }
            .map(StackFrame::script)
        val suspended: Stream<Script?> = result.ESS.papyrus.suspendedStacks.values.parallelStream()
            .map(SuspendedStack::script)
            .filter { obj: Script? -> Objects.nonNull(obj) }
        val frameCounts: Map<Script, Long> = Stream.concat(active, suspended)
            .filter { obj: Script? -> Objects.nonNull(obj) }
            .collect(Collectors.groupingBy({ f: Script? -> f }, Collectors.counting()))
        val frames: List<Script> = ArrayList(frameCounts.keys)
        frames.sortedWith { a: Script, b: Script ->
            frameCounts[b]!!
                .compareTo(frameCounts[a]!!)
        }
        if (frames.isNotEmpty()) {
            val most = frames[0]
            var numFrames = 0L
            for (v in frameCounts.values) {
                numFrames += v
            }
            if (numStacks > 200 || numFrames > 1000) {
                BUF.append(
                    String.format(
                        "<p>There are %d stacks and %d frames, which probably indicates a problem.<br/>",
                        numStacks,
                        numFrames
                    )
                )
                BUF.append(
                    String.format(
                        "%s occurs the most often (%d occurrences)</p>",
                        most.toHTML(null),
                        frameCounts[most]
                    )
                )
                shouldWorry = true
            } else if (numStacks > 50 || numFrames > 150) {
                BUF.append(
                    String.format(
                        "<p>There are %d stacks and %d frames, which may indicate a problem.</p>",
                        numStacks,
                        numFrames
                    )
                )
                BUF.append(
                    String.format(
                        "%s occurs the most often (%d occurrences)</p>",
                        most.toHTML(null),
                        frameCounts[most]
                    )
                )
                shouldWorry = true
            }
            val deep: MutableList<ActiveScript> = mutableListOf()
            for (thread in result.ESS.papyrus.activeScripts.values) {
                if (thread.stackFrames.size >= 100) {
                    deep.add(thread)
                }
            }
            deep.sortWith { a1: ActiveScript, a2: ActiveScript -> a2.stackFrames.size.compareTo(a1.stackFrames.size) }
            if (deep.isNotEmpty()) {
                val deepest = deep[0]
                val depth = deepest.stackFrames.size
                val msg = String.format("<p>There is a stack %d frames deep (${deepest.toHTML(null)}).</p>", depth)
                BUF.append(msg)
                shouldWorry = true
            }
        }

        // Get a map of namespaces to scriptInstances in that namespace.
        val currentNamespaces: MutableMap<String, MutableList<ScriptInstance>> = mutableMapOf()
        for (instance in result.ESS.papyrus.scriptInstances.values) {
            if (instance.scriptName.toString().contains(":")) {
                val SplitValues = instance.scriptName.toString().split(":")
                currentNamespaces.getOrPut(SplitValues[0]) { ArrayList() }.add(instance)
            }
        }
        val currentCanaries: MutableMap<Script?, Int?> = mutableMapOf()
        for (instance in result.ESS.papyrus.scriptInstances.values) {
            if (instance.hasCanary()) {
                check(currentCanaries.put(instance.script, instance.canary) == null) { "Duplicate key" }
            }
        }
        if (previousESS != null) {
            val H1 = previousESS!!.header
            val H2 = result.ESS.header
            if (WStringElement.compare(H1.NAME, H2.NAME) == 0 && H1.FILETIME < H2.FILETIME) {
                val previousSize = previousESS!!.calculateSize()
                val currentSize = result.ESS.calculateSize()
                val difference = 200.0 * (currentSize - previousSize) / (currentSize + previousSize)
                if (difference < -5.0) {
                    val msg = String.format(
                        "<p>This savefile has %2.2f%% less papyrus data the previous one.</p>",
                        -difference
                    )
                    BUF.append(msg)
                    shouldWorry = true
                }
            }
            val missingNamespaces: MutableList<String> = mutableListOf()
            for (namespace in previousNamespaces!!.keys) {
                if (!currentNamespaces.containsKey(namespace)) {
                    for (scriptInstance in previousNamespaces!![namespace]!!) {
                        val refID = scriptInstance.refID
                        if (!refID.isZero) {
                            if (result.ESS.changeForms.containsKey(refID)) {
                                missingNamespaces.add(namespace)
                                break
                            }
                        }
                    }
                }
            }
            if (missingNamespaces.isNotEmpty()) {
                val msg = "This savefile has %d missing namespaces (the Canary error)."
                BUF.append(makeHTMLList(msg, missingNamespaces, LIMIT, Function { i: String? -> i }))
                shouldWorry = true
            }
            val canaryErrors: MutableList<Script?> = mutableListOf()
            for (script in previousCanaries!!.keys) {
                if (currentCanaries.containsKey(script)) {
                    if (previousCanaries!![script] != 0) {
                        if (currentCanaries[script] == 0) {
                            canaryErrors.add(script)
                        }
                    }
                }
            }
            if (canaryErrors.isNotEmpty()) {
                val msg = "This savefile has %d zeroed canaries."
                BUF.append(makeHTMLList(msg, canaryErrors, LIMIT, Function { i: Script? -> i?.toHTML(null) }))
                shouldWorry = true
            }
        }
        val memberless: MutableList<ScriptInstance> = ArrayList()
        for (scriptInstance in result.ESS.papyrus.scriptInstances.values) {
            if (scriptInstance.hasMemberlessError()) {
                memberless.add(scriptInstance)
            }
        }
        if (memberless.isNotEmpty()) {
            val msg = "This savefile has %d script instances whose data is missing."
            BUF.append(makeHTMLList(msg, memberless, LIMIT, Function { i: ScriptInstance -> i.script!!.toHTML(null) }))
            shouldWorry = true
        }
        val definitionErrors: MutableList<ScriptInstance> = mutableListOf()
        for (scriptInstance in result.ESS.papyrus.scriptInstances.values) {
            if (scriptInstance.hasDefinitionError()) {
                definitionErrors.add(scriptInstance)
            }
        }
        if (definitionErrors.isNotEmpty()) {
            val msg = "This savefile has %d script instances with mismatched member data."
            BUF.append(
                makeHTMLList(msg, definitionErrors, LIMIT, Function { i: ScriptInstance -> i.script!!.toHTML(null) }))
            shouldWorry = true
        }
        previousNamespaces = currentNamespaces
        previousCanaries = currentCanaries
        previousESS = result.ESS
        return BUF
    }

    fun check(result: ESS.Result) {
        message = null
        shouldWorry = false
        disableSaving = false
        val PERFORMANCE = checkPerformance(result)
        val FATAL = checkFatal(result)
        val NONFATAL = checkNonFatal(result)
        val BUF = StringBuilder()
        if (shouldDisableSaving()) {
            BUF.append("<h3>Serious problems were identified</h2><h3>Saving is disabled. Trust me, it's for your own good.</h3>")
            BUF.append(FATAL).append("<hr/>")
        }
        BUF.append(PERFORMANCE).append("<hr/>")
        if (shouldWorry()) {
            BUF.append(if (shouldDisableSaving()) "<h3>Additional problems were identified</h3>" else "<h3>Potential problems were identified</h3>")
            BUF.append(NONFATAL).append("<hr/>")
        }
        message = BUF.toString()
    }

    fun shouldDisableSaving(): Boolean {
        return disableSaving
    }

    fun shouldWorry(): Boolean {
        return shouldWorry
    }

    var message: String? = null
        private set
    private var shouldWorry = false
    private var disableSaving = false
    private var previousESS: ESS? = null
    private var previousCanaries: Map<Script?, Int?>? = null
    private var previousNamespaces: Map<String, MutableList<ScriptInstance>>? =
        null

    companion object {
        private val LOG = Logger.getLogger(Worrier::class.java.canonicalName)
        private val MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{fos,ess}")
        private const val LIMIT = 12
    }
}