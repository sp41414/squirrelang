package org.squirrelang;

import java.util.HashMap;
import java.util.Map;

public class SqInstance {
    private final Map<String, Object> fields = new HashMap<>();
    private SqClass cls;

    SqInstance(SqClass cls) {
        this.cls = cls;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme))
            return fields.get(name.lexeme);
        SqFunction method = cls.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    SqClass getCls() { return this.cls; }
    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return "<" + cls.name + " instance>";
    }
}
