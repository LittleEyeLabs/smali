package org.jf.baksmali;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
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
	
	final static String INSTRUMENTATION_PACKAGE = "Lcom/littleeyelabs/instrumentation";
	
	final static String DEFAULT_HTTPCLIENT = "Lorg/apache/http/impl/client/DefaultHttpClient;";
	final static String HTTPCLIENT = "Lorg/apache/http/client/HttpClient;";
	final static String HTTPCLIENT_WRAPPER = "Lcom/littleeyelabs/instrumentation/HttpClientWrapper;";
	final static String EXECUTE = "execute";
	
	
	final static String HTTP_URI_REQ = "Lorg/apache/http/client/methods/HttpUriRequest;";
	final static String HTTP_RESPONSE = "Lorg/apache/http/HttpResponse;";
	

    static ClassDef transformClass(ClassDef dexClassDef) {
        System.out.println("\nProcessing Class: " + dexClassDef);
        ImmutableClassDef classDef = ImmutableClassDef.of(dexClassDef);

        // First, scan and see if we need to modify this class
        boolean needToChange = needToTransform(classDef);

        if (needToChange) {
            Iterable<? extends Method> methods = classDef.getMethods();
            List<Method> newMethods = new ArrayList<Method>();

            for (Method method: methods) {
                System.out.println("Processing Method: " + method.getName());
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
    	Opcode op = old.getOpcode();
    	if (old instanceof ImmutableInstruction35c) {
    		ImmutableInstruction35c old35c = (ImmutableInstruction35c) old;
    		
    		// Think about whether these casts are a bad idea
    		ImmutableMethodReference ref = (ImmutableMethodReference) old35c.getReference();
    		
    		// debug - get the httpclient wrapper MethodRef details
    		if (ref.getDefiningClass().equals(HTTPCLIENT_WRAPPER) && (ref.getName().equals(EXECUTE))) {
    			int debug = 0;
    		}
    		
    		// do the actual conversion
    		if (ref.getDefiningClass().equals(HTTPCLIENT) && (ref.getName().equals(EXECUTE))) {
    			ImmutableMethodReference clientWrapperRef = new ImmutableMethodReference(HTTPCLIENT_WRAPPER, EXECUTE,
    					ImmutableList.of(HTTPCLIENT, HTTP_URI_REQ),
    					HTTP_RESPONSE);
    			
    			// TODO: Figure out how to get the MethodRef for the Wrapper function
    			ImmutableInstruction35c newInstruction = new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2,
    					old35c.getRegisterC(), old35c.getRegisterD(), old35c.getRegisterE(), old35c.getRegisterF(), old35c.getRegisterG(),
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
