package pl.jkarczewski.JarEditor.helpers;

import javafx.util.Pair;

public class PrimitivesHelper {
    public static Pair<Class<?>, Object> translateNameToPair(String argType, String argText) throws PrimitivesHelperException {
        try {
            switch (argType) {
                case "byte":
                    return new Pair<>(byte.class, Byte.parseByte(argText));
                case "short":
                    return new Pair<>(short.class, Short.parseShort(argText));
                case "int":
                    return new Pair<>(int.class, Integer.parseInt(argText));
                case "long":
                    return new Pair<>(long.class, Long.parseLong(argText));
                case "float":
                    return new Pair<>(float.class, Float.parseFloat(argText));
                case "double":
                    return new Pair<>(double.class, Double.parseDouble(argText));
                case "boolean":
                    return new Pair<>(boolean.class, Boolean.parseBoolean(argText));
                case "char":
                    return new Pair<>(char.class, argText.charAt(0));
                default:
                    // Code not prepared to deal with non-primitive args
                    throw new PrimitivesHelperException("Got non-primitive arg");
            }
        } catch (Exception e) {
            throw new PrimitivesHelperException("Error parsing argument");
        }
    }
}
