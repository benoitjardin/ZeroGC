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
package com.zerogc.collections;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author Benoit Jardin
 */

public class Generator {
    private static final String KEY_NAME = "_KeyName_";
    private static final String VALUE_NAME = "_ValueName_";
    private static final String KEY_TYPE = "_KeyType_";
    private static final String VALUE_TYPE = "_ValueType_";

    public static String[][] map = new String[][] {
        //{"_KeyName_", "_KeyType_", "_ValueName_", "_ValueType_" },
        {"ByteSlice",  "ByteSlice",  "Int", "int" },
        {"Int",  "int",  "Int", "int" },
        {"Int",  "int",  "Long", "long" },
        {"Int",  "int",  "Object", "Object" },
        {"Long", "long", "Int", "int" },
        {"Long", "long", "Long", "long" },
        {"Long", "long", "Object", "Object" },
        {"Double", "double", "Object", "Object" },
        {"Object", "Object", "Object", "Object" },
        //{"Long", "long", "ByteArray", "byte[]" },
    };

    public static void main(String[] args) {
        System.out.println("Hello world!");

        // Make sure to generate files with Windows style CRLF
        //System.setProperty("line.separator", "\r\n");
        // Make sure to generate files with Unix style LF
        System.setProperty("line.separator", "\n");

        File dir = new File(args[0]);
        System.out.println("Templates: " + dir.getAbsolutePath());
        File dest = new File(args[1]);
        System.out.println("Destination: " + dest.getAbsolutePath());

        StringBuilder sb = new StringBuilder();

        ScriptEngineManager manager = new ScriptEngineManager();
        for (ScriptEngineFactory factory : manager.getEngineFactories()) {
             System.out.println("Available language: " + factory.getLanguageName());
        }
        ScriptEngine engine = manager.getEngineByName("javascript");
        if (engine != null) {
            System.out.println("Instanciated: " + engine.toString());
        }

        //Pattern pattern = Pattern.compile("^//#(\\w+)(\\s*\"(\\w+)\"\\s*([!=]=)\\s*\"(\\w+)\")?");
        Pattern pattern = Pattern.compile("^//#(\\w+)\\s*(.*)");

        for (String[] mapping : map) {
            String keyName = mapping[0];
            String keyType = mapping[1];
            String valueName = mapping[2];
            String valueType = mapping[3];

            for (File file : dir.listFiles()) {
                String name = file.getName();
                String newKeyName = name.replaceAll(KEY_NAME, keyName);
                String newValueName = newKeyName.replaceAll(VALUE_NAME, valueName);
                boolean hasKeyName = !newKeyName.equals(name);
                boolean hasValueName = !newValueName.equals(newKeyName);
                if (!hasKeyName && !hasValueName) {
                    continue;
                }
                System.out.println(newValueName);
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dest, newValueName)));
                    String line;
                    int count = 0;
                    boolean copy = true;
                    while ((line = reader.readLine()) != null) {
                        count++;
                        if (hasKeyName) {
                            line = line.replaceAll(KEY_NAME, keyName);
                            line = line.replaceAll(KEY_TYPE, keyType);
                        }
                        if (hasValueName) {
                            line = line.replaceAll(VALUE_NAME, valueName);
                            line = line.replaceAll(VALUE_TYPE, valueType);
                        }

                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            if (matcher.group(1).equals("if") || matcher.group(1).equals("elsif")) {
// Simple Regexp alternative to JavaScriptEngine expression
//                                if (matcher.groupCount() == 5) {
//                                    copy = matcher.group(3).equals(matcher.group(5)) ^ !matcher.group(4).equals("==");
                                if (matcher.groupCount() == 2) {
                                    try {
                                        copy = (Boolean)engine.eval(matcher.group(2));
                                    } catch (ScriptException e) {
                                        System.err.println("Error while processing file: " + name);
                                        e.printStackTrace();
                                    }
                                } else {
                                    System.err.println("Line " + count + ": Preprocessor error: " + line);
                                    throw new RuntimeException("Line " + count + ": Preprocessor error: " + line);
                                }
                            } else if (matcher.group(1).equals("else")) {
                                copy = !copy;
                            } else if (matcher.group(1).equals("endif")) {
                                copy = true;
                            }
                            continue;
                        }

                        if (copy) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                    reader.close();
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error while processing file: " + name);
                    e.printStackTrace();
                }
            }
        }
    }
}
