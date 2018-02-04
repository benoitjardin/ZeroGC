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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Benoit Jardin
 *
 * Class for parsing XSD schemas
 */
public abstract class GeneratorBase extends DefaultHandler {
    protected String pkg;
    protected String prefix = "";
    protected String xsdPath;
    protected String destPath;
    protected String generatedDir;
    protected String comment;
    protected static final String OUTBOUND = "Request";
    protected static final String INBOUND = "";

    // Cache for custom types
    protected Hashtable<String, Type> simpleTypeHashtable;
    // Mapping between table names and pair of columnName, columnTypeName
    protected Hashtable<String, ArrayList<Pair<String, String>>> complexTypeHashtable;

    // List of pairs of columnName, columnTypeName
    protected ArrayList<Pair<String, String>> tableColumns = new ArrayList<Pair<String, String>>();
    protected ArrayList<String> index = new ArrayList<String>();
    private Stack<String> stack = new Stack<String>();
    protected String simpleTypeName = null;
    protected String complexTypeName = null;

    protected Type type = null;

    protected String characters = "";
    protected String enumAppInfoText = "";
    protected String enumAppInfoDirection = null;
    protected String enumAppInfoDirectionText = "";
    protected String enumValue = "";
    protected String enumDocumentation = "";

    // List of messages organized by Message and Major
    protected Map<String, List<MessageMetaData>> inboundMessages;
    protected Map<String, List<MessageMetaData>> outboundMessages;

    protected String location = "";

    final protected StringBuilder sb = new StringBuilder();

    static class Type {
        String javaName;
        String xsdTypeName;
        int maxLength;
        int fractionDigits = 0;
        int totalDigits = 0;
        int alignment = 1;

        static Type create(String xsdType) {
            Type type = null;
            if (xsdType.equals("xs:string")) {
                type = new Type("String", "xs:string", 0, 1);
            } else if (xsdType.equals("xs:byte")) {
                type = new Type("byte", "xs:byte", 1, 1);
            } else if (xsdType.equals("xs:short")) {
                type = new Type("short", "xs:short", 2, 1);
            } else if (xsdType.equals("xs:int")) {
                type = new Type("int", "xs:int", 4, 1);
            } else if (xsdType.equals("xs:long")) {
                type = new Type("long", "xs:long", 8, 1);
            } else if (xsdType.equals("xs:decimal")) {
                type = new Type("long", "xs:decimal", 8, 1);
            } else {
                System.out.println("Unknown type: " + xsdType);
            }
            return type;
        }
        Type(String javaName, String xsdTypeName, int maxLength, int alignment) {
            this.javaName = javaName;
            this.xsdTypeName = xsdTypeName;
            this.maxLength = maxLength;
            this.alignment = alignment;
        }

        public boolean isString() {
            return javaName.equals("String") ||
                    (xsdTypeName.equals("xs:string") && !javaName.equals("byte"));
        }

        public boolean isChar() {
            return xsdTypeName.equals("xs:string") && javaName.equals("byte");
        }
    }

    public static class Pair <First, Second> {
        private final First first;
        private final Second second;

        public Pair(First first, Second second) {
            this.first = first;
            this.second = second;
        }

        public First getFirst() {
            return this.first;
        }

        public Second getSecond() {
            return this.second;
        }
    }

    public class MessageMetaData{

        private final String major;

        public MessageMetaData(String major) {
            this.major = major;
        }

        public MessageMetaData(String major, boolean b) {
            this.major = major;
        }

        public String getMajor() {
            return major;
        }
    }

    protected GeneratorBase(String pkg) {
        this.pkg = pkg;
        this.inboundMessages = new TreeMap<String, List<MessageMetaData>>();
        this.outboundMessages = new TreeMap<String, List<MessageMetaData>>();
    }

    protected GeneratorBase(GeneratorBase generator) {
        this.pkg = generator.pkg;
        this.prefix = generator.prefix;
        this.comment = generator.comment;
        this.inboundMessages = new TreeMap<String, List<MessageMetaData>>();
        this.outboundMessages = new TreeMap<String, List<MessageMetaData>>();
    }

    protected abstract GeneratorBase cloneGenerator();

    protected String getPackage() {
        return this.pkg;
    }

    public void initialize(String xsdPath, String generatedDir)
    {
        // Make sure to generate files with Windows style CRLF
        //System.setProperty("line.separator", "\r\n");
        // Make sure to generate files with Unix style LF
        System.setProperty("line.separator", "\n");

        this.comment =
                "/*\n" +
                " * PLEASE DO NOT EDIT!\n" +
                " * \n" +
                " * This code has been automatically generated.\n" +
                " * Generator: " + this.getClass().getName() + "\n" +
                " * Schema: " + xsdPath + "\n" +
                " */\n";
        this.xsdPath = xsdPath;
        this.generatedDir = generatedDir;
        this.simpleTypeHashtable = new Hashtable<String, Type>();
        this.complexTypeHashtable = new Hashtable<String, ArrayList<Pair<String, String>>>();
    }

