package org.squirrelang;

import java.util.List;
import java.util.Map;

public class SqClass implements SqCallable {
    final String name;
    private final Map<String, SqFunction> methods;

    SqClass(String name, Map<String, SqFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    SqFunction findMethod(String name) {
        return methods.get(name);
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
