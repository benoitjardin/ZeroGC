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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * @author Benoit Jardin
 */
public class GeneratorConstants extends GeneratorBase {

    static final String PACKAGE = "com.zerogc.generated.constants";

    protected StringBuilder enumFormat = new StringBuilder();

    GeneratorConstants(String pkg) {
        super(pkg);
    }

    protected GeneratorConstants(GeneratorConstants generator) {
        super(generator);
    }

    @Override
    protected GeneratorBase cloneGenerator() {
        return new GeneratorConstants(this);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        super.startElement(uri, localName, qName, attributes);
        if (location.equals("/xs:schema/xs:simpleType")) {
            enumFormat.setLength(0);
        }
    }


    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        //String tagName = xmlPullParser.getName();
        if (location.equals("/xs:schema/xs:simpleType")) {
            if (enumFormat.length() > 0) {
                // System.out.println("Enum: " + typeName);
                try {
                    System.out.println("Generate: " + pascal(prefix) + pascal(simpleTypeName) + ".java");
                    PrintStream enumPrintStream = new PrintStream(generatedDir +"/" + pascal(prefix) + pascal(simpleTypeName) + ".java");
                    enumPrintStream.println(comment);
                    enumPrintStream.println();
                    enumPrintStream.println("package " + getPackage() + ";");
                    enumPrintStream.println();
                    enumPrintStream.println("public class " + pascal(prefix)+pascal(simpleTypeName) + " {");
                    enumPrintStream.print(enumFormat);
                    enumPrintStream.println("}");
                    enumPrintStream.close();
                } catch (IOException e) {
                    System.err.println("IOException: " + e.getMessage());
                }
            }
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:enumeration/xs:annotation/xs:appinfo") &&
                enumAppInfoDirection == null) {
            String value = enumValue;
            if (type.isString()) {
                value = '"' + value + '"';
            } else if (type.isChar()) {
                value = "'" + value + "'";
            }
            enumFormat.append("    public static final " + type.javaName + " " + characters.trim() + " = " + value + ";\n");
        }
        super.endElement(uri, localName, qName);
    }

    public static void main(String[] args) {
        GeneratorConstants generator = new GeneratorConstants(PACKAGE);
        generator.parseArgs(args);
        generator.run();
    }
}
