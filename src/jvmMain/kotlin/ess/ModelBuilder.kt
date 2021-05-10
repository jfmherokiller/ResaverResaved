/*
 * Copyright 2018 Mark.
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
package ess

import resaver.IString
import resaver.ProgressModel
import ess.papyrus.*
import resaver.gui.FilterTreeModel.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

/**
 *
 * @author Mark
 */
/**
 * @param progress
 */
class ModelBuilder(progress: ProgressModel) {
    /**
     * Add a `PluginInfo` to the model.
     *
     * @param plugins The `PluginInfo`.
     */
    fun addPluginInfo(plugins: PluginInfo) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Plugins (full)")
            NODE.addAll(plugins.fullPlugins.map { element: Plugin? -> element?.let { PluginNode(it) } }.toList())
            PROGRESS.modifyValue(1)
            NODE
        }))
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Plugins (lite)")
            NODE.addAll(plugins.litePlugins.map { element: Plugin? -> element?.let { PluginNode(it) } }.toList())
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `GlobalVariableTable` to the model.
     *
     * @param gvt The `GlobalVariableTable`.
     */
    fun addGlobalVariableTable(gvt: GlobalVariableTable) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Global Variables", gvt.variables).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `StringTable` to the model.
     *
     * @param table The `StringTable`.
     */
    fun addStringTable(table: StringTable) {
        TASKS.add(EXECUTOR.submit(Callable {
            val DICTIONARY = table.stream()
                .collect(Collectors.groupingBy(ALPHABETICAL))
            val NODES: List<Node> = DICTIONARY.entries
                .map { (key, value) -> ContainerNode(key.toString(), value).sort() }
                .toList()
            val NODE = ContainerNode("Strings")
            NODE.addAll(NODES).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `ScriptMap` to the model.
     *
     * @param script The `ScriptMap`.
     */
    fun addScripts(script: ScriptMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Script Definitions", script.values).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `StructMap` to the model.
     *
     * @param structs The `StructMap`.
     */
    fun addStructs(structs: StructMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Struct Definitions", structs.values).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `ReferenceMap` to the model.
     *
     * @param references The `ReferenceMap`.
     */
    fun addReferences(references: ReferenceMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("References", references.values).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add an `ArrayMap` to the model.
     *
     * @param arrays The `ArrayMap`.
     */
    fun addArrays(arrays: ArrayMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Arrays", arrays.values).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `UnbindMap` to the model.
     *
     * @param unbinds The `UnbindMap`.
     */
    fun addUnbinds(unbinds: UnbindMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("QueuedUnbinds", unbinds.values).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * @param unknownIDs The `EID` list.
     */
    fun addUnknownIDList(unknownIDs: List<EID?>?) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = unknownIDs?.let { ContainerNode("Unknown ID List", it).sort() }
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `GlobalVariableTable` to the model.
     *
     * @param animations The `GlobalVariableTable`.
     */
    fun addAnimations(animations: AnimObjects) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Global Variables", animations.animations)
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `ScriptInstanceMap` to the model.
     *
     * @param instances The `ScriptInstanceMap`.
     */
    fun addScriptInstances(instances: ScriptInstanceMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val DICTIONARY = instances.values.stream()
                .collect(Collectors.groupingBy(ALPHABETICAL))
            val NODES: List<Node> = DICTIONARY.entries
                .map { (key, value) -> ContainerNode(key.toString(), value).sort() }.toList()
            val NODE = ContainerNode("Script Instances").addAll(NODES).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `StructInstanceMap` to the model.
     *
     * @param instances The `StructInstanceMap`.
     */
    fun addStructInstances(instances: StructInstanceMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Struct Instances", instances.values).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a `ActiveScriptMap` to the model.
     *
     * @param threads The `ActiveScriptMap`.
     */
    fun addThreads(threads: ActiveScriptMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Active Scripts").addAll(
                threads.values.map { element: ActiveScript? -> element?.let { ActiveScriptNode(it) } }.toList()
            ).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a list of `FunctionMessage` to the model.
     *
     * @param messages The list of `FunctionMessage`.
     */
    fun addFunctionMessages(messages: List<FunctionMessage?>) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Function Messages").addAll(
                messages.map { element: FunctionMessage? -> element?.let { FunctionMessageNode(it) } }.toList()
            ).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a list of `SuspendedStack` to the model.
     *
     * @param stacks The list of `SuspendedStack`.
     */
    fun addSuspendedStacks1(stacks: SuspendedStackMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Suspended Stacks 1").addAll(
                stacks.values.map { element: SuspendedStack? -> element?.let { SuspendedStackNode(it) } }
                    .toList()).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add a list of `SuspendedStack` to the model.
     *
     * @param stacks The list of `SuspendedStack`.
     */
    fun addSuspendedStacks2(stacks: SuspendedStackMap) {
        TASKS.add(EXECUTOR.submit(Callable {
            val NODE = ContainerNode("Suspended Stacks 2").addAll(
                stacks.values.map { element: SuspendedStack? -> element?.let { SuspendedStackNode(it) } }
                    .toList()).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     *
     * @param changeForms
     */
    fun addChangeForms(changeForms: Map<RefID?, ChangeForm?>) {
        TASKS.add(EXECUTOR.submit(Callable {
            val DICTIONARY: Map<ChangeFormType?, List<ChangeForm>> =
                changeForms.values.toList().filterNotNull().groupBy(ChangeForm::type)
            val NODES: List<Node> = DICTIONARY.entries
                .map { (key, value) -> ContainerNode(key.toString(), value).sort() }
                .toList()
            val NODE = ContainerNode("ChangeForms").addAll(NODES).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     * Add `OtherData` to the model.
     *
     * @param data The `OtherData`.
     */
    fun addOtherData(data: OtherData?) {
        TASKS.add(EXECUTOR.submit(Callable {
            val OTHERDATA_NODES: MutableList<Node> = mutableListOf()
            data?.values?.forEach { (key: IString?, `val`: Any?) ->
                if (`val` is Array<*>) {
                    val convertedArray = `val`.map {i:Any? -> i as Element?}
                    OTHERDATA_NODES.add(ContainerNode(key, convertedArray))
                } else if (`val` is List<*>) {
                    val convertedArray = `val`.map {i:Any? -> i as Element?}
                    OTHERDATA_NODES.add(ContainerNode(key, convertedArray))
                }
            }
            val NODE = ContainerNode("Mystery Arrays").addAll(OTHERDATA_NODES).sort()
            PROGRESS.modifyValue(1)
            NODE
        }))
    }

    /**
     *
     * @param ess
     * @return
     */
    fun finish(ess: ESS?): resaver.gui.FilterTreeModel? {
        try {
            EXECUTOR.shutdown()
            EXECUTOR.awaitTermination(2, TimeUnit.MINUTES)
        } catch (ex: InterruptedException) {
            LOG.log(Level.SEVERE, "Model building was interrupted.", ex)
            return null
        }
        if (!TASKS.all { obj: Future<Node> -> obj.isDone }) {
            LOG.severe("Some tasks didn't finish.")
            return null
        }

        // Populate the root elementNode.
        val ROOT_NODES = ArrayList<Node>(15)
        TASKS.forEach(Consumer { task: Future<Node> ->
            try {
                val NODE = task.get()
                ROOT_NODES.add(NODE)
                PROGRESS.modifyValue(1)
            } catch (ex: InterruptedException) {
                throw IllegalStateException("ModelBuilding failed.", ex)
            } catch (ex: ExecutionException) {
                throw IllegalStateException("ModelBuilding failed.", ex)
            }
        })
        val ROOT: Node = ess?.let { RootNode(it, ROOT_NODES) }!!
        MODEL.root = ROOT
        return MODEL
    }

    private val MODEL: resaver.gui.FilterTreeModel
    private val EXECUTOR: ExecutorService
    private val TASKS: MutableList<Future<Node>>
    private val PROGRESS: ProgressModel

    companion object {
        fun createModel(ess: ESS, progress: ProgressModel): resaver.gui.FilterTreeModel? {
            val MB = ModelBuilder(progress)
            val papyrus = ess.papyrus
            MB.addPluginInfo(ess.pluginInfo)
            ess.globals.let { MB.addGlobalVariableTable(it) }
            MB.addChangeForms(ess.changeForms)
            if (papyrus != null) {
                MB.addStringTable(papyrus.stringTable)
            }
            if (papyrus != null) {
                MB.addScripts(papyrus.scripts)
            }
            if (ess.isFO4) {
                if (papyrus != null) {
                    MB.addStructs(papyrus.structs)
                }
            }
            if (papyrus != null) {
                MB.addScriptInstances(papyrus.scriptInstances)
            }
            if (ess.isFO4) {
                if (papyrus != null) {
                    MB.addStructInstances(papyrus.structInstances)
                }
            }
            if (papyrus != null) {
                MB.addReferences(papyrus.references)
            }
            if (papyrus != null) {
                MB.addArrays(papyrus.arrays)
            }
            if (papyrus != null) {
                MB.addThreads(papyrus.activeScripts)
            }
            if (papyrus != null) {
                MB.addFunctionMessages(papyrus.functionMessages)
            }
            if (papyrus != null) {
                MB.addSuspendedStacks1(papyrus.suspendedStacks1)
            }
            if (papyrus != null) {
                MB.addSuspendedStacks2(papyrus.suspendedStacks2)
            }
            if (papyrus != null) {
                MB.addUnknownIDList(papyrus.unknownIDList)
            }
            if (papyrus != null) {
                MB.addUnbinds(papyrus.unbinds)
            }
            MB.addAnimations(ess.animations)
            return MB.finish(ess)
        }

        /**
         * Maps a `TString` to a character.
         */
        val ALPHABETICAL = Function { v: Element? ->
            val str = v.toString()
            val firstChar = if (str.isEmpty()) '0' else Character.toUpperCase(str[0])
            val category = if (Character.isLetter(firstChar)) firstChar else '0'
            category
        }
        private val LOG = Logger.getLogger(ModelBuilder::class.java.canonicalName)
    }


    init {
        progress.maximum = 36
        MODEL = resaver.gui.FilterTreeModel()
        EXECUTOR = Executors.newFixedThreadPool(2)
        TASKS = mutableListOf()
        PROGRESS = progress
    }
}