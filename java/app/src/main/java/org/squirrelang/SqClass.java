package org.squirrelang;

import java.util.List;
import java.util.Map;

public class SqClass implements SqCallable {
    final String name;
    private final Map<String, SqFunction> methods;
    private final Map<String, SqFunction> staticMethods;

    SqClass(String name, Map<String, SqFunction> methods, Map<String, SqFunction> staticMethods) {
        this.name = name;
        this.methods = methods;
        this.staticMethods = staticMethods;
    }

    SqFunction findMethod(String name) {
        return methods.get(name);
    }

    SqFunction findStaticMethod(String name) {
        return staticMethods.get(name);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        SqInstance instance = new SqInstance(this);
        SqFunction initializer = findMethod("init");

        if (initializer != null) {
            initializer.bind(instance).call(interpreter, args);
        }

        return instance;
    }

    @Override
    public int arity() {
        SqFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
