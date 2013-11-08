package org.jf.baksmali;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * umbreyta is icelandic for transform :-)
 * 
 * 1. Read in the dex file with DexFileFactory.loadDexFile()
 * 2. Create a list of ImmutableClassDef
 * 3. Iterate over the dex file, for each class, make any method modifications as needed, and convert the modified class to an ImmutableClassDef and add it to the above list
 * 4. Construct a new ImmutableDexFile using the class list, and write that with DexFileFactory.writeDexFile()
 * 
 * TODO:
 * - Consider scanning the class/method first before even trying to instrument
 * - More generic way of defining what instruction has to be transformed and into what.
 * 
 * @author gaurav lochan
 */
public class umbreyta {
	// Instrumentation code
	final static String INSTRUMENTATION_PACKAGE = "Lcom/littleeyelabs/instrumentation";
	final static String HTTPCLIENT_WRAPPER = "Lcom/littleeyelabs/instrumentation/HttpClientWrapper;";
	final static String INSTRUMENTATION_FACTORY = "Lcom/littleeyelabs/instrumentation/InstrumentationFactory;";
	
	
	// Classes to replace
	final static String HTTPCLIENT = "Lorg/apache/http/client/HttpClient;";  // the interface
	final static String DEFAULT_HTTPCLIENT = "Lorg/apache/http/impl/client/DefaultHttpClient;";
	final static String ANDROID_HTTPCLIENT = "Landroid/net/http/AndroidHttpClient;";
	final static String URL_CLASS = "Ljava/net/URL;";

	// Methods to replace
	final static String EXECUTE = "execute";
	final static String OPEN_CONN_METHOD = "openConnection";
	
	// Argument types
	final static String HTTP_URI_REQ = "Lorg/apache/http/client/methods/HttpUriRequest;";
	
	// Return type
	final static String HTTP_RESPONSE = "Lorg/apache/http/HttpResponse;";
	final static String URL_CONNECTION = "Ljava/net/URLConnection;";
	

