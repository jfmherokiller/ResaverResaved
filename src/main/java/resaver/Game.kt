/*
 * Copyright 2016 Mark Fairchild.
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
package resaver

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import javax.swing.filechooser.FileNameExtensionFilter

/**
 *
 * @author Mark Fairchild
 */
enum class Game(
    /**
     * The name of the supported game.
     */
    val NAME: String, saveName: String,
    /**
     * The file extension for the game's savefiles.
     */
    val SAVE_EXT: String,
    /**
     * The file extension for the game's cosaves.
     */
    val COSAVE_EXT: String,
    /**
     * The name of the game directory.
     */
    val GAME_DIRECTORY: Path,
    /**
     * A `Path` to the default location for savefiles.
     */
    val SAVE_DIRECTORY: Path,
    /**
     * The name of the game executable.
     */
    val EXECUTABLE: Path, vararg patchNames: String
) {
    SKYRIM_LE(
        "Skyrim Legendary Edition",
        "Skyrim Savefile",
        "ess",
        "skse",
        Path.of("skyrim"),
        Path.of("Skyrim/Saves"),
        Path.of("tesv.exe"),
        "Unofficial Skyrim Legendary Edition Patch.esp"
    ),
    SKYRIM_SE(
        "Skyrim Special Edition",
        "Skyrim SE Savefile",
        "ess",
        "skse",
        Path.of("skyrim special edition"),
        Path.of("Skyrim Special Edition/Saves"),
        Path.of("skyrimse.exe"),
        "Unofficial Skyrim Special Edition Patch.esp"
    ),
    SKYRIM_SW(
        "Skyrim Switch Edition",
        "Skyrim Switch Savefile",
        "sav0",
        "skse",
        Path.of("skyrim switch edition"),
        Path.of("Skyrim SW/Saves"),
        Path.of("SkyrimSE.exe")
    ),
    SKYRIM_VR(
        "Skyrim VR Edition",
        "Skyrim VR Savefile",
        "ess",
        "skse",
        Path.of("Elderscroll SkyrimVR"),
        Path.of("Skyrim VR/Saves"),
        Path.of("SkyrimVR.exe")
    ),
    FALLOUT4(
        "Fallout 4",
        "Fallout 4 Savefile",
        "fos",
        "f4se",
        Path.of("fallout 4"),
        Path.of("fallout4/Saves"),
        Path.of("fallout4.exe"),
        "Unofficial Fallout 4 Patch.esp"
    ),
    FALLOUT_VR(
        "Fallout 4 VR",
        "Fallout 4 VR Savefile",
        "ess",
        "skse",
        Path.of("Fallout 4 VR"),
        Path.of("Fallout4VR/Saves"),
        Path.of("fallout4vr.exe")
    );

    /**
     * An FX_FILTER, for dialog boxes that choose a savefile.
     */
    val FILTER: FileNameExtensionFilter

    /**
     * Names of unofficial patches.
     */
    val PATCH_NAMES: List<String>

    /**
     * A `PathMatcher` that matches savefile names.
     */
    private val SAVE_MATCHER: PathMatcher

    /**
     * Test if a savefile matches.
     *
     * @param path
     * @return
     */
    fun testFilename(path: Path): Boolean {
        return SAVE_MATCHER.matches(path.fileName)
    }

    /**
     * @return Flag indicating whether the game has a 64bit IDs.
     */
    val isID64: Boolean
        get() = !isSLE

    /**
     * @return Flag indicating whether the game is Fallout 4.
     */
    val isFO4: Boolean
        get() = when (this) {
            FALLOUT4, FALLOUT_VR -> true
            else -> false
        }

    /**
     * @return Flag indicating whether the game is an edition of Skyrim.
     */
    val isSkyrim: Boolean
        get() = when (this) {
            SKYRIM_LE, SKYRIM_SW, SKYRIM_SE, SKYRIM_VR -> true
            else -> false
        }

    /**
     * @return Flag indicating whether the game is Skyrim Legendary Edition.
     */
    val isSLE: Boolean
        get() = this == SKYRIM_LE

    companion object {
        /**
         * A filename filter for all of the games.
         */
        val FILTER_ALL = FileNameExtensionFilter("Bethesda Savefiles", "ess", "fos", "sav0")

        /**
         * Cached list version of the values.
         */
        var VALUES: MutableList<Game> = mutableListOf(*values())
    }

    /**
     * Creates a new `Game` for the specified extension.
     *
     * @param gameName The game's name.
     * @param saveName The name for savefiles.
     * @param saveExt The file extension for savefiles.
     * @param cosaveExt The file extension for co-saves.
     * @param gameDir The default name of the game's directory.
     * @param exe The filename of the game's executable.
     */
    init {
        FILTER = FileNameExtensionFilter(saveName, SAVE_EXT)
        PATCH_NAMES = mutableListOf(*patchNames)
        SAVE_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.$SAVE_EXT")
    }
}