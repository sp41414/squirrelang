package org.squirrelang;

import static org.squirrelang.TokenType.*;

/*
TODO list:
[x] implement ternary operator
[x] bitwise operators by booleans aswell (convert to 0 and 1)
[x] 3 < "pancake" should be 3 < "pancake".length() for example
[] implement "scone" + 4 = "scone4"
[x] error on division by 0
 */
public class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Squirrelang.runtimeError(error);
        }
    }

    private String stringify(Object value) {
        if (value == null) return "nil";
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return value.toString();
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                return +(double) right;
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            // cant do NOT on doubles, so make it LONG, apply NOT, then back to double again
            case TILDE:
                return Double.longBitsToDouble(~Double.doubleToLongBits((double) right));
        };
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case MINUS:
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero");
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String) {
                    return left + stringify(right);
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or a string and any type.");
            case GREATER:
            case GREATER_EQUAL:
            case LESS:
            case LESS_EQUAL:
                checkNumberOrStringOperands(expr.operator, left, right);
                return binaryComparison(expr.operator, left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case XOR:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation(left, XOR, right);
            case AND:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation(left, AND, right);
            case OR:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation(left, OR, right);
            case SHIFT_LEFT:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation(left, SHIFT_LEFT, right);
            case SHIFT_RIGHT:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation(left, SHIFT_RIGHT, right);
            case SHIFT_RIGHT_UNSIGNED:
                checkNumberOperands(expr.operator, left, right);
                return binaryBitwiseOperation(left, SHIFT_RIGHT_UNSIGNED, right);
        }
        return null;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);
        if (isTruthy(condition)) return evaluate(expr.thenBranch);
        else return evaluate(expr.elseBranch);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private boolean binaryComparison(Token op, Object left, Object right) {
        // lexicographical comparison
        if (left instanceof String l && right instanceof String r) {
            int cmp = l.compareTo(r);
            return switch (op.type) {
                case GREATER       -> cmp > 0;
                case GREATER_EQUAL -> cmp >= 0;
                case LESS          -> cmp < 0;
                case LESS_EQUAL    -> cmp <= 0;
                default -> throw new RuntimeError(op, "Unexpected operator.");
            };
        }

        // string length comparison (to another double)
        // or double to double comparison
        double l = toDouble(left);
        double r = toDouble(right);

        return switch (op.type) {
            case GREATER -> l > r;
            case GREATER_EQUAL ->  l >= r;
            case LESS_EQUAL -> l <= r;
            case LESS -> l < r;
            default -> throw new RuntimeError(op, "Unexpected error in parsing token type.");
        };
    }

    private double binaryBitwiseOperation(Object a, TokenType op, Object b) {
        long longA = toLong(a);
        long longB = toLong(b);

        return (double) switch(op) {
            case XOR                  -> longA ^ longB;
            case AND                  -> longA & longB;
            case OR                   -> longA | longB;
            case SHIFT_RIGHT          -> longA >> longB;
            case SHIFT_LEFT           -> longA << longB;
            case SHIFT_RIGHT_UNSIGNED -> longA >>> longB;
            default                   -> 0L;
        };
    }

    private long toLong(Object val) {
        if (val instanceof Double d)  return d.longValue();
        if (val instanceof Boolean b) return b ? 1L : 0L;
        return 0L;
    }

    private double toDouble(Object val) {
        if (val instanceof Double d) return d;
        if (val instanceof String s) return s.length();
        return 0.0;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOrStringOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        if (left instanceof String || right instanceof String) return;
        throw new RuntimeError(operator, "Operands must be numbers or strings.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }
}