    static ClassDef transformClass(ClassDef classDef) {
        // System.out.println("\nProcessing Class: " + classDef);

        if (isInstrumentationClass(classDef)) {
            System.out.println("Skip instrumentation class: " + classDef);
        } else {
        	
        	Iterable<? extends Method> methods = classDef.getMethods();
            List<Method> newMethods = new ArrayList<Method>();

            boolean changed = false;
            for (Method method: methods) {
            	Method transformed = transformMethod(method);
            	if (transformed != null) {
                    System.out.println("Transformed Class: " + classDef + "\n");
            		newMethods.add(transformed);
            		changed = true;
            	} else {
            		newMethods.add(method);
            	}
            }
        	
            if (changed) {
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
        }
        
        return classDef;
    }

    
    /**
     * Transform a single method
     * 
     * @param method
     * 
     * @return null if no transformation was done
     */
    private static Method transformMethod(Method method) {
        // System.out.println("  Processing Method: " + method.getName());

    	// A method contains a bunch of metadata (that shouldn't change in our
    	// transformation) and an implementation.
        MethodImplementation implementation = method.getImplementation();

        if (implementation != null) {
            MutableMethodImplementation mmi = new MutableMethodImplementation(implementation);

            Iterable<? extends Instruction> instructions = mmi.getInstructions();
            boolean changed = false;
            int index = 0;
            for (Instruction instruction:instructions) {
                // System.out.println("    Processing instruction: " + instruction.getClass().getSimpleName());
                BuilderInstruction newInstr = replaceInstruction(instruction);
                if (newInstr != null) {
                	mmi.replaceInstruction(index, newInstr);
                	changed = true;
                }
                index++;
            }
            
            if (changed) {
                System.out.println("  Transformed Method: " + method.getName());

            	ImmutableMethod newMethod = new ImmutableMethod(
	        		method.getDefiningClass(),
	        		method.getName(),
	        		method.getParameters(),
	        		method.getReturnType(),
	        		method.getAccessFlags(),
	        		method.getAnnotations(),
	        		mmi);
            	return newMethod;
            }
            
            return null;
        } else {
        	// No implementation
        	return null;
        }
    }
        
    
    private static BuilderInstruction replaceInstruction(Instruction old) {

    	if (old instanceof Instruction35c) {
    		// Cast to ImmutableInstruction since it has methods we need
    		ImmutableInstruction35c old35c = (ImmutableInstruction35c) ImmutableInstruction35c.of(old);
    		ImmutableReference ref2 = old35c.getReference();

    		if (ref2 instanceof ImmutableMethodReference) {
        		ImmutableMethodReference ref = (ImmutableMethodReference) ref2;
        		
        		// Replace httpClient->execute
        		if ( (ref.getDefiningClass().equals(HTTPCLIENT) && (ref.getName().equals(EXECUTE))) ||
        				(ref.getDefiningClass().equals(DEFAULT_HTTPCLIENT) && (ref.getName().equals(EXECUTE))) ||
        				(ref.getDefiningClass().equals(ANDROID_HTTPCLIENT) && (ref.getName().equals(EXECUTE))) ) {

        			System.out.println("    *** Replacing Instruction: " + getPrintable(ref));

        			// Construct the method definition:
        	        // Insert HTTPCLIENT as the first arg since we're passing it into the wrapper
        	        // Use the other types as is
        			Builder<String> builder = new ImmutableList.Builder<String>();
        	        builder.add(HTTPCLIENT);
        	        ImmutableList<String> originalParams = ref.getParameterTypes();
        	        builder.addAll(originalParams);
        	        ImmutableList<String> immutableList = builder.build();

        			ImmutableMethodReference clientWrapperRef = new ImmutableMethodReference(HTTPCLIENT_WRAPPER, EXECUTE,
        					immutableList,   // Used to be ImmutableList.of(HTTPCLIENT, HTTP_URI_REQ);
        					ref.getReturnType()); // Used to be HTTP_RESPONSE);

        			
        			// Note: In HttpClient calls, the first register pointed to the httpClient object itself
        			// Since this is now a static method, the register pointing to 'this' is not needed.  
        			// The cool thing is that the wrapper code expects the httpclient as the first param
        			// So keep the register list as is.
        			BuilderInstruction35c newInstruction = new BuilderInstruction35c(
        					Opcode.INVOKE_STATIC, old35c.getRegisterCount(),
        					old35c.getRegisterC(), old35c.getRegisterD(), old35c.getRegisterE(), old35c.getRegisterF(), old35c.getRegisterG(),
        					clientWrapperRef);
        			
        			return newInstruction;
        		}
        		
        		// Replace url->openConnection
        		if (ref.getDefiningClass().equals(URL_CLASS) && (ref.getName().equals(OPEN_CONN_METHOD))) {
        			System.out.println("    *** Replacing Instruction: " + getPrintable(ref));

        			// Sanity checks
        			if (!ref.getReturnType().equalsIgnoreCase(URL_CONNECTION)) {
        				System.err.println("UrlConnection return Type mismatch");
        			}

        			ImmutableMethodReference clientWrapperRef = new ImmutableMethodReference(INSTRUMENTATION_FACTORY, OPEN_CONN_METHOD,
        					ImmutableList.of(URL_CLASS),
        					URL_CONNECTION);


        			BuilderInstruction35c newInstruction = new BuilderInstruction35c(
        					Opcode.INVOKE_STATIC, 1,
        					old35c.getRegisterC(), 0, 0, 0, 0,
        					clientWrapperRef);
        			
        			return newInstruction;
        		}
        		
    		} else {
    			System.out.println("Skipping since it's not a MethodReference ");
    		}
    	}
    	return null;
    }

    
        
    private static boolean isInstrumentationClass(ClassDef classDef) {
    	if (classDef.toString().startsWith(INSTRUMENTATION_PACKAGE)) {
    		return true;
    	}
    	return false;
    }
    
    private static String getPrintable(ImmutableMethodReference ref) {
    	String clazz = ref.getDefiningClass();
    	return clazz.substring(1, clazz.length()-1).replace('/', '.') + "." + ref.getName();
    }

    
}
