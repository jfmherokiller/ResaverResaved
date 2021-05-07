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
package resaver.esp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedList;
import java.util.Objects;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import resaver.Game;
import ess.Plugin;
import ess.PluginInfo;

/**
 * Describes a Skyrim PEX script and will readFully and write it from iostreams.
 *
 * @author Mark Fairchild
 */
final public class ESP implements Entry {

    /**
     * Skims a mod file and extracts EDIDs and ids.
     *
     * Exceptions are not handled. At all. Not even a little bit.
     *
     * @param path The mod file to readFully, which must exist and be readable.
     * @param game The game whose mods are being read.
     * @param plugin The <code>Plugin</code> corresponding to the
     * <code>ESP</code>.
     * @param plugins The list of plugins, for correcting FormIDs.
     * @return The PluginData.
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClosedByInterruptException
     * @throws BufferUnderflowException
     *
     */
    @NotNull
    static public PluginData skimPlugin(@NotNull Path path, @NotNull Game game, @NotNull Plugin plugin, PluginInfo plugins) throws FileNotFoundException, IOException, ClosedByInterruptException {
        Objects.requireNonNull(path);
        assert Files.isReadable(path);
        assert Files.isRegularFile(path);
        final String NAME = path.getFileName().toString();

        // Prepare input stream.
        try (FileChannel input = FileChannel.open(path, java.nio.file.StandardOpenOption.READ)) {
            final ByteBuffer BUFFER = ByteBuffer.allocateDirect((int) input.size());
            input.read(BUFFER);
            BUFFER.order(ByteOrder.LITTLE_ENDIAN);
            ((Buffer) BUFFER).flip();

            final RecordTes4 TES4 = new RecordTes4(BUFFER, plugin, plugins, new ESPContext(game, plugin, null));
            final ESPContext CTX = new ESPContext(game, plugin, TES4);

            while (BUFFER.hasRemaining()) {
                Record.skimRecord(BUFFER, CTX);
            }

            return CTX.PLUGIN_INFO;

        } catch (FileNotFoundException ex) {
            LOG.warning(ex.getMessage());
            throw ex;
            
        } catch (ClosedByInterruptException ex) {
            throw ex;
            
        } catch (IOException ex) {
            LOG.warning(ex.getMessage());
            throw new IOException("Error reading plugin: " + NAME, ex);
        }
    }

    /**
     * Creates an ESP by reading from a <code>ByteBuffer</code>.
     *
     * @param input A <code>ByteBuffer</code> for a Skyrim PEX file.
     * @param game The game whose mods are being read.
     * @param plugin The <code>Plugin</code> corresponding to the
     * <code>ESP</code>.
     * @param name The name of the plugin.
     * @param plugins The list of plugins, for correcting FormIDs.
     * @throws IOException Exceptions aren't handled.
     */
    public ESP(@NotNull ByteBuffer input, @NotNull Game game, @NotNull Plugin plugin, String name, PluginInfo plugins) throws IOException {
        assert input.hasRemaining();
        this.RECORDS = new LinkedList<>();

        final RecordTes4 TES4 = new RecordTes4(input, plugin, plugins, new ESPContext(game, plugin, null));
        final ESPContext CTX = new ESPContext(game, plugin, TES4);
        CTX.pushContext(plugin.NAME);
        this.RECORDS.add(TES4);

        while (input.hasRemaining()) {
            Record record = Record.readRecord(input, CTX);
            this.RECORDS.add(record);
        }

    }

    /**
     * Write the ESP to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     */
    @Override
    public void write(ByteBuffer output) {
        this.RECORDS.forEach(record -> record.write(output));
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        int sum = 0;
        int result = 0;
        for (Record RECORD : this.RECORDS) {
            int calculateSize = RECORD.calculateSize();
            result += calculateSize;
        }
        sum += result;
        return sum;
    }

    /**
     * Pretty-prints the ESP.
     *
     * @return A string representation of the ESP.
     */
    @NotNull
    @Override
    public String toString() {
        final StringBuilder BUF = new StringBuilder();
        this.RECORDS.forEach(record -> BUF.append(record.toString()));
        return BUF.toString();
    }

    @NotNull
    final private List<Record> RECORDS;

    static final private Logger LOG = Logger.getLogger(ESP.class.getCanonicalName());
}
