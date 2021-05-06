/*
 * Copyright 2017 Mark Fairchild.
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
package resaver.ess;

import java.nio.ByteBuffer;
import java.util.*;
import resaver.IString;
import resaver.ess.papyrus.EID;
import resaver.ess.papyrus.PapyrusContext;

/**
 * A very generalized element. It's not quite as efficient or customizable as
 * other elements, but it's good for elements that can have a range of different
 * members depending on flags.
 *
 * This should generally only be used for elements of which there are not very
 * many.
 *
 * @author Mark Fairchild
 */
public class GeneralElement implements Element {

    /**
     * Create a new <code>GeneralElement</code>.
     */
    protected GeneralElement() {
        this.DATA = new LinkedHashMap<>();
    }

    /**
     *
     * @return The number of sub-elements in the <code>Element</code>.
     */
    final public int count() {
        return this.DATA.size();
    }

    /**
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public Map<IString, Object> getValues() {
        return this.DATA;
        //return this.DATA.entrySet().stream()
        //        .collect(Collectors.toMap(IString k -> k.toString(), v -> v));
    }

    /**
     * Tests whether the <code>GeneralElement</code> contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public boolean hasVal(Enum<?> name) {
        Objects.requireNonNull(name);
        return this.hasVal(name.toString());
    }

    /**
     * Tests whether the <code>GeneralElement</code> contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public boolean hasVal(String name) {
        Objects.requireNonNull(name);
        return this.hasVal(IString.get(name));
    }

    /**
     * Retrieves a value by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match.
     */
    final public Object getVal(String name) {
        Objects.requireNonNull(name);
        return this.getVal(IString.get(name));
    }

    /**
     * Retrieves an <code>Element</code> by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not an <code>Element</code>.
     */
    final public Element getElement(String name) {
        Objects.requireNonNull(name);
        Object val = this.getVal(name);
        if (val instanceof Element) {
            return (Element) val;
        }
        return null;
    }

    /**
     * Retrieves a <code>GeneralElement</code> by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not a <code>GeneralElement</code>.
     */
    final public GeneralElement getGeneralElement(String name) {
        Objects.requireNonNull(name);
        Object val = this.getVal(name);
        if (val instanceof GeneralElement) {
            return (GeneralElement) val;
        }
        return null;
    }

    /**
     * Retrieves a <code>GeneralElement</code> by name from an
     * <code>Enum</code>.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not a <code>GeneralElement</code>.
     */
    final public Element getElement(Enum<?> name) {
        return this.getElement(Objects.requireNonNull(name.toString()));
    }

    /**
     * Reads a byte.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The byte.
     */
    final public byte readByte(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        byte val = input.get();
        return this.addValue(name, val);
    }

    /**
     * Reads a short.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The short.
     */
    final public short readShort(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        short val = input.getShort();
        return this.addValue(name, val);
    }

    /**
     * Reads an int.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The int.
     */
    final public int readInt(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        int val = input.getInt();
        return this.addValue(name, val);
    }

    /**
     * Reads an long.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The long.
     */
    final public long readLong(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        long val = input.getLong();
        return this.addValue(name, val);
    }

    /**
     * Reads a float.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The float.
     */
    final public float readFloat(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        float val = input.getFloat();
        return this.addValue(name, val);
    }

    /**
     * Reads a zstring.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The string.
     */
    final public String readZString(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        String val = mf.BufferUtil.getZString(input);
        return this.addValue(name, val);
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param reader The element reader.
     * @param <T> The element type.
     * @return The element.
     *
     */
    final public <T extends Element> T readElement(ByteBuffer input, Enum<?> name, ElementReader<T> reader) {
        return this.readElement(input, Objects.requireNonNull(name.toString()), reader);
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param reader The element reader.
     * @param <T> The element type.
     * @return The element.
     *
     */
    final public <T extends Element> T readElement(ByteBuffer input, String name, ElementReader<T> reader) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        T element = reader.read(input);
        return this.addValue(name, element);
    }

    /**
     * Reads a 32bit ID.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The Papyrus context data.
     * @return The ID.
     *
     */
    final public EID readID32(ByteBuffer input, String name, PapyrusContext context) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        return this.readElement(input, name, i -> context.readEID32(input));
    }

