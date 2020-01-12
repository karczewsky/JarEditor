package pl.jkarczewski.JarEditor.helpers;

import javassist.CtConstructor;
import javassist.Modifier;
import javassist.bytecode.Descriptor;

public class ConstructorWrapper {
    private CtConstructor ctConstructor;

    ConstructorWrapper(CtConstructor ctConstructor) {
        this.ctConstructor = ctConstructor;
    }

    @Override
    public String toString() {
        return Modifier.toString(ctConstructor.getModifiers()) + " " +
                ctConstructor.getName() + Descriptor.toString(ctConstructor.getSignature());
    }

    public CtConstructor getCtConstructor() {
        return ctConstructor;
    }
}
