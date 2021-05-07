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

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


import mf.BufferUtil;
import resaver.IString;
import ess.Plugin;
import static resaver.esp.Entry.advancingSlice;
import ess.PluginInfo;

/**
 * RecordTes4 is the first record. It handles its own data and is not read using
 * Record.readRecord().
 *
 * @author Mark Fairchild
 */
public class RecordTes4 extends Record {

    /**
     * Creates a new RecordTes4 by reading it from a LittleEndianInput.
     *
     * @param input The <code>ByteBuffer</code> to read.
     * @param plugin The <code>Plugin</code> corresponding to the
     * <code>ESP</code>.
     * @param plugins The list of plugins, for correcting FormIDs.
     * @param ctx The mod descriptor.
     */
    public RecordTes4(ByteBuffer input, Plugin plugin, PluginInfo plugins, ESPContext ctx) {
        this.CODE = RecordCode.TES4;
        this.PLUGIN = Objects.requireNonNull(plugin);
        this.PLUGINS = Objects.requireNonNull(plugins);

        final byte[] CODEBYTES = new byte[4];
        input.get(CODEBYTES);
        final String CODESTRING = new String(CODEBYTES);
        assert CODESTRING.equals("TES4");

        ctx.pushContext(CODESTRING);

        final int DATASIZE = input.getInt();
        this.HEADER = new Record.Header(input, ctx);

        // Read the record data.
        final ByteBuffer FIELDINPUT = advancingSlice(input, DATASIZE);
        this.FIELDS = new FieldList();

        while (FIELDINPUT.hasRemaining()) {
            FieldList newFields = Record.readField(RecordCode.TES4, FIELDINPUT, ctx);
            this.FIELDS.addAll(newFields);
        }

        List<String> masters = new ArrayList<>();
        for (Field FIELD : this.FIELDS) {
            if (FIELD.getCode().equals(IString.get("MAST"))) {
                if (FIELD instanceof FieldSimple) {
                    FieldSimple fieldSimple = (FieldSimple) FIELD;
                    ByteBuffer byteBuffer = fieldSimple.getByteBuffer();
                    String zString = BufferUtil.getZString(byteBuffer);
                    masters.add(zString);
                }
            }
        }
        this.MASTERS = java.util.Collections.unmodifiableList(new ArrayList<>(masters));

        Optional<ByteBuffer> HEDR = Optional.empty();
        for (Field f : this.FIELDS) {
            if (f.getCode().equals(IString.get("HEDR"))) {
                if (f instanceof FieldSimple) {
                    FieldSimple fieldSimple = (FieldSimple) f;
                    ByteBuffer byteBuffer = fieldSimple.getByteBuffer();
                    HEDR = Optional.of(byteBuffer);
                    break;
                }
            }
        }

        if (HEDR.isPresent()) {
            this.VERSION = HEDR.get().getFloat();
            this.RECORD_COUNT = HEDR.get().getInt();
            this.NEXT_RECORD = HEDR.get().getInt();
        } else {
            this.VERSION = Float.NaN;
            this.RECORD_COUNT = 0;
            this.NEXT_RECORD = 0;
        }

        /*Map<String, Integer> esps = new java.util.HashMap<>(espList.size());
        for (int i = 0; i < espList.size(); i++) {
            esps.put(espList.get(i), i);
        }

        this.ESPs = java.util.Collections.unmodifiableMap(esps);*/
    }

    /**
     * @see Entry#write(transposer.ByteBuffer)
     * @param output The ByteBuffer.
     */
    @Override
    public void write(ByteBuffer output) {
        output.put(this.CODE.toString().getBytes());
        output.putInt(this.calculateSize() - 24);
        this.HEADER.write(output);
        this.FIELDS.forEach(field -> field.write(output));
    }

    /**
     * @return The calculated size of the field.
     * @see Entry#calculateSize()
     */
    @Override
    public int calculateSize() {
        int sum = 24;
        int result = 0;
        for (Field FIELD : this.FIELDS) {
            int calculateSize = FIELD.calculateSize();
            result += calculateSize;
        }
        sum += result;
        return sum;
    }

    /**
     * Returns the record code.
     *
     * @return The record code.
     */
    @Override
    public RecordCode getCode() {
        return this.CODE;
    }

    /**
     * @return The record header.
     */
    public Record.Header getHeader() {
        return this.HEADER;
    }

    /**
     * Returns a String representation of the Record, which will just be the
     * code string.
     *
     * @return A string representation.
     *
     */
    @Override
    public String toString() {
        return this.getCode().toString();
    }

    /**
     * Remaps formIDs. If the formID's master is not available, the plugin field
     * of the formid will be set to 255.
     *
     * @param id The ID to remap.
     * @param ctx The mod descriptor.
     * @return
     */
    public int remapFormID(int id, ESPContext ctx) {
        int headerIndex = id >>> 24;
        assert 0 <= headerIndex && headerIndex < 256;

        if (headerIndex == this.MASTERS.size()) {
            return PluginInfo.makeFormID(this.PLUGIN, id);

        } else if (headerIndex < this.MASTERS.size()) {
            String originPluginName = this.MASTERS.get(headerIndex);
            Plugin origin = this.PLUGINS.getPaths().get(Paths.get(originPluginName));
            return origin == null ? (id | 0xFF000000) : PluginInfo.makeFormID(origin, id);
            
        } else {
            return id | 0xFF000000;
        }
    }

    final public Plugin PLUGIN;
    final public List<String> MASTERS;
    final private PluginInfo PLUGINS;

    final private RecordCode CODE;
    final private Record.Header HEADER;
    final private FieldList FIELDS;
    final private float VERSION;
    final private int RECORD_COUNT;
    final private int NEXT_RECORD;

}
