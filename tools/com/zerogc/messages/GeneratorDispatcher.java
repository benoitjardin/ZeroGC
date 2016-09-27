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
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

/**
 * @author Benoit Jardin
 */
public class GeneratorDispatcher extends GeneratorBase {
    static final String PACKAGE = "com.zerogc.dispatchers";

    private PrintStream dispatcherPrintStream = null;
    
    GeneratorDispatcher(String pkg) {
        super(pkg);
    }
    
    protected GeneratorDispatcher(GeneratorDispatcher generator) {
        super(generator);
    }
    
    @Override
    protected GeneratorBase cloneGenerator() {
        return new GeneratorDispatcher(this);
    }
    
    @Override
    public void startDocument() throws SAXException
    {
        super.startDocument();
    }
    
    @Override
    public void run()
    {
        super.run();

        try {
            generateDispatcher(INBOUND, inboundMessages);
            generateDispatcher(OUTBOUND, outboundMessages);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
    
    public void generateDispatcher(String direction, Map<String, List<MessageMetaData>> messages) throws IOException {
        String dispatcherName = direction + "Dispatcher";
        String listenerName = direction + "Listener";
        String handlerName = direction + "Handler";

        System.out.println("Generate: " + dispatcherName + ".java");
    	dispatcherPrintStream = new PrintStream(generatedDir +"/" + pascal(prefix) + dispatcherName  + ".java");

        dispatcherPrintStream.println(this.comment);
        dispatcherPrintStream.println();
        dispatcherPrintStream.println("package " + getPackage() + ";");
        dispatcherPrintStream.println("import java.nio.ByteBuffer;");
        dispatcherPrintStream.println();
        dispatcherPrintStream.println("import com.zerogc.constants." + pascal(prefix) + "Type;");
        dispatcherPrintStream.println("import com.zerogc.messages.*;");
        dispatcherPrintStream.println("import com.zerogc.tools.ClientMessageListener;");
        dispatcherPrintStream.println("import com.zerogc.util.ByteStringBuilder;");
        dispatcherPrintStream.println("import com.zerogc.util.Level;");
        dispatcherPrintStream.println("import com.zerogc.util.Logger;");
        dispatcherPrintStream.println();
        dispatcherPrintStream.println("public class " + pascal(prefix) + dispatcherName + " implements ClientMessageListener {");
        dispatcherPrintStream.println();
        dispatcherPrintStream.println("    private final Logger log = new Logger(\"" + pascal(prefix) + dispatcherName + "\");");
        dispatcherPrintStream.println("    private final " + pascal(prefix) + listenerName + " messageListener;");
        dispatcherPrintStream.println();
        for (Map.Entry<String, List<MessageMetaData>> mapEntry : messages.entrySet()) {
            String message = mapEntry.getKey() + direction;
            dispatcherPrintStream.println("    private final " + pascal(prefix) + pascal(message) + " " + camel(message) + " = new " + pascal(prefix) + pascal(message) + "();");
            
        }
        dispatcherPrintStream.println();
        dispatcherPrintStream.println("    public " + pascal(prefix) + dispatcherName + "(" + pascal(prefix) + listenerName + " messageListener)");
        dispatcherPrintStream.println("    {");
        dispatcherPrintStream.println("        this.messageListener = messageListener;");
        dispatcherPrintStream.println("    }");
        dispatcherPrintStream.println();
        dispatcherPrintStream.println("    //@Override");
        dispatcherPrintStream.println("    public void onClientMessage(ByteBuffer buffer)");
        dispatcherPrintStream.println("    {");
        dispatcherPrintStream.println("        final byte type = buffer.get(buffer.position());");
        dispatcherPrintStream.println("        switch (type) {");
        for (Map.Entry<String, List<MessageMetaData>> mapEntry : messages.entrySet()) {
        	String message = mapEntry.getKey() + direction;
        	for (MessageMetaData data: mapEntry.getValue()) {
        		dispatcherPrintStream.println("        case " + pascal(prefix) + "Type." + data.getMajor() + ":");
            }
        	dispatcherPrintStream.println("            " + camel(message) + ".setBuffer(buffer);"); 
        	dispatcherPrintStream.println("            messageListener.on" + pascal(prefix) + pascal(message) + "(" + camel(message)+ ");");            
        	dispatcherPrintStream.println("            break;");
        }
        dispatcherPrintStream.println("        default:");
        //dispatcherPrintStream.println("                log.log(Levels.WARN, log.getSB().append(\"Unsupported message: \").appendHexDump(buffer));");
        dispatcherPrintStream.println("            ByteStringBuilder sb = log.getSB().append(\"Unsupported message: \");");
        dispatcherPrintStream.println("            sb.append(\"Process Message\");");
        dispatcherPrintStream.println("            sb.appendHexDump(buffer);");
        dispatcherPrintStream.println("            log.log(Level.WARN, sb);");
        dispatcherPrintStream.println("        }");
        dispatcherPrintStream.println("    }");
        dispatcherPrintStream.println("}");
        dispatcherPrintStream.close();
        
        
    	System.out.println("Generate: " + pascal(prefix) + listenerName + ".java");
    	PrintStream listenerPrintStream = new PrintStream(generatedDir + "/" + pascal(prefix) + listenerName + ".java");
    	listenerPrintStream.println(comment);
    	listenerPrintStream.println();
		listenerPrintStream.println("package " + getPackage() + ";");
    	listenerPrintStream.println();
    	listenerPrintStream.println("import com.zerogc.messages.*;");
    	listenerPrintStream.println();
    	listenerPrintStream.println("public interface " + pascal(prefix) + listenerName + " {");

    	for (Map.Entry<String, List<MessageMetaData>> mapEntry : messages.entrySet()) {
    		String message = mapEntry.getKey() + direction;
    		listenerPrintStream.println("    public void on" + pascal(prefix) + pascal(message) + "(" + pascal(prefix) + pascal(message) + " " + camel(message) + ");");
    	}

    	listenerPrintStream.println("}");
    	listenerPrintStream.close();

    	System.out.println("Generate: " + pascal(prefix) + handlerName + ".java");
    	PrintStream handlerPrintStream = new PrintStream(generatedDir + "/" + pascal(prefix) + handlerName + ".java");
    	handlerPrintStream.println(comment);
    	handlerPrintStream.println();
   		handlerPrintStream.println("package " + getPackage() + ";");
    	handlerPrintStream.println();
    	handlerPrintStream.println("import com.zerogc.util.Level;");
    	handlerPrintStream.println("import com.zerogc.util.Logger;");
    	handlerPrintStream.println("import com.zerogc.messages.*;");
    	handlerPrintStream.println();
    	handlerPrintStream.println("public class " + pascal(prefix) + handlerName + " implements " + pascal(prefix) + listenerName + " {");
    	handlerPrintStream.println();    
    	handlerPrintStream.println("    protected Logger log;"); 
    	handlerPrintStream.println("    protected byte level;");
    	handlerPrintStream.println();    
    	handlerPrintStream.println("    public " + pascal(prefix) + handlerName + "(Logger log, byte level) {");    
    	handlerPrintStream.println("        this.log = log;");    
    	handlerPrintStream.println("        this.level = level;");    
    	handlerPrintStream.println("    }");    

    	for (Map.Entry<String, List<MessageMetaData>> mapEntry : messages.entrySet()) {
    		String message = mapEntry.getKey() + direction;
    		handlerPrintStream.println("    public void on" + pascal(prefix) + pascal(message) + "(" + pascal(prefix) + pascal(message) + " " + camel(message) + ") {");
    		handlerPrintStream.println("        log.log(level, " + camel(message) + ".toString(log.getSB()));");    
    		handlerPrintStream.println("    }");    
    	}

    	handlerPrintStream.println("}");
    	handlerPrintStream.close();
    }
    
    public static void main(String[] args) {
        GeneratorDispatcher generator = new GeneratorDispatcher(PACKAGE);
        generator.parseArgs(args);
        generator.run();
    }
}