    public void initialize(String xsdPath, GeneratorBase generator)
    {
        this.xsdPath = xsdPath;
        this.generatedDir = generator.generatedDir;
        this.simpleTypeHashtable = generator.simpleTypeHashtable;
        this.complexTypeHashtable = generator.complexTypeHashtable;
    }

    protected String pascal(String str) {
        sb.setLength(0);
        for (int i=0; i< str.length(); i++) {
            if (i == 0) {
                sb.append(Character.toUpperCase(str.charAt(i)));
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    protected String camel(String str) {
        sb.setLength(0);
        for (int i=0; i< str.length(); i++) {
            if (i == 0) {
                sb.append(Character.toLowerCase(str.charAt(i)));
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        super.startElement(uri, localName, qName, attributes);
        String tagName = qName;
        stack.push(location);
        location += "/" + tagName;
        characters = "";

        if (location.equals("/xs:schema/xs:simpleType")) {
            // Column type definition
            simpleTypeName = attributes.getValue("name");
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction")) {
            String xsdBaseType = attributes.getValue("base");
            type = simpleTypeHashtable.get(xsdBaseType);
            if (type == null) {
                type = Type.create(xsdBaseType);
                // Don't cache XSD native types in simpleTypeHashtable
            }
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:enumeration")) {
            enumValue = attributes.getValue("value");
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:enumeration/xs:annotation/xs:appinfo")) {
            enumAppInfoDirection = attributes.getValue("dc:direction");
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:enumeration/xs:annotation/xs:documentation")) {
            enumDocumentation = "";
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:fractionDigits")) {
            type.fractionDigits = Integer.parseInt(attributes.getValue("value"));
        } else if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:maxLength")) {
            type.maxLength = Integer.parseInt(attributes.getValue("value"));
            if (type.javaName.equals("String")) {
                if (type.maxLength == 1) {
                    type = new Type("byte", type.xsdTypeName, type.maxLength, 1);
                } else if (type.maxLength == 2) {
                    type = new Type("short", type.xsdTypeName, type.maxLength, 1);
                } else if (type.maxLength == 4) {
                    type = new Type("int", type.xsdTypeName, type.maxLength, 1);
                } else if (type.maxLength == 8) {
                    type = new Type("long", type.xsdTypeName, type.maxLength, 1);
                }
            }
        } else if (location.equals("/xs:schema/xs:include")) {
            String schemaLocation = attributes.getValue("schemaLocation");
            try {
                XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                String includeXsd = new File(xsdPath).getParent() + "/" + schemaLocation;
                System.out.println("Include schema file: " + includeXsd);
                GeneratorBase generator = cloneGenerator();
                generator.initialize(includeXsd, this);
                xmlReader.setContentHandler(generator);
                xmlReader.parse(includeXsd);
            } catch (SAXException e) {
                System.out.println("XML parse error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        } else if (location.equals("/xs:schema/xs:element") ||
                location.equals("/xs:schema/xs:complexType")) {
            complexTypeName = attributes.getValue("name");

            tableColumns.clear();
        } else if (location.endsWith("/xs:complexType/xs:complexContent/xs:extension")) {
            String base = attributes.getValue("base");
            for (Pair<String, String> column : complexTypeHashtable.get(base)) {
                tableColumns.add(column);
            }
        } else if (location.endsWith("/xs:complexType/xs:complexContent/xs:extension/xs:attribute") ||
                location.endsWith("/xs:complexType/xs:attribute")) {
            String columnName = attributes.getValue("name");
            String columnTypeName = attributes.getValue("type");
            Type columnType = simpleTypeHashtable.get(columnTypeName);
            if (columnType == null) {
                columnType = Type.create(columnTypeName);
                // Don't cache XSD native types in simpleTypeHashtable
            }
            tableColumns.add(new Pair<String, String>(columnName, columnTypeName));
        } else if (location.equals("/xs:schema/xs:element/xs:unique")) {
            //String keyName = attributes.getValue("name");
            //System.out.println("Primary Key: " + keyName);
            index.clear();
        } else if (location.equals("/xs:schema/xs:element/xs:unique/xs:field")) {
            // PRIMARY KEY
            String keyColumnName = attributes.getValue("xpath").substring(1);
            //System.out.println("PrimaryKeyColumnName: " + keyColumnName);
            index.add(keyColumnName);
        } else if (location.equals("/xs:schema/xs:element/xs:key")) {
            //String keyName = attributes.getValue("name");
            //System.out.println("Key: " + keyName);
            index.clear();
        } else if (location.equals("/xs:schema/xs:element/xs:key/xs:field")) {
            // PRIMARY KEY
            String keyColumnName = attributes.getValue("xpath").substring(1);
            //System.out.println("KeyColumnName: " + keyColumnName);
            index.add(keyColumnName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        super.characters(ch, start, length);
        characters += new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (location.equals("/xs:schema/xs:simpleType/xs:restriction/xs:enumeration/xs:annotation/xs:appinfo")) {
            if (enumAppInfoDirection != null) {
                enumAppInfoDirectionText = characters.trim();
                if (enumAppInfoDirection.equals("Inbound")) {
                    if (!inboundMessages.containsKey(enumAppInfoDirectionText)) {
                        inboundMessages.put(enumAppInfoDirectionText, new ArrayList<MessageMetaData>());
                    }
                    inboundMessages.get(enumAppInfoDirectionText).add(new MessageMetaData(enumAppInfoText));
                } else if (enumAppInfoDirection.equals("Outbound")) {
                    if (!outboundMessages.containsKey(enumAppInfoDirectionText)) {
                        outboundMessages.put(enumAppInfoDirectionText, new ArrayList<MessageMetaData>());
                    }
                    outboundMessages.get(enumAppInfoDirectionText).add(new MessageMetaData(enumAppInfoText));
                }
            } else {
                enumAppInfoText = characters.trim();
            }
        }
        //String tagName = xmlPullParser.getName();
        if (location.equals("/xs:schema/xs:simpleType")) {
            //System.out.println(typeName + ": " + typeDesc);
            // Cache custom types
            simpleTypeHashtable.put(simpleTypeName, type);
        } else if (location.equals("/xs:schema/xs:element") ||
                location.equals("/xs:schema/xs:complexType")) {
            complexTypeHashtable.put(complexTypeName, tableColumns);
            tableColumns = new ArrayList<Pair<String, String>>();
        }
        location = stack.pop();

        super.endElement(uri, localName, qName);
    }

    public void run() {
        inboundMessages.clear();
        outboundMessages.clear();
        try {
            File genDir = new File(destPath, getPackage().replace('.', '/')).getCanonicalFile();
            if (!genDir.exists()) {
                if (!genDir.mkdirs()) {
                    throw new IOException("Failed to create destination directory: " + genDir);
                }
                System.out.println("Created directory: " + genDir.getAbsolutePath());
            }
            this.generatedDir = genDir.getCanonicalPath();

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();

            System.out.println(String.format("%1$s -dest %2$s", GeneratorBase.class.getSimpleName(), destPath)); ;
            System.out.println("Parse schema file: " + xsdPath);
            System.out.println("Current directory: " + new File(".").getCanonicalPath());

            initialize(xsdPath, generatedDir);
            xmlReader.setContentHandler(this);
            xmlReader.parse(xsdPath);
        } catch (SAXException e) {
            System.out.println("XML parse error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    protected void parseArgs(String[] args){
        // Parse command line arguments
        boolean error = true;
        int i= 0;
        while (i < args.length && args[i].startsWith("-")) {
            String arg = args[i++].toLowerCase();
            if (arg.equals("-dest")) {
                error = false;
                if (i < args.length) {
                    destPath = args[i++];
                    System.err.println("Generate source under: " + destPath);
                } else {
                    System.err.println("-dest requires a dir path");
                    error = true;
                    break;
                }
            } else if (arg.equals("-xsd")) {
                error = false;
                if (i < args.length) {
                    xsdPath = args[i++];
                    System.err.println("Using xsd path: " + xsdPath);
                } else {
                    System.err.println("-xsd requires a file path");
                    error = true;
                    break;
                }
            } else if (arg.equals("-pkg")) {
                error = false;
                if (i < args.length) {
                    pkg = args[i++];
                    System.err.println("Using pkg: " + pkg);
                } else {
                    System.err.println("-pkg requires a package name");
                    error = true;
                    break;
                }
            } else if (arg.equals("-prefix")) {
                error = false;
                if (i < args.length) {
                    this.prefix = args[i++];
                    System.err.println("Using prefix: " + prefix);
                } else {
                    System.err.println("-prefix requires a prefix");
                    error = true;
                    break;
                }
            } else {
                error = true;
                break;
            }
        }
        if (error || destPath == null) {
            System.err.println("Current directory: " + new File(".").getAbsolutePath());
            System.err.println(String.format("usage: %1$s -dest <path> -xsd <path> -prefix <prefix> -pkg <pkg>", new Object[] { GeneratorBase.class.getSimpleName() }));
            System.err.println(" -dest <path>: destination directory");
            System.err.println(" -xsd <path>: source file path");
            System.err.println(" -prefix <prefix>: prefix");
            System.err.println(" -pkg <pkg>: package name");
            System.exit(-1);
        }
    }
}
