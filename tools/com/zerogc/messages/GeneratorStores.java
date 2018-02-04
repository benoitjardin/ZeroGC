/*
 * Copyright 2016 Benoit Jardin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zerogc.messages;

import java.io.IOException;
import java.io.PrintStream;

import org.xml.sax.SAXException;


/**
 * @author Benoit Jardin
 */
public class GeneratorStores extends GeneratorBase {
    static final String PACKAGE = "com.zerogc.generated.stores";

    protected GeneratorStores(String pkg) {
        super(pkg);
    }

    protected GeneratorStores(GeneratorStores generator) {
        super(generator);
    }

    @Override
    protected GeneratorBase cloneGenerator() {
        return new GeneratorStores(this);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (location.equals("/xs:schema/xs:element")) {
            try {
                StringBuilder gettersAndSetterStringBuilder = new StringBuilder();
                StringBuilder byteOffsetStringBuilder = new StringBuilder();
                StringBuilder byteLengthStringBuilder = new StringBuilder();
                StringBuilder shortOffsetStringBuilder = new StringBuilder();
                StringBuilder shortLengthStringBuilder = new StringBuilder();
                StringBuilder intOffsetStringBuilder = new StringBuilder();
                StringBuilder intLengthStringBuilder = new StringBuilder();
                StringBuilder longOffsetStringBuilder = new StringBuilder();
                StringBuilder longLengthStringBuilder = new StringBuilder();
                StringBuilder doubleOffsetStringBuilder = new StringBuilder();
                StringBuilder doubleLengthStringBuilder = new StringBuilder();

                StringBuilder toStringStringBuilder = new StringBuilder();
                StringBuilder clearStringBuilder = new StringBuilder();

                int byteOffset = 0;
                int shortOffset = 0;
                int intOffset = 0;
                int longOffset = 0;
                int doubleOffset = 0;

                String indent = "        ";
                for (Pair<String, String> column : tableColumns) {
                    String columnName = column.getFirst();
                    String columnTypeName = column.getSecond();
                    Type columnType = simpleTypeHashtable.get(columnTypeName);

                    if (columnType == null) {
                        columnType = Type.create(columnTypeName);
                    }

                    String toString = indent + "sb.append(\"[" + columnName + "=\").append(get" + pascal(columnName) + "(slot)).append(']');\n";
                    String clear = indent + "set" + pascal(columnName) + "(slot, 0);\n";

                    String getString = null; // Get as native type
                    String setString = null; // Set as native type
                    String getSetType = columnType.javaName;

                    if (columnType.javaName.equals("byte")) {
                        clear = indent + "set" + pascal(columnName) + "(slot, (byte)0);\n";
                        getString = "this.bytes[slot*ByteOffsets.end + ByteOffsets." + columnName + "]";
                        setString = "this.bytes[slot*ByteOffsets.end + ByteOffsets." + columnName + "] = value";
                    } else if (columnType.javaName.equals("short")) {
                        getString = "this.shorts[slot*IntOffsets.end + ShortOffsets." + columnName + "]";
                        setString = "this.shorts[slot*IntOffsets.end + ShortOffsets." + columnName + "] = value";
                    } else if (columnType.javaName.equals("int")) {
                        getString = "this.ints[slot*IntOffsets.end + IntOffsets." + columnName + "]";
                        setString = "this.ints[slot*IntOffsets.end + IntOffsets." + columnName + "] = value";
                    } else if (columnType.javaName.equals("long")) {
                        getString = "this.longs[slot*LongOffsets.end + LongOffsets." + columnName + "]";
                        setString = "this.longs[slot*LongOffsets.end + LongOffsets." + columnName + "] = value";
                    } else if (columnType.javaName.equals("double")) {
                        getString = "this.doubles[slot*DoubleOffsets.end + DoubleOffsets." + columnName + "]";
                        setString = "this.doubles[slot*DoubleOffsets.end + DoubleOffsets." + columnName + "] = value";
                    } else {
                        if (columnType.maxLength == 1) {
                            getString = "this.bytes[slot*ByteOffsets.end + ByteOffsets." + columnName + "]&0xFF";
                            setString = "this.bytes[slot*ByteOffsets.end + ByteOffsets." + columnName + "] = (byte)value";
                        }
                    }

                    if (columnType.xsdTypeName.equals("xs:string") && columnType.maxLength > 1) {
                        clear = indent + "set" + pascal(columnName) + "(slot, emptySlice);\n";
                        byteOffsetStringBuilder.append("        public static final int " + columnName + " = " + byteOffset + ";\n");
                        byteLengthStringBuilder.append("        public static final int " + columnName + " = " + columnType.maxLength + ";\n");
                        byteOffset += columnType.maxLength;
                        gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(int slot, ByteSlice byteSlice) {\n");
                        gettersAndSetterStringBuilder.append("        ByteUtils.copySpacePadded(byteSlice.getBuffer(), byteSlice.getOffset(), byteSlice.getLength(), this.bytes,  slot*ByteOffsets.end + ByteOffsets." + columnName + ", ByteLengths." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                        gettersAndSetterStringBuilder.append("    public ByteSlice get" + pascal(columnName) + "(int slot, ByteSlice byteSlice) {\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setBuffer(this.bytes);\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setOffset(slot*ByteOffsets.end + ByteOffsets." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setLength(ByteLengths." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        return byteSlice;\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                        gettersAndSetterStringBuilder.append("    @Deprecated\n");
                        gettersAndSetterStringBuilder.append("    public ByteSlice get" + pascal(columnName) + "(int slot) {\n");
                        gettersAndSetterStringBuilder.append("        return get" + pascal(columnName) + "(slot, slice);\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                    } else {
                        if (columnType.javaName.equals("byte")) {
                            byteOffsetStringBuilder.append("        public static final int " + columnName + " = " + byteOffset + ";\n");
                            byteLengthStringBuilder.append("        public static final int " + columnName + " = " + columnType.maxLength + ";\n");
                            byteOffset += columnType.maxLength;
                        } else if (columnType.javaName.equals("short")) {
                            shortOffsetStringBuilder.append("        public static final int " + columnName + " = " + shortOffset + ";\n");
                            shortLengthStringBuilder.append("        public static final int " + columnName + " = " + columnType.maxLength/2 + ";\n");
                            shortOffset += columnType.maxLength/2;
                        } else if (columnType.javaName.equals("int")) {
                            intOffsetStringBuilder.append("        public static final int " + columnName + " = " + intOffset + ";\n");
                            intLengthStringBuilder.append("        public static final int " + columnName + " = " + columnType.maxLength/4 + ";\n");
                            intOffset += columnType.maxLength/4;
                        } else if (columnType.javaName.equals("long")) {
                            longOffsetStringBuilder.append("        public static final int " + columnName + " = " + longOffset + ";\n");
                            longLengthStringBuilder.append("        public static final int " + columnName + " = " + columnType.maxLength/8 + ";\n");
                            longOffset += columnType.maxLength/8;
                        } else if (columnType.javaName.equals("double")) {
                            doubleOffsetStringBuilder.append("        public static final int " + columnName + " = " + doubleOffset + ";\n");
                            doubleLengthStringBuilder.append("        public static final int " + columnName + " = " + columnType.maxLength/8 + ";\n");
                            doubleOffset += columnType.maxLength/8;
                        }
                        gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(int slot, " + getSetType + " value) {\n");
                        gettersAndSetterStringBuilder.append("        " + setString + ";\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                        gettersAndSetterStringBuilder.append("    public " + getSetType + " get" + pascal(columnName) + "(int slot) {\n");
                        gettersAndSetterStringBuilder.append("        return " + getString + ";\n");
                        gettersAndSetterStringBuilder.append("    }\n");

                        if (columnType.fractionDigits > 0) {
                            toString =  indent + "sb.append(\"[" + columnName + "=\").append(get" + pascal(columnName) + "(slot), " + columnType.fractionDigits + ", 0).append(']');\n";
                        }
                    }

                    toStringStringBuilder.append(toString);
                    clearStringBuilder.append(clear);
                }

                System.out.println("Generate: " + pascal(prefix) + pascal(complexTypeName) + "Store.java");
                PrintStream printStream = new PrintStream(generatedDir + "/" + pascal(prefix) + pascal(complexTypeName) + "Store.java");

                printStream.println(comment);
                printStream.println();
                printStream.println("package " + getPackage() + ";");
                printStream.println();

                printStream.println("import com.zerogc.core.ByteUtils;");
                printStream.println("import com.zerogc.core.ByteSlice;");
                printStream.println("import com.zerogc.core.ByteStringBuilder;");
                printStream.println("import com.zerogc.logging.Level;");
                printStream.println("import com.zerogc.logging.LogManager;");
                printStream.println("import com.zerogc.logging.Logger;");
                printStream.println();

                printStream.println("public class " + pascal(prefix)+pascal(complexTypeName) + "Store {");
                printStream.println("    protected final Logger log;");
                printStream.println();

                if (byteLengthStringBuilder.length() > 0) {
                    printStream.println("    public static class ByteLengths {");
                    printStream.print(byteLengthStringBuilder);
                    printStream.println("    }");
                    printStream.println();
                    printStream.println("    public static class ByteOffsets {");
                    printStream.print(byteOffsetStringBuilder);
                    printStream.println("        public static final int end = " + byteOffset + ";");
                    printStream.println("    }");
                    printStream.println();
                }
                if (shortLengthStringBuilder.length() > 0) {
                    printStream.println("    public static class ShortLengths {");
                    printStream.print(shortLengthStringBuilder);
                    printStream.println("    }");
                    printStream.println();
                    printStream.println("    public static class ShortOffsets {");
                    printStream.print(shortOffsetStringBuilder);
                    printStream.println("        public static final int end = " + shortOffset + ";");
                    printStream.println("    }");
                    printStream.println();
                }
                if (intLengthStringBuilder.length() > 0) {
                    printStream.println("    public static class IntLengths {");
                    printStream.print(intLengthStringBuilder);
                    printStream.println("    }");
                    printStream.println();
                    printStream.println("    public static class IntOffsets {");
                    printStream.print(intOffsetStringBuilder);
                    printStream.println("        public static final int end = " + intOffset + ";");
                    printStream.println("    }");
                    printStream.println();
                }
                if (longLengthStringBuilder.length() > 0) {
                    printStream.println("    public static class LongLengths {");
                    printStream.print(longLengthStringBuilder);
                    printStream.println("    }");
                    printStream.println();
                    printStream.println("    public static class LongOffsets {");
                    printStream.print(longOffsetStringBuilder);
                    printStream.println("        public static final int end = " + longOffset + ";");
                    printStream.println("    }");
                    printStream.println();
                }
                if (doubleLengthStringBuilder.length() > 0) {
                    printStream.println("    public static class DoubleLengths {");
                    printStream.print(doubleLengthStringBuilder);
                    printStream.println("    }");
                    printStream.println();
                    printStream.println("    public static class DoubleOffsets {");
                    printStream.print(doubleOffsetStringBuilder);
                    printStream.println("        public static final int end = " + doubleOffset + ";");
                    printStream.println("    }");
                    printStream.println();
                }
                if (byteLengthStringBuilder.length() > 0) {
                    printStream.println("    private byte[] bytes;");
                }
                if (shortLengthStringBuilder.length() > 0) {
                    printStream.println("    private short[] shorts;");
                }
                if (intLengthStringBuilder.length() > 0) {
                    printStream.println("    private int[] ints;");
                }
                if (longLengthStringBuilder.length() > 0) {
                    printStream.println("    private long[] longs;");
                }
                if (doubleLengthStringBuilder.length() > 0) {
                    printStream.println("    private double[] doubles;");
                }
                printStream.println();
                printStream.println("    protected int capacity = 0;");
                printStream.println();
                printStream.println("    private final ByteSlice emptySlice = new ByteSlice(new byte[0], 0, 0);");
                printStream.println("    private final ByteSlice slice = new ByteSlice();");
                printStream.println();
                printStream.println("    public " + pascal(prefix)+pascal(complexTypeName) + "Store(String name, int initialCapacity) {");
                printStream.println("        log = LogManager.getLogger(name);");
                printStream.println("        grow(initialCapacity);");
                printStream.println("    }");
                printStream.println();
                printStream.println("    public int getCapacity() {");
                printStream.println("       return capacity;");
                printStream.println("    }");
                printStream.println();
                printStream.println("    protected void grow(int newCapacity) {");
                printStream.println("        if (capacity > 0) {");
                printStream.println("           log.log(Level.WARN, log.getSB().append(\"Resizing "+pascal(prefix)+pascal(complexTypeName)+"Store from \").append(capacity).append(\" to \").append(newCapacity));");
                printStream.println("        }");
                printStream.println("        ");
                if (byteLengthStringBuilder.length() > 0) {
                    printStream.println("        byte[] newBytes = new byte[newCapacity*ByteOffsets.end];");
                }
                if (shortLengthStringBuilder.length() > 0) {
                    printStream.println("        short[] newShorts = new short[newCapacity*ShortOffsets.end];");
                }
                if (intLengthStringBuilder.length() > 0) {
                    printStream.println("        int[] newInts = new int[newCapacity*IntOffsets.end];");
                }
                if (longLengthStringBuilder.length() > 0) {
                    printStream.println("        long[] newLongs = new long[newCapacity*LongOffsets.end];");
                }
                if (doubleLengthStringBuilder.length() > 0) {
                    printStream.println("        double[] newDoubles = new double[newCapacity*DoubleOffsets.end];");
                }
                printStream.println("        ");
                printStream.println("        if (capacity > 0) {");
                if (byteLengthStringBuilder.length() > 0) {
                    printStream.println("            System.arraycopy(bytes, 0, newBytes, 0, capacity*ByteOffsets.end);");
                }
                if (shortLengthStringBuilder.length() > 0) {
                    printStream.println("            System.arraycopy(shorts, 0, newShorts, 0, capacity*ShortOffsets.end);");
                }
                if (intLengthStringBuilder.length() > 0) {
                    printStream.println("            System.arraycopy(ints, 0, newInts, 0, capacity*IntOffsets.end);");
                }
                if (longLengthStringBuilder.length() > 0) {
                    printStream.println("            System.arraycopy(longs, 0, newLongs, 0, capacity*LongOffsets.end);");
                }
                if (doubleLengthStringBuilder.length() > 0) {
                    printStream.println("            System.arraycopy(doubles, 0, newDoubles, 0, capacity*DoubleOffsets.end);");
                }
                printStream.println("        }");
                if (byteLengthStringBuilder.length() > 0) {
                    printStream.println("        bytes = newBytes;");
                }
                if (shortLengthStringBuilder.length() > 0) {
                    printStream.println("        shorts = newShorts;");
                }
                if (intLengthStringBuilder.length() > 0) {
                    printStream.println("        ints = newInts;");
                }
                if (longLengthStringBuilder.length() > 0) {
                    printStream.println("        longs = newLongs;");
                }
                if (doubleLengthStringBuilder.length() > 0) {
                    printStream.println("        doubles = newDoubles;");
                }
                printStream.println("        capacity = newCapacity;");
                printStream.println("    }");
                printStream.println();

                printStream.print(gettersAndSetterStringBuilder);

                printStream.println();
                printStream.println("    public void clear(int slot) {");
                printStream.print(clearStringBuilder);
                printStream.println("    }");
                printStream.println();
                printStream.println();
                printStream.println("    public ByteStringBuilder toString(ByteStringBuilder sb, int slot) {");
                printStream.println("        sb.append(\"" + pascal(prefix)+pascal(complexTypeName) + "Store\");");
                printStream.print(toStringStringBuilder);
                printStream.println("        sb.append(\"\\n\");");
                printStream.println("        return sb;");
                printStream.println("    }");
                printStream.println("}");
                printStream.close();

            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }

        super.endElement(uri, localName, qName);
    }

    public static void main(String[] args) {
        GeneratorStores generator = new GeneratorStores(PACKAGE);
        generator.parseArgs(args);
        generator.run();
    }
}
