package pl.jkarczewski.JarEditor.helpers;

import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.Descriptor;

public class MethodWrapper {
    private CtMethod ctMethod;

    MethodWrapper(CtMethod ctMethod) {
        this.ctMethod = ctMethod;
    }

    @Override
    public String toString() {
        String out;
        try {
            String returnType = ctMethod.getReturnType().getName();
            out = Modifier.toString(ctMethod.getModifiers()) + " " + returnType + " " +
                    ctMethod.getName() + Descriptor.toString(ctMethod.getSignature());
        } catch (Exception e) {
            out = Modifier.toString(ctMethod.getModifiers()) + " " +
                    ctMethod.getName() + Descriptor.toString(ctMethod.getSignature());
        }

        return out;
    }

    public CtMethod getCtMethod() {
        return ctMethod;
    }
}
