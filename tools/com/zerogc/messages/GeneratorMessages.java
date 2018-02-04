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
public class GeneratorMessages extends GeneratorBase {
    static final String PACKAGE = "com.zerogc.generated.messages";

    protected GeneratorMessages(String pkg) {
        super(pkg);
    }

    protected GeneratorMessages(GeneratorMessages generator) {
        super(generator);
    }

    @Override
    protected GeneratorBase cloneGenerator() {
        return new GeneratorMessages(this);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (location.equals("/xs:schema/xs:element")) {
            try {
                if (inboundMessages.containsKey(complexTypeName)) {
                    generateMessage(complexTypeName + INBOUND);
                }
                if (outboundMessages.containsKey(complexTypeName)) {
                    generateMessage(complexTypeName + OUTBOUND);
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
        super.endElement(uri, localName, qName);
    }

    public void generateMessage(String messageName) throws IOException {

        StringBuilder createStringBuilder = new StringBuilder();
        String createParams = "";
        StringBuilder gettersAndSetterStringBuilder = new StringBuilder();
        StringBuilder offsetStringBuilder = new StringBuilder();
        StringBuilder lengthStringBuilder = new StringBuilder();
        StringBuilder toStringStringBuilder = new StringBuilder();

        int offset = 0;
        String indent = "        ";

        for (Pair<String, String> column : tableColumns) {
            String columnName = column.getFirst();
            String columnTypeName = column.getSecond();
            Type columnType = simpleTypeHashtable.get(columnTypeName);

            if (columnType == null) {
                columnType = Type.create(columnTypeName);
            }

            // Fix alignement if necessary
            if (offset%columnType.alignment > 0) {
                offset += columnType.alignment - offset%columnType.alignment;
            }

            offsetStringBuilder.append("        public static final short " + columnName + " = " + offset + ";\n");
            lengthStringBuilder.append("        public static final short " + columnName + " = " + columnType.maxLength + ";\n");

            String baseOffset = "this.baseOffset";

            String toString = null;
            String getString = null; // Get as native type
            String setString = null; // Set as native type
            if (columnType.javaName.equals("byte")
                    || columnType.javaName.equals("short")
                    || columnType.javaName.equals("int")
                    || columnType.javaName.equals("long")) {
                toString = indent + "sb.append(\"[" + columnName + "=\").append(get" + pascal(columnName) + "()).append(']');\n";
                if (columnType.maxLength == 1) {
                    if (columnType.javaName.equals("byte")) {
                        getString = "this.buffer.get(" + baseOffset + " + Offsets." + columnName + ")";
                        setString = "this.buffer.put(" + baseOffset + " + Offsets." + columnName + ", " + columnName + ")";
                    } else {
                        // Same but cast value to a byte
                        getString = "this.buffer.get(" + baseOffset + " + Offsets." + columnName + ")&0xFF";
                        setString = "this.buffer.put(" + baseOffset + " + Offsets." + columnName + ", (byte)" + columnName + ")";
                    }
                } else if (columnType.maxLength == 2) {
                    getString = "this.buffer.getShort(" + baseOffset + " + Offsets." + columnName + ")";
                    setString = "this.buffer.putShort(" + baseOffset + " + Offsets." + columnName + ", " + columnName + ")";
                } else if (columnType.maxLength == 4) {
                    getString = "this.buffer.getInt(" + baseOffset + " + Offsets." + columnName + ")";
                    setString = "this.buffer.putInt(" + baseOffset + " + Offsets." + columnName + ", " + columnName + ")";
                } else if (columnType.maxLength == 8) {
                    getString = "this.buffer.getLong(" + baseOffset + " + Offsets." + columnName + ")";
                    setString = "this.buffer.putLong(" + baseOffset + " + Offsets." + columnName + ", " + columnName + ")";
                }
            }
            gettersAndSetterStringBuilder.append("\n");
            if (columnType.isString()) {
                if (columnType.maxLength == 0) {
                    // Special case to handle the last field with variable length
                    // ex: Details on SubscriptionRequest
                    gettersAndSetterStringBuilder.append("    public int get" + pascal(columnName) + "Offset() {\n");
                    gettersAndSetterStringBuilder.append("        return " + baseOffset + " + Offsets." + columnName + ";\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(byte[] buffer) {\n");
                    gettersAndSetterStringBuilder.append("        this.buffer.limit(" + baseOffset + " + Offsets." + columnName + " + buffer.length);\n");
                    gettersAndSetterStringBuilder.append("        ByteUtils.copy(buffer, 0, this.buffer, " + baseOffset + " + Offsets." + columnName + ", buffer.length);\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    if (createParams.length() > 0) {
                        createParams += ", ";
                    }
                    createParams += "byte[] " + columnName;
                    createStringBuilder.append("        ByteUtils.copy(" + columnName + ", 0, this.buffer, " + baseOffset + " + Offsets." + columnName + ", " + columnName + ".length);\n");
                    createStringBuilder.append("        this.buffer.limit(" + baseOffset + " + Offsets." + columnName + " + " + columnName + ".length);\n");
                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(byte[] buffer, int offset, int length) {\n");
                    gettersAndSetterStringBuilder.append("        this.buffer.limit(" + baseOffset + " + Offsets." + columnName + " + length);\n");
                    gettersAndSetterStringBuilder.append("        ByteUtils.copy(buffer, offset, this.buffer, " + baseOffset + " + Offsets." + columnName + ", length);\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(ByteSlice byteSlice) {\n");
                    gettersAndSetterStringBuilder.append("        this.buffer.limit(" + baseOffset + " + Offsets." + columnName + " + byteSlice.getLength());\n");
                    gettersAndSetterStringBuilder.append("        ByteUtils.copy(byteSlice.getBuffer(), byteSlice.getOffset(), this.buffer, " + baseOffset + " + Offsets." + columnName + ", byteSlice.getLength());\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    /*
                        gettersAndSetterStringBuilder.append("    public ByteSlice get" + pascal(columnName) + "(ByteSlice byteSlice) {\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setBuffer(this.buffer.array());\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setOffset(" + baseOffset + " + Offsets." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setLength(this.buffer.limit() - " + baseOffset + " - Offsets." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        return byteSlice;\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                     */
                } else {
                    gettersAndSetterStringBuilder.append("    public int get" + pascal(columnName) + "Offset() {\n");
                    gettersAndSetterStringBuilder.append("        return " + baseOffset + " + Offsets." + columnName + ";\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    gettersAndSetterStringBuilder.append("    public int get" + pascal(columnName) + "Length() {\n");
                    gettersAndSetterStringBuilder.append("        return Lengths." + columnName + ";\n");
                    gettersAndSetterStringBuilder.append("    }\n");

                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(ByteSlice byteSlice) {\n");
                    gettersAndSetterStringBuilder.append("        ByteUtils.copySpacePadded(byteSlice.getBuffer(), byteSlice.getOffset(), byteSlice.getLength(), this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ");\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    /*
                        gettersAndSetterStringBuilder.append("    public ByteSlice get" + pascal(columnName) + "(ByteSlice byteSlice) {\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setBuffer(this.buffer.array());\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setOffset(" + baseOffset + " + Offsets." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setLength(Lengths." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        return byteSlice;\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                     */
                    /*
                        gettersAndSetterStringBuilder.append("    public ByteSlice get" + pascal(columnName) + "Trimmed(ByteSlice byteSlice) {\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setBuffer(this.buffer.array());\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setOffset(" + baseOffset + " + Offsets." + columnName + ");\n");
                        gettersAndSetterStringBuilder.append("        byteSlice.setLength(ByteUtils.getSpaceTrimmedLength(this.buffer.array(), this.baseOffset + Offsets." + columnName + ", Lengths." + columnName + "));\n");
                        gettersAndSetterStringBuilder.append("        return byteSlice;\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                     */
                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(byte[] buffer) {\n");
                    gettersAndSetterStringBuilder.append("        ByteUtils.copySpacePadded(buffer, 0, buffer.length, this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ");\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    gettersAndSetterStringBuilder.append("    public int get" + pascal(columnName) + "(byte[] buffer) {\n");
                    gettersAndSetterStringBuilder.append("        return ByteUtils.copySpaceTrimmed(this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ", buffer, 0);\n");
                    gettersAndSetterStringBuilder.append("    }\n");

                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(byte[] buffer, int offset, int length) {\n");
                    gettersAndSetterStringBuilder.append("        ByteUtils.copySpacePadded(buffer, offset, length, this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ");\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    if (createParams.length() > 0) {
                        createParams += ", ";
                    }
                    createParams += "byte[] " + columnName;
                    createStringBuilder.append("        ByteUtils.copySpacePadded(" + columnName + ", 0, " + columnName + ".length, this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ");\n");
                    gettersAndSetterStringBuilder.append("    public int get" + pascal(columnName) + "(byte[] buffer, int offset) {\n");
                    gettersAndSetterStringBuilder.append("        return ByteUtils.copySpaceTrimmed(this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ", buffer, offset);\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                }
                if (getString != null) {
                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "As" + pascal(columnType.javaName) + "(" + columnType.javaName + " " + columnName + ") {\n");
                    gettersAndSetterStringBuilder.append("        " + setString + ";\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    gettersAndSetterStringBuilder.append("    public " + columnType.javaName + " get" + pascal(columnName) + "As" + pascal(columnType.javaName) + "() {\n");
                    gettersAndSetterStringBuilder.append("        return " + getString + ";\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                }
                toString =  indent + "sb.append(\"[" + columnName + "=\").append(this.buffer, " + baseOffset + " + Offsets." + columnName + ", Lengths." + columnName + ").append(']');\n";

            } else {
                gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "(" + columnType.javaName + " " + columnName + ") {\n");
                if (setString != null) {
                    gettersAndSetterStringBuilder.append("        " + setString + ";\n");

                    if (createParams.length() > 0) {
                        createParams += ", ";
                    }
                    createParams += columnType.javaName + " " + columnName;
                    createStringBuilder.append("        " + setString + ";\n");
                }
                gettersAndSetterStringBuilder.append("    }\n");
                gettersAndSetterStringBuilder.append("    public " + columnType.javaName + " get" + pascal(columnName) + "() {\n");
                if (getString != null) {
                    gettersAndSetterStringBuilder.append("        return " + getString + ";\n");
                } else {
                    gettersAndSetterStringBuilder.append("        return Long.MIN_VALUE;\n");
                }
                gettersAndSetterStringBuilder.append("    }\n");

                if (columnType.fractionDigits > 0) {
                    // Double
                    gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "AsDouble(double doubleValue) {\n");
                    gettersAndSetterStringBuilder.append("        " + columnType.javaName + " " + columnName + " = (" + columnType.javaName + ")((doubleValue*1");
                    for (int i=0; i<columnType.fractionDigits; i++) {
                        gettersAndSetterStringBuilder.append('0');
                    }
                    gettersAndSetterStringBuilder.append(".0) + 0.5);\n");
                    gettersAndSetterStringBuilder.append("        set" + pascal(columnName) + "(" + columnName + ");\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    gettersAndSetterStringBuilder.append("    public double get" + pascal(columnName) + "AsDouble() {\n");
                    gettersAndSetterStringBuilder.append("        " + columnType.javaName + " " + columnName + " = get" + pascal(columnName) + "();\n");
                    gettersAndSetterStringBuilder.append("        return " + columnName + "/1");
                    for (int i=0; i<columnType.fractionDigits; i++) {
                        gettersAndSetterStringBuilder.append('0');
                    }
                    gettersAndSetterStringBuilder.append(".0;\n");
                    gettersAndSetterStringBuilder.append("    }\n");
                    /*
                        //BigDecimal
                        gettersAndSetterStringBuilder.append("    public void set" + pascal(columnName) + "AsBigDecimal(BigDecimal bigDecimalValue) {\n");
                        gettersAndSetterStringBuilder.append("        " + columnType.javaName + " " + columnName + " = bigDecimalValue.movePointRight(" + columnType.fractionDigits + ").setScale(0, RoundingMode.HALF_UP)." + columnType.javaName + "Value();\n");
                        gettersAndSetterStringBuilder.append("        " + setString + ";\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                        gettersAndSetterStringBuilder.append("    public BigDecimal get" + pascal(columnName) + "AsBigDecimal() {\n");
                        gettersAndSetterStringBuilder.append("        " + columnType.javaName + " " + columnName + " = " + getString + ";\n");
                        gettersAndSetterStringBuilder.append("        return BigDecimal.valueOf(" + columnName + ").movePointLeft(" + columnType.fractionDigits + ");\n");
                        gettersAndSetterStringBuilder.append("    }\n");
                     */
                    toString =  indent + "sb.append(\"[" + columnName + "=\").appendFormatted(get" + pascal(columnName) + "(), " + columnType.fractionDigits + ", 0).append(']');\n";
                }
            }

            offset += columnType.maxLength;

            toStringStringBuilder.append(toString);

            if (columnName.equals("repeatingGroup")) {
                toStringStringBuilder.append("        for (int groupIndex=0; groupIndex < getRepeatingGroup(); ++groupIndex) {\n");
                toStringStringBuilder.append("            sb.append('\\n');\n");
                indent += "    ";
            }
        }

        System.out.println("Generate: " + pascal(prefix) + pascal(messageName) + ".java");
        PrintStream printStream = new PrintStream(generatedDir + "/" + pascal(prefix) + pascal(messageName) + ".java");

        printStream.println(comment);
        printStream.println();
        printStream.println("package " + getPackage() + ";");
        printStream.println();
        printStream.println();

        printStream.println("import java.nio.ByteBuffer;");
        printStream.println("import com.zerogc.core.ByteSlice;");
        printStream.println("import com.zerogc.core.ByteStringBuilder;");
        printStream.println("import com.zerogc.core.ByteUtils;");
        printStream.println("import com.zerogc.core.ToByteString;");
        printStream.println();

        printStream.println("public class " + pascal(prefix)+pascal(messageName) + " implements ToByteString {");

        printStream.println("    public static class Lengths {");
        printStream.print(lengthStringBuilder);
        printStream.println("    }");
        printStream.println();
        printStream.println("    public static class Offsets {");
        printStream.print(offsetStringBuilder);
        printStream.println("    }");
        printStream.println("    public static final short endOfAttributes = " + offset + ";");
        printStream.println();

        printStream.println("    private ByteBuffer buffer;");
        printStream.println("    private int baseOffset;");
        printStream.println("    private final ByteSlice byteSlice = new ByteSlice();");
        printStream.println();
        printStream.println("    public " + pascal(prefix)+pascal(messageName) + "() {");
        printStream.println("    }");
        printStream.println();

        printStream.println("    public void setBuffer(ByteBuffer buffer) {");
        printStream.println("        this.buffer = buffer;");
        printStream.println("        this.baseOffset = buffer.position();");
        printStream.println("        buffer.position(this.baseOffset + endOfAttributes);");
        printStream.println("    }");
        printStream.println();
        printStream.println("    public ByteBuffer getBuffer() {");
        printStream.println("        return this.buffer;");
        printStream.println("    }");
        printStream.println();
        printStream.println("    public int getBaseOffset() {");
        printStream.println("        return this.baseOffset;");
        printStream.println("    }");

        printStream.println();
        printStream.println("    public void create(" + createParams + ") {");
        printStream.print(createStringBuilder);
        printStream.println("    }");

        printStream.print(gettersAndSetterStringBuilder);

        printStream.println();
        printStream.println("    @Override");
        printStream.println("    public ByteStringBuilder toByteString(ByteStringBuilder sb) {");
        printStream.println("        sb.append(\"" + pascal(messageName) + "\");");
        printStream.print(toStringStringBuilder);
        printStream.println("        return sb;");
        printStream.println("    }");
        printStream.println();
        printStream.println("    @Override");
        printStream.println("    public String toString() {");
        printStream.println("        return toByteString(new ByteStringBuilder()).toString();");
        printStream.println("    }");
        printStream.println("}");
        printStream.close();
    }

    public static void main(String[] args) {
        GeneratorMessages generator = new GeneratorMessages(PACKAGE);
        generator.parseArgs(args);
        generator.run();
    }
}
