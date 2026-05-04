package org.squirrelang;

import java.util.List;

public interface SqCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> args);
}
