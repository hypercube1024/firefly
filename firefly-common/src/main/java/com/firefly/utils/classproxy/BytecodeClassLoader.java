package com.firefly.utils.classproxy;

public class BytecodeClassLoader extends ClassLoader {

	public BytecodeClassLoader(){
        super(getParentClassLoader());
    }
    
    public BytecodeClassLoader(ClassLoader parent){
        super(parent);
    }

    static ClassLoader getParentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                contextClassLoader.loadClass(BytecodeClassLoader.class.getName());
                return contextClassLoader;
            } catch (ClassNotFoundException e) {
                // skip
            }
        }
        return BytecodeClassLoader.class.getClassLoader();
    }
}