    /**
     * Reads a 64bit ID.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The Papyrus context data.
     * @return The ID.
     *
     */
    final public EID readID64(ByteBuffer input, String name, PapyrusContext context) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        return this.readElement(input, name, i -> context.readEID64(input));
    }

    /**
     * Reads a refid.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The <code>ESSContext</code>.
     * @return The RefID.
     *
     */
    final public RefID readRefID(ByteBuffer input, String name, ESS.ESSContext context) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        return this.readElement(input, name, i -> context.readRefID(input));
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The RefID.
     *
     */
    final public VSVal readVSVal(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        VSVal val = new VSVal(input);
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length byte array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     *
     */
    final public byte[] readBytes(ByteBuffer input, String name, int size) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        if (size < 0) {
            throw new IllegalArgumentException("Negative array count: " + size);
        } else if (256 < size) {
            throw new IllegalArgumentException("Excessive array count: " + size);
        }

        byte[] val = new byte[size];
        input.get(val);
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length short array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     *
     */
    final public short[] readShorts(ByteBuffer input, String name, int size) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        if (size < 0) {
            throw new IllegalArgumentException("Negative array count: " + size);
        } else if (256 < size) {
            throw new IllegalArgumentException("Excessive array count: " + size);
        }

        short[] val = new short[size];
        for (int i = 0; i < size; i++) {
            val[i] = input.getShort();
        }
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length int array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     *
     */
    final public int[] readInts(ByteBuffer input, String name, int size) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        if (size < 0) {
            throw new IllegalArgumentException("Negative array count: " + size);
        } else if (256 < size) {
            throw new IllegalArgumentException("Excessive array count: " + size);
        }

        int[] val = new int[size];
        for (int i = 0; i < size; i++) {
            val[i] = input.getInt();
        }
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length long array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     *
     */
    final public long[] readLongs(ByteBuffer input, String name, int size) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        if (size < 0) {
            throw new IllegalArgumentException("Negative array count: " + size);
        } else if (256 < size) {
            throw new IllegalArgumentException("Excessive array count: " + size);
        }

        long[] val = new long[size];
        for (int i = 0; i < size; i++) {
            val[i] = input.getLong();
        }
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length float array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     *
     */
    final public float[] readFloats(ByteBuffer input, String name, int size) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        if (size < 0) {
            throw new IllegalArgumentException("Negative array count: " + size);
        } else if (256 < size) {
            throw new IllegalArgumentException("Excessive array count: " + size);
        }

        float[] val = new float[size];
        for (int i = 0; i < size; i++) {
            val[i] = input.getFloat();
        }
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length element array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @param reader The element reader.
     * @return The array.
     * @param <T> The element type.
     *
     */
    final public <T extends Element> Element[] readElements(ByteBuffer input, String name, int size, ElementReader<T> reader) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);
        Objects.requireNonNull(reader);

        if (size < 0) {
            throw new IllegalArgumentException("Negative array count: " + size);
        } else if (256 < size) {
            throw new IllegalArgumentException("Excessive array count: " + size);
        }

        Element[] val = new Element[size];

        for (int i = 0; i < size; i++) {
            T element = reader.read(input);
            val[i] = element;
        }
        return this.addValue(name, val);
    }

    /**
     * Reads a fixed-length byte array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     *
     */
    final public byte[] readBytesVS(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readBytes(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length short array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     *
     */
    final public short[] readShortsVS(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readShorts(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length int array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     *
     */
    final public int[] readIntsVS(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readInts(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length long array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     *
     */
    final public long[] readLongsVS(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readLongs(input, name, COUNT.getValue());
    }

    /**
     * Reads a fixed-length float array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     *
     */
    final public float[] readFloatsVS(ByteBuffer input, String name) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }
        return this.readFloats(input, name, COUNT.getValue());
    }

    /**
     * Reads an array of elements using a supplier functional.
     *
     * @param input The inputstream.
     * @param reader
     * @param name The name of the new element.
     * @param <T> The element type.
     * @return The array.
     *
     */
    @SuppressWarnings("unchecked")
    final public <T extends Element> T[] readVSElemArray(ByteBuffer input, String name, ElementReader<T> reader) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(name);

        final VSVal COUNT = this.readVSVal(input, name + "_COUNT");
        if (COUNT.getValue() < 0) {
            throw new IllegalArgumentException("Negative array count: " + COUNT);
        }

        Element[] val = new Element[COUNT.getValue()];

        for (int i = 0; i < COUNT.getValue(); i++) {
            T e = reader.read(input);
            val[i] = e;
        }

        return (T[]) this.addValue(name, val);
    }

    /**
     * Reads an array of elements using a supplier functional.
     *
     * @param input The inputstream.
     * @param reader
     * @param name The name of the new element.
     * @param <T> The element type.
     * @return The array.
     *
     */
    final public <T extends Element> Element[] read32ElemArray(ByteBuffer input, String name, ElementReader<T> reader) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(name);

        final int COUNT = this.readInt(input, name + "_COUNT");
        if (COUNT < 0) {
            throw new IllegalArgumentException("Count is negative: " + COUNT);
        }

        Element[] val = new Element[COUNT];
        for (int i = 0; i < COUNT; i++) {
            T e = reader.read(input);
            val[i] = e;
        }

        return this.addValue(name, val);
    }

    /**
     * Adds an object value.
     *
     * @param name The name of the new element.
     * @param val The value.
     */
    private <T> T addValue(String name, T val) {
        boolean b = true;
        for (Class<?> type : SUPPORTED) {
            if (type.isInstance(val)) {
                b = false;
                break;
            }
        }
        if (b) {
            throw new IllegalStateException(String.format("Invalid type for %s: %s", name, val.getClass()));
        }

        this.DATA.put(IString.get(name), val);
        return val;
    }

    /**
     * @see Element#write(ByteBuffer)
     * @param output output buffer
     */
    @Override
    public void write(ByteBuffer output) {
        this.DATA.values().forEach(v -> {
            if (v instanceof Element) {
                Element element = (Element) v;
                element.write(output);
            } else if (v instanceof Byte) {
                output.put((Byte) v);
            } else if (v instanceof Short) {
                output.putShort((Short) v);
            } else if (v instanceof Integer) {
                output.putInt((Integer) v);
            } else if (v instanceof Float) {
                output.putFloat((Float) v);
            } else if (v instanceof String) {
                mf.BufferUtil.putZString(output, (String) v);
            } else if (v instanceof byte[]) {
                output.put((byte[]) v);
            } else if (v instanceof short[]) {
                final short[] ARR = (short[]) v;
                for (short s : ARR) {
                    output.putShort(s);
                }
            } else if (v instanceof int[]) {
                final int[] ARR = (int[]) v;
                for (int i : ARR) {
                    output.putInt(i);
                }
            } else if (v instanceof float[]) {
                final float[] ARR = (float[]) v;
                for (float f : ARR) {
                    output.putFloat(f);
                }
            } else if (v instanceof Element[]) {
                final Element[] ARR = (Element[]) v;
                for (Element e : ARR) {
                    e.write(output);
                }
            } else if (v == null) {
                throw new IllegalStateException("Null element!");
            } else {
                throw new IllegalStateException("Unknown element: " + v.getClass());
            }
        });
    }

    /**
     * @see Element#calculateSize()
     * @return
     */
    @Override
    public int calculateSize() {
        if (this.DATA.containsValue(null)) {
            throw new NullPointerException("GeneralElement may not contain null.");
        }

        int sum = 0;

        for (Object v : this.DATA.values()) {
            if (v instanceof Element) {
                sum += ((Element) v).calculateSize();
            } else if (v instanceof Byte) {
                sum += 1;
            } else if (v instanceof Short) {
                sum += 2;
            } else if (v instanceof Integer) {
                sum += 4;
            } else if (v instanceof Float) {
                sum += 4;
            } else if (v instanceof String) {
                sum += 1 + ((String) v).getBytes().length;
            } else if (v instanceof byte[]) {
                sum += 1 * ((byte[]) v).length;
            } else if (v instanceof short[]) {
                sum += 2 * ((short[]) v).length;
            } else if (v instanceof int[]) {
                sum += 4 * ((int[]) v).length;
            } else if (v instanceof float[]) {
                sum += 4 * ((float[]) v).length;
            } else if (v instanceof Element[]) {
                sum += Arrays.stream((Element[]) v).mapToInt(Element::calculateSize).sum();
            } else if (v == null) {
                throw new IllegalStateException("Null element!");
            } else {
                throw new IllegalStateException("Unknown element: " + v.getClass());
            }
        }

        return sum;
    }

    /**
     * @see java.lang.Object#hashCode()
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.DATA);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }

        final GeneralElement other = (GeneralElement) obj;
        return Objects.equals(this.DATA, other.DATA);
    }

    /**
     *
     * @return String representation.
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     *
     * @return String representation.
     */
    public String toTextBlock() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (IString n : DATA.keySet()) {
            String format = String.format("%s=%s", n, getVal(n));
            joiner.add(format);
        }
        return joiner.toString();
    }

    /**
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    public String toString(int level) {
        return this.toString(null, level);
    }

    /**
     * @param name A name to display.
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    protected String toString(String name, int level) {
        final StringBuilder BUF = new StringBuilder();

        if (this.DATA.keySet().isEmpty()) {
            indent(BUF, level);
            if (null != name) {
                BUF.append(name);
            }
            BUF.append("{}");
            return BUF.toString();
        }

        indent(BUF, level);
        if (null != name) {
            BUF.append(name);
        }

        BUF.append("{\n");

        this.DATA.forEach((key, val) -> {
            if (val instanceof GeneralElement) {
                GeneralElement element = (GeneralElement) val;
                String str = element.toString(key.toString(), level + 1);
                BUF.append(str);
                BUF.append('\n');
            } else if (val instanceof Element[]) {
                String str = eaToString(key, level + 1, (Element[]) val);
                BUF.append(str);
                BUF.append('\n');
            } else {
                indent(BUF, level + 1);
                String str;
                if (val instanceof Byte) {
                    str = String.format("%02x", Byte.toUnsignedInt((Byte) val));
                } else if (val instanceof Short) {
                    str = String.format("%04x", Short.toUnsignedInt((Short) val));
                } else if (val instanceof Integer) {
                    str = String.format("%08x", Integer.toUnsignedLong((Integer) val));
                } else if (val instanceof Long) {
                    str = String.format("%16x", (Long) val);
                } else if (val instanceof Object[]) {
                    str = Arrays.toString((Object[]) val);
                } else if (val instanceof boolean[]) {
                    str = Arrays.toString((boolean[]) val);
                } else if (val instanceof byte[]) {
                    str = Arrays.toString((byte[]) val);
                } else if (val instanceof char[]) {
                    str = Arrays.toString((char[]) val);
                } else if (val instanceof double[]) {
                    str = Arrays.toString((double[]) val);
                } else if (val instanceof float[]) {
                    str = Arrays.toString((float[]) val);
                } else if (val instanceof int[]) {
                    str = Arrays.toString((int[]) val);
                } else if (val instanceof long[]) {
                    str = Arrays.toString((long[]) val);
                } else if (val instanceof short[]) {
                    str = Arrays.toString((short[]) val);
                } else {
                    str = Objects.toString(val);
                }

                BUF.append(String.format("%s=%s\n", key, str));
            }
        });

        indent(BUF, level);

        BUF.append("}");
        return BUF.toString();
    }

    /**
     * Tests whether the <code>GeneralElement</code> contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name,value> map.
     *
     */
    final public boolean hasVal(IString name) {
        Objects.requireNonNull(name);
        return this.DATA.containsKey(name);
    }

    /**
     * Retrieves a value by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match.
     */
    private Object getVal(IString name) {
        Objects.requireNonNull(name);
        return this.DATA.get(name);
    }

    /**
     * Appends <code>n</code> indents to a <code>StringBuilder</code>.
     *
     * @param b
     * @param n
     */
    static private void indent(StringBuilder b, int n) {
        for (int i = 0; i < n; i++) {
            b.append('\t');
        }
    }

    /**
     * Creates a string representation of an <code>ElementArrayList</code>.
     *
     * @param name A name to display.
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    static private String eaToString(IString name, int level, Element[] list) {
        final StringBuilder BUF = new StringBuilder();

        if (list.length == 0) {
            indent(BUF, level);
            if (null != name) {
                BUF.append(name);
            }
            BUF.append("[]");
            return BUF.toString();
        }

        indent(BUF, level);
        if (null != name) {
            BUF.append(name);
        }

        BUF.append("[\n");

        for (Element e : list) {
            if (e instanceof GeneralElement) {
                GeneralElement element = (GeneralElement) e;
                String str = element.toString(level + 1);
                BUF.append(str).append('\n');
            } else if (e != null) {
                indent(BUF, level + 1);
                String str = e.toString();
                BUF.append(str).append('\n');
            } else {
                BUF.append("null");
            }
        }

        indent(BUF, level);
        BUF.append("]");
        return BUF.toString();
    }

    /**
     * @see AnalyzableElement#getInfo(resaver.Analysis, resaver.ess.ESS)
     * @param analysis
     * @param save
     * @return
     */
    public String getInfo(resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        BUF.append("<table border=1>");

        this.DATA.forEach((key, val) -> {
            if (val instanceof Linkable) {
                final Linkable LINKABLE = (Linkable) val;
                final String STR = LINKABLE.toHTML(null);
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, STR));

            } else if (val instanceof List<?>) {
                final List<?> LIST = (List<?>) val;
                final String STR = GeneralElement.formatList(key.toString(), LIST, analysis, save);
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, STR));

            } else if (val instanceof GeneralElement) {
                final GeneralElement GEN = (GeneralElement) val;
                final String STR = GeneralElement.formatGeneralElement(key.toString(), GEN, analysis, save);
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, STR));

            } else {
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, val));
            }
        });

        BUF.append("</table>");
        return BUF.toString();
    }

    static private String formatElement(String key, Object val, resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        if (val == null) {
            BUF.append(String.format("%s: <NULL>", key));

        } else if (val instanceof Linkable) {
            final Linkable LINKABLE = (Linkable) val;
            final String STR = LINKABLE.toHTML(null);
            BUF.append(String.format("%s: %s", key, STR));

        } else if (val instanceof List<?>) {
            final List<?> LIST = (List<?>) val;
            final String STR = GeneralElement.formatList(key, LIST, analysis, save);
            BUF.append(String.format("%s: %s", key, STR));

        } else if (val.getClass().isArray()) {
            if (val instanceof Object[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((Object[]) val)));
            } else if (val instanceof boolean[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((boolean[]) val)));
            } else if (val instanceof byte[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((byte[]) val)));
            } else if (val instanceof char[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((char[]) val)));
            } else if (val instanceof double[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((double[]) val)));
            } else if (val instanceof float[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((float[]) val)));
            } else if (val instanceof int[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((int[]) val)));
            } else if (val instanceof long[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((long[]) val)));
            } else if (val instanceof short[]) {
                BUF.append(String.format("%s: %s", key, Arrays.toString((short[]) val)));
            }
            final List<?> LIST = (List<?>) val;
            final String STR = GeneralElement.formatList(key, LIST, analysis, save);
            BUF.append(String.format("%s: %s", key, STR));

        } else if (val instanceof GeneralElement) {
            final GeneralElement GEN = (GeneralElement) val;
            final String STR = GeneralElement.formatGeneralElement(key, GEN, analysis, save);
            BUF.append(String.format("%s: %s", key, STR));

        } else {
            BUF.append(String.format("%s: %s", key, val));
        }
        return BUF.toString();
    }

    static private String formatGeneralElement(String key, GeneralElement gen, resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        gen.getValues().forEach((k, v) -> {
            final String S = GeneralElement.formatElement(k.toString(), v, analysis, save);
            BUF.append(String.format("<p>%s</p>", S));
        });
        //BUF.append("</ol>");
        return BUF.toString();
    }

    static private String formatList(String key, List<?> list, resaver.Analysis analysis, ESS save) {
        final StringBuilder BUF = new StringBuilder();
        //BUF.append(String.format("<p>%s</p>", key));
        int i = 0;
        for (Object val : list) {
            final String K = Integer.toString(i);
            final String S = GeneralElement.formatElement(K, val, analysis, save);
            BUF.append(String.format("<p>%s</p>", S));
            i++;
        }
        //BUF.append("<");
        return BUF.toString();
    }

    /**
     * Stores the actual data.
     */
    final private Map<IString, Object> DATA;

    static final private Set<Class<?>> SUPPORTED = new HashSet<>(Arrays.asList(
            Element.class,
            Byte.class,
            Short.class,
            Integer.class,
            Float.class,
            String.class,
            byte[].class,
            short[].class,
            int[].class,
            long[].class,
            float[].class,
            Object[].class));

    @FunctionalInterface
    static public interface ElementReader<T extends Element> {

        T read(ByteBuffer input);
    }

}
