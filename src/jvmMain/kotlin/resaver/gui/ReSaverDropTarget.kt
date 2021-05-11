/*
 * Copyright 2019 Mark.
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
package resaver.gui

import GenericConsumer
import mu.KotlinLogging
import java.awt.dnd.DropTarget
import kotlin.jvm.Synchronized
import java.awt.dnd.DropTargetDropEvent
import java.util.Objects
import java.awt.dnd.DnDConstants
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.nio.file.Path

/**
 * Handles file-drop events.
 *
 * @author Mark Fairchild
 */
private val logger = KotlinLogging.logger {}
class ReSaverDropTarget(handler: GenericConsumer<Path>) : DropTarget() {
    /**
     *
     * @param event
     */
    @Synchronized
    override fun drop(event: DropTargetDropEvent) {
        try {
            Objects.requireNonNull(event, "The event must not be null.")
            event.acceptDrop(DnDConstants.ACTION_COPY)
            val TRANSFER = event.transferable
            Objects.requireNonNull(TRANSFER, "The DnD transferable must not be null.")
            val DATA = TRANSFER.getTransferData(DataFlavor.javaFileListFlavor)
            Objects.requireNonNull(DATA, "The DnD data block must not be null.")
            if (DATA is List<*>) {
                val FILES = DATA
                FILES.stream()
                    .map { obj: Any? -> (obj as File).toPath() }
                    .findFirst()
                    .ifPresent(HANDLER)
            }
        } catch (ex: UnsupportedFlavorException) {
            logger.warn{"Drop and drop problem: ${ex.message}"}
        } catch (ex: IOException) {
            logger.warn{"Drop and drop problem: ${ex.message}"}
        }
    }

    private val HANDLER: GenericConsumer<Path> = handler

    companion object {
    }

}