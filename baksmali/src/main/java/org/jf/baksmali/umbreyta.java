package org.jf.baksmali;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;

import com.google.common.collect.ImmutableList;

/**
 * umbreyta is icelandic for transform :-)
 * 
 * 1. Read in the dex file with DexFileFactory.loadDexFile()
 * 2. Create a list of ImmutableClassDef
 * 3. Iterate over the dex file, for each class, make any method modifications as needed, and convert the modified class to an ImmutableClassDef and add it to the above list
 * 4. Construct a new ImmutableDexFile using the class list, and write that with DexFileFactory.writeDexFile()
 * 
 * @author gaurav lochan
 */
public class umbreyta {
	// Instrumentation
	final static String INSTRUMENTATION_PACKAGE = "Lcom/littleeyelabs/instrumentation";
	final static String HTTPCLIENT_WRAPPER = "Lcom/littleeyelabs/instrumentation/HttpClientWrapper;";
	final static String INSTRUMENTATION_FACTORY = "Lcom/littleeyelabs/instrumentation/InstrumentationFactory;";
	
	
	// Classes
	final static String DEFAULT_HTTPCLIENT = "Lorg/apache/http/impl/client/DefaultHttpClient;";
	final static String HTTPCLIENT = "Lorg/apache/http/client/HttpClient;";
	
	// Methods
	final static String EXECUTE = "execute";
	
	// Argument types
	final static String HTTP_URI_REQ = "Lorg/apache/http/client/methods/HttpUriRequest;";
	
	// Return type
	final static String HTTP_RESPONSE = "Lorg/apache/http/HttpResponse;";
	

	
    // invoke-virtual {v2}, Ljava/net/URL;->openConnection()Ljava/net/URLConnection;
	// to
	// invoke-static {v2}, Lcom/littleeyelabs/instrumentation/InstrumentationFactory;->getUrlConnection(Ljava/net/URL;)Ljava/net/URLConnection;
	final static String URL_CLASS = "Ljava/net/URL;";
	final static String OPEN_CONN_METHOD = "openConnection";
	final static String URL_CONNECTION = "Ljava/net/URLConnection;";

    static ClassDef transformClass(ClassDef classDef) {
        System.out.println("\nProcessing Class: " + classDef);

        // First, scan and see if we need to modify this class
        boolean needToChange = needToTransform(classDef);
        

        if (needToChange) {
        	Iterable<? extends Method> methods = classDef.getMethods();
            List<Method> newMethods = new ArrayList<Method>();

            for (Method method: methods) {
                newMethods.add(transformMethod(method));
            }
        	
            return new ImmutableClassDef(
                    classDef.getType(),
                    classDef.getAccessFlags(),
                    classDef.getSuperclass(),
                    classDef.getInterfaces(),
                    classDef.getSourceFile(),
                    classDef.getAnnotations(),
                    classDef.getFields(),
                    newMethods);
        }
        
        return classDef;
    }

    
    /**
     * Transform a single method
     * 
     * @param method
     * @return
     */
    private static Method transformMethod(Method method) {
        System.out.println("  Processing Method: " + method.getName());

    	// A method contains a bunch of metadata (that shouldn't change in our
    	// transformation) and an implementation.
        MethodImplementation implementation = method.getImplementation();

        // Go through each of the instructions and change them if required.
        Iterable<? extends Instruction> instructions = implementation.getInstructions();
        List<Instruction> newInstructions = new ArrayList<Instruction>();
        
        for (Instruction instruction:instructions) {
        	Instruction newInstr = replaceInstruction(instruction);
        	newInstructions.add(newInstr);
        }

        // Since converting the original MethodImplementation to MutableMethodImplementation doesn't work,
        // could I just create a MutableMethodImplementation and add each instruction to it?
        
        MethodImplementation newImpl = new ImmutableMethodImplementation(
        		implementation.getRegisterCount(),   // TODO: We may need to change this in some cases
        		newInstructions,
        		implementation.getTryBlocks(),
        		implementation.getDebugItems());

        ImmutableMethod newMethod = new ImmutableMethod(
        		method.getDefiningClass(),
        		method.getName(),
        		method.getParameters(),
        		method.getReturnType(),
        		method.getAccessFlags(),
        		method.getAnnotations(),
        		newImpl);
        
        return newMethod;
    }
        
    
    
	// TODO: Do the conversion
    private static Instruction replaceInstruction(Instruction old) {
        // System.out.println("    Processing Instruction: " + old.getOpcode());

    	Opcode op = old.getOpcode();

    	if (old instanceof Instruction35c) {
    		// Cast to ImmutableInstruction since it has methods we need
    		ImmutableInstruction35c old35c = (ImmutableInstruction35c) ImmutableInstruction35c.of(old);
    		ImmutableMethodReference ref = (ImmutableMethodReference) old35c.getReference();
    		
    		// debug - get the httpclient wrapper MethodRef details
    		if (ref.getDefiningClass().equals(HTTPCLIENT_WRAPPER) && (ref.getName().equals(EXECUTE))) {
    			int debug = 0;
    		}

    		if (ref.getDefiningClass().equals(INSTRUMENTATION_FACTORY) && (ref.getName().equals(OPEN_CONN_METHOD))) {
    			int debug = 0;
    		}

    		// do the actual conversion
    		if (ref.getDefiningClass().equals(HTTPCLIENT) && (ref.getName().equals(EXECUTE))) {
    	        System.out.println("    *** Replacing Instruction: " + HTTPCLIENT + "->" + EXECUTE);
    			
    			ImmutableMethodReference clientWrapperRef = new ImmutableMethodReference(HTTPCLIENT_WRAPPER, EXECUTE,
    					ImmutableList.of(HTTPCLIENT, HTTP_URI_REQ),
    					HTTP_RESPONSE);
    			
    			ImmutableInstruction35c newInstruction = new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2,
    					old35c.getRegisterC(), old35c.getRegisterD(), 0, 0, 0,
    					clientWrapperRef);
    			
    			return newInstruction;
    		}
    		
    		if (ref.getDefiningClass().equals(URL_CLASS) && (ref.getName().equals(OPEN_CONN_METHOD))) {
    	        System.out.println("    *** Replacing Instruction: " + URL_CLASS + "->" + OPEN_CONN_METHOD);

    			ImmutableMethodReference clientWrapperRef = new ImmutableMethodReference(INSTRUMENTATION_FACTORY, OPEN_CONN_METHOD,
    					ImmutableList.of(URL_CLASS),
    					URL_CONNECTION);
    			
    			ImmutableInstruction35c newInstruction = new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 1,
    					old35c.getRegisterC(), 0, 0, 0, 0,
    					clientWrapperRef);
    			
    			return newInstruction;

    		}
    	}
    	return ImmutableInstruction.of(old);
    }
    
    private static boolean needToTransform(ClassDef classDef) {
    	// TODO: Check if class needs to be transformed
    	if (classDef.toString().startsWith(INSTRUMENTATION_PACKAGE)) {
    		System.out.println("*** Skip instrumentation class");
    		return false;
    	}
    
    	return true;
    }



}
