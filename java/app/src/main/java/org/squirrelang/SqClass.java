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
        return new SqInstance(this);
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
