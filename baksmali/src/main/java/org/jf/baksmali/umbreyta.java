package org.jf.baksmali;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;

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
	
    static ClassDef transformClass(ClassDef oldClassDef) {
        System.out.println("\nProcessing Class: "+oldClassDef);
        ImmutableClassDef classDef = ImmutableClassDef.of(oldClassDef);
        
        // First, scan and see if we need to modify this class
        
        
        // if need to modify, then need to create a new list of methods
        // then go through each method and add it to the list
        List<ImmutableMethod> newMethods = new ArrayList<ImmutableMethod>();
        Iterable<? extends Method> methods = classDef.getMethods();

        for (Method method: methods) {
            System.out.println("Processing Method: " + method.getName());
            
            MutableMethodImplementation methodImpl = new MutableMethodImplementation(method.getImplementation());
            ImmutableMethod newMethod = new ImmutableMethod(
            		method.getDefiningClass(),
            		method.getName(),
            		method.getParameters(),
            		method.getReturnType(),
            		method.getAccessFlags(),
            		method.getAnnotations(),
                    //mutableMethodImplementation);
            		methodImpl);
            newMethods.add(newMethod);
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
        
        
//        Iterable<DexBackedMethod> methods = (Iterable<DexBackedMethod>) classDef.getMethods();
//        
//        for (DexBackedMethod method: methods) {
//        	DexBackedMethodImplementation impl = method.getImplementation();
//        	Iterable<Instruction> instructions = (Iterable<Instruction>) impl.getInstructions();
//        	System.out.print("  Method: "+method.getName());
//        	System.out.println("");
//        	
//        }
    }


}
