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
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import resaver.IString;

/**
 * Describes script fragments for QUST records.
 *
 * @author Mark Fairchild
 */
public class FragmentQust extends FragmentBase {

    public FragmentQust(@NotNull ByteBuffer input, @NotNull ESPContext ctx) {
        try {
            this.UNKNOWN = input.get();
            int fragmentCount = Short.toUnsignedInt(input.getShort());

            if (ctx.GAME.isFO4()) {
                ctx.pushContext("FragmentQust");
                this.FILENAME = null;
                this.SCRIPT = new Script(input, ctx);
                ctx.PLUGIN_INFO.addScriptData(this.SCRIPT);
            } else {
                this.FILENAME = mf.BufferUtil.getUTF(input);
                this.SCRIPT = null;
                ctx.pushContext("FragmentQust:" + this.FILENAME);
            }

            this.FRAGMENTS = new java.util.LinkedList<>();
            this.ALIASES = new java.util.LinkedList<>();

            for (int i = 0; i < fragmentCount; i++) {
                Fragment fragment = new Fragment(input, ctx);
                this.FRAGMENTS.add(fragment);
            }

            int aliasCount = Short.toUnsignedInt(input.getShort());
            for (int i = 0; i < aliasCount; i++) {
                Alias alias = new Alias(input, ctx);
                this.ALIASES.add(alias);
            }

        } finally {
            ctx.popContext();
        }
    }

    @Override
    public void write(@NotNull ByteBuffer output) {
        output.put(this.UNKNOWN);
        output.putShort((short) this.FRAGMENTS.size());
        if (null != this.FILENAME) {
            output.put(this.FILENAME.getBytes(UTF_8));
        }
        if (null != this.SCRIPT) {
            this.SCRIPT.write(output);
        }

        this.FRAGMENTS.forEach(fragment -> fragment.write(output));

        output.putShort((short) this.ALIASES.size());
        this.ALIASES.forEach(alias -> alias.write(output));
    }

    @Override
    public int calculateSize() {
        int sum = 5;
        sum += (null != this.FILENAME ? 2 + this.FILENAME.length() : 0);
        sum += (null != this.SCRIPT ? this.SCRIPT.calculateSize() : 0);
        int result = 0;
        for (Fragment FRAGMENT : this.FRAGMENTS) {
            int calculateSize = FRAGMENT.calculateSize();
            result += calculateSize;
        }
        sum += result;
        int sum1 = 0;
        for (Alias ALIAS : this.ALIASES) {
            int calculateSize = ALIAS.calculateSize();
            sum1 += calculateSize;
        }
        sum += sum1;
        return sum;
    }

    @Override
    public String toString() {
        if (null != this.SCRIPT) {
            return String.format("Quest: %s (%d, %d frags, %d aliases)", this.SCRIPT.NAME, this.UNKNOWN, this.FRAGMENTS.size(), this.ALIASES.size());
        } else if (null != this.FILENAME) {
            return String.format("Quest: %s (%d, %d frags, %d aliases)", this.FILENAME, this.UNKNOWN, this.FRAGMENTS.size(), this.ALIASES.size());
        } else {
            return String.format("Quest: (%d, %d frags, %d aliases)", this.UNKNOWN, this.FRAGMENTS.size(), this.ALIASES.size());

        }
    }

    final private byte UNKNOWN;
    @Nullable
    final private String FILENAME;
    @Nullable
    final private Script SCRIPT;
    @NotNull
    final private List<Fragment> FRAGMENTS;
    @NotNull
    final private List<Alias> ALIASES;

    /**
     *
     */
    public class Fragment implements Entry {

        public Fragment(@NotNull ByteBuffer input, ESPContext ctx) {
            this.STAGE = Short.toUnsignedInt(input.getShort());
            this.UNKNOWN1 = input.getShort();
            this.LOGENTRY = input.getInt();
            this.UNKNOWN2 = input.get();
            this.SCRIPTNAME = IString.get(mf.BufferUtil.getUTF(input));
            this.FRAGMENTNAME = IString.get(mf.BufferUtil.getUTF(input));
        }

        @Override
        public void write(@NotNull ByteBuffer output) {
            output.putShort((short) this.STAGE);
            output.putShort(this.UNKNOWN1);
            output.putInt(this.LOGENTRY);
            output.put(this.UNKNOWN2);
            output.put(this.SCRIPTNAME.getUTF8());
            output.put(this.FRAGMENTNAME.getUTF8());
        }

        @Override
        public int calculateSize() {
            int sum = 13;
            sum += this.SCRIPTNAME.length();
            sum += this.FRAGMENTNAME.length();
            return sum;
        }

        final private int STAGE;
        final private short UNKNOWN1;
        final private int LOGENTRY;
        final private byte UNKNOWN2;
        @NotNull
        final private IString SCRIPTNAME;
        @NotNull
        final private IString FRAGMENTNAME;
    }

    /**
     *
     */
    public class Alias implements Entry {

        public Alias(@NotNull ByteBuffer input, @NotNull ESPContext ctx) {
            this.OBJECT = input.getLong();
            this.VERSION = input.getShort();
            this.OBJFORMAT = input.getShort();
            this.SCRIPTS = new java.util.LinkedList<>();

            int scriptCount = Short.toUnsignedInt(input.getShort());
            for (int i = 0; i < scriptCount; i++) {
                Script script = new Script(input, ctx);
                this.SCRIPTS.add(script);
                ctx.PLUGIN_INFO.addScriptData(script);
            }
        }

        @Override
        public void write(@NotNull ByteBuffer output) {
            output.putLong(this.OBJECT);
            output.putShort(this.VERSION);
            output.putShort(this.OBJFORMAT);
            output.putShort((short) this.SCRIPTS.size());
            this.SCRIPTS.forEach(script -> script.write(output));
        }

        @Override
        public int calculateSize() {
            int sum = 14;
            int result = 0;
            for (Script script : this.SCRIPTS) {
                int calculateSize = script.calculateSize();
                result += calculateSize;
            }
            sum += result;
            return sum;
        }

        final private long OBJECT;
        final private short VERSION;
        final private short OBJFORMAT;
        @NotNull
        final private List<Script> SCRIPTS;
    }
}
