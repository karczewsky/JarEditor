package pl.jkarczewski.JarEditor;

import javassist.*;

import java.util.LinkedList;
import java.util.List;

public class ClassWrapper {
    private CtClass ctClass;

    ClassWrapper(ClassPool cp, String name) {
        try {
            ctClass = cp.getCtClass(name);
        } catch (NotFoundException e) {
           throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return ctClass.getName();
    }

    public CtClass getCtClass() {
        return ctClass;
    }

    public List<MethodWrapper> getMethods() {
        List<MethodWrapper> list = new LinkedList<>();

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            list.add(new MethodWrapper(ctMethod));
        }

        return list;
    }

    public List<ConstructorWrapper> getConstructors() {
        List<ConstructorWrapper> list = new LinkedList<>();

        for (CtConstructor ctConstructor : ctClass.getConstructors()) {
            list.add(new ConstructorWrapper(ctConstructor));
        }

        return list;
    }
}
