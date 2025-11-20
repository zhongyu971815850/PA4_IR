package ir.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ast.Addition;
import ast.Assignment;
import ast.Computation;
import ast.DeclarationList;
import ast.Division;
import ast.Expression;
import ast.FunctionDeclaration;
import ast.Multiplication;
import ast.Statement;
import ast.StatementSequence;
import ast.Subtraction;
import ast.VariableDeclaration;
import ir.IRFunction;
import ir.IRProgram;
import ir.cfg.BasicBlock;
import ir.cfg.ControlFlowGraph;
import ir.tac.Add;
import ir.tac.Branch;
import ir.tac.Call;
import ir.tac.ConditionalBranch;
import ir.tac.Copy;
import ir.tac.Div;
import ir.tac.Literal;
import ir.tac.Mul;
import ir.tac.Return;
import ir.tac.Sub;
import ir.tac.Value;
import ir.tac.Variable;
import mocha.Symbol;

public class IRGenerator {

    private final AtomicInteger id = new AtomicInteger();
    private final Map<Symbol, Variable> globals = new HashMap<>();

    private Map<Symbol, Variable> locals;
    private IRFunction currentFunction;
    private BasicBlock exitBlock;
    private int blockCounter;

    public IRProgram generate(ast.AST astRoot) {
        Computation comp = astRoot.computation();
        IRProgram program = new IRProgram();
        if (comp == null) {
            return program;
        }
        // collect globals
        emitGlobals(comp.variables());
        // main
        IRFunction main = newFunction(comp.main());
        buildFunctionBody(comp.mainStatementSequence(), comp.variables());
        program.addFunction(main);
        program.setEntryFunction(main);
        // other functions
        for (FunctionDeclaration fnDecl : comp.functions()) {
            IRFunction fn = newFunction(fnDecl.function());
            emitLocals(fnDecl.body().variables());
            buildFunctionBody(fnDecl.body().statements(), null);
            program.addFunction(fn);
        }
        program.setGlobalSymbols(globals.keySet());
        return program;
    }

    private IRFunction newFunction(Symbol sym) {
        locals = new HashMap<>();
        blockCounter = 0;
        currentFunction = new IRFunction(sym, new ControlFlowGraph());
        BasicBlock entry = new BasicBlock(newBlockId());
        BasicBlock exit = new BasicBlock(newBlockId());
        currentFunction.setEntry(entry);
        currentFunction.setExit(exit);
        exitBlock = exit;
        exitBlock.addInstruction(new Return(id.getAndIncrement(), null));
        return currentFunction;
    }

    private void emitGlobals(DeclarationList globalsDecl) {
        if (globalsDecl == null) return;
        for (VariableDeclaration decl : globalsDecl.declarations()) {
            for (Symbol sym : decl.variables()) {
                globals.put(sym, new Variable(sym));
            }
        }
    }

    private void emitLocals(DeclarationList decls) {
        if (decls == null) return;
        emitLocals(decls.declarations());
    }

    private void emitLocals(Iterable<VariableDeclaration> decls) {
        if (decls == null) return;
        for (VariableDeclaration decl : decls) {
            for (Symbol sym : decl.variables()) {
                locals.put(sym, new Variable(sym));
            }
        }
    }

    private void buildFunctionBody(StatementSequence body, DeclarationList additionalDecls) {
        emitLocals(additionalDecls);
        BasicBlock entry = currentFunction.getEntry();
        BasicBlock exit = exitBlock;
        BasicBlock start = entry;
        BasicBlock end = buildStatements(body, start, exit);
        if (end != null && end != exit) {
            end.addSuccessor(exit);
        }
    }

    private BasicBlock buildStatements(StatementSequence seq, BasicBlock current, BasicBlock follow) {
        BasicBlock cursor = current;
        if (seq == null) return cursor;
        for (Statement stmt : seq.statements()) {
            if (cursor == null) break;
            if (stmt instanceof Assignment assign) {
                emitAssignment(assign, cursor);
            } else if (stmt instanceof ast.FunctionCall call) {
                emitCall(call, cursor);
            } else if (stmt instanceof ast.IfStatement ifs) {
                cursor = emitIf(ifs, cursor, follow);
            } else if (stmt instanceof ast.WhileStatement ws) {
                cursor = emitWhile(ws, cursor, follow);
            } else if (stmt instanceof ast.RepeatStatement rs) {
                cursor = emitRepeat(rs, cursor, follow);
            } else if (stmt instanceof ast.ReturnStatement ret) {
                emitReturn(ret, cursor);
                cursor = null; // terminate block
            }
        }
        return cursor;
    }

    private void emitAssignment(Assignment stmt, BasicBlock block) {
        Variable dest = materializeDesignator(stmt.destination());
        Value rhs = materializeExpression(stmt.source(), block);
        block.addInstruction(new Copy(id.incrementAndGet(), dest, rhs));
    }

    private void emitCall(ast.FunctionCall call, BasicBlock block) {
        ir.tac.ValueList args = new ir.tac.ValueList();
        if (call.arguments() != null) {
            call.arguments().arguments().forEach(arg -> args.add(materializeExpression(arg, block)));
        }
        block.addInstruction(new Call(id.incrementAndGet(), call.function(), args));
    }

    private void emitReturn(ast.ReturnStatement ret, BasicBlock block) {
        Value val = null;
        if (ret.expression() != null) {
            val = materializeExpression(ret.expression(), block);
        }
        block.addInstruction(new Return(id.incrementAndGet(), val));
        block.addSuccessor(exitBlock);
    }

    private BasicBlock emitIf(ast.IfStatement ifs, BasicBlock current, BasicBlock follow) {
        BasicBlock thenBlock = new BasicBlock(newBlockId());
        BasicBlock elseBlock = ifs.elseBranch() != null ? new BasicBlock(newBlockId()) : follow;
        BasicBlock after = follow != null ? follow : new BasicBlock(newBlockId());
        // condition jump
        emitCondition(ifs.condition(), current, thenBlock, elseBlock != null ? elseBlock : after);
        currentFunction.addBlock(thenBlock);
        BasicBlock endThen = buildStatements(ifs.thenBranch(), thenBlock, after);
        if (endThen != null) {
            endThen.addSuccessor(after);
        }
        if (ifs.elseBranch() != null) {
            currentFunction.addBlock(elseBlock);
            BasicBlock endElse = buildStatements(ifs.elseBranch(), elseBlock, after);
            if (endElse != null) {
                endElse.addSuccessor(after);
            }
        }
        if (follow == null) {
            currentFunction.addBlock(after);
        }
        return after;
    }

    private BasicBlock emitWhile(ast.WhileStatement ws, BasicBlock current, BasicBlock follow) {
        BasicBlock condBlock = new BasicBlock(newBlockId());
        current.addSuccessor(condBlock);
        currentFunction.addBlock(condBlock);
        BasicBlock body = new BasicBlock(newBlockId());
        BasicBlock after = follow != null ? follow : new BasicBlock(newBlockId());
        emitCondition(ws.condition(), condBlock, body, after);
        currentFunction.addBlock(body);
        BasicBlock endBody = buildStatements(ws.body(), body, condBlock);
        if (endBody != null) {
            endBody.addSuccessor(condBlock);
        }
        if (follow == null) {
            currentFunction.addBlock(after);
        }
        return after;
    }

    private BasicBlock emitRepeat(ast.RepeatStatement rs, BasicBlock current, BasicBlock follow) {
        BasicBlock body = new BasicBlock(newBlockId());
        current.addSuccessor(body);
        currentFunction.addBlock(body);
        BasicBlock after = follow != null ? follow : new BasicBlock(newBlockId());
        BasicBlock endBody = buildStatements(rs.body(), body, null);
        BasicBlock condBlock = endBody != null ? endBody : body;
        emitCondition(rs.condition(), condBlock, condBlock, after);
        if (follow == null) {
            currentFunction.addBlock(after);
        }
        return after;
    }

    private void emitCondition(Expression cond, BasicBlock block, BasicBlock trueTarget, BasicBlock falseTarget) {
        if (cond instanceof ast.Relation rel) {
            Value left = materializeExpression(rel.left(), block);
            Value right = materializeExpression(rel.right(), block);
            ConditionalBranch.Condition c = mapCondition(rel.operator().kind());
            block.addInstruction(new ConditionalBranch(id.incrementAndGet(), c, left, right, trueTarget, falseTarget));
        } else {
            Value test = materializeExpression(cond, block);
            block.addInstruction(new ConditionalBranch(id.incrementAndGet(), ConditionalBranch.Condition.NON_ZERO, test, null, trueTarget, falseTarget));
        }
        block.addSuccessor(trueTarget);
        block.addSuccessor(falseTarget);
    }

    private ConditionalBranch.Condition mapCondition(mocha.Token.Kind kind) {
        return switch (kind) {
            case EQUAL_TO -> ConditionalBranch.Condition.EQUAL;
            case NOT_EQUAL -> ConditionalBranch.Condition.NOT_EQUAL;
            case LESS_THAN -> ConditionalBranch.Condition.LESS;
            case LESS_EQUAL -> ConditionalBranch.Condition.LESS_EQUAL;
            case GREATER_THAN -> ConditionalBranch.Condition.GREATER;
            case GREATER_EQUAL -> ConditionalBranch.Condition.GREATER_EQUAL;
            default -> ConditionalBranch.Condition.NON_ZERO;
        };
    }

    private Variable materializeDesignator(Expression expr) {
        if (expr instanceof ast.Designator des) {
            if (locals.containsKey(des.symbol())) {
                return locals.get(des.symbol());
            }
            if (globals.containsKey(des.symbol())) {
                return globals.get(des.symbol());
            }
            Variable v = new Variable(des.symbol());
            globals.put(des.symbol(), v);
            return v;
        }
        Symbol tempSym = new Symbol("tmp" + locals.size(), null, false);
        return locals.computeIfAbsent(tempSym, s -> new Variable(s));
    }

    private Value materializeExpression(Expression expr, BasicBlock block) {
        if (expr instanceof ast.IntegerLiteral
                || expr instanceof ast.FloatLiteral
                || expr instanceof ast.BoolLiteral) {
            return new Literal(expr);
        }
        if (expr instanceof ast.Designator des) {
            return materializeDesignator(des);
        }
        if (expr instanceof Addition add) {
            return materializeBinary(add.left(), add.right(), block, Op.ADD);
        }
        if (expr instanceof Subtraction sub) {
            return materializeBinary(sub.left(), sub.right(), block, Op.SUB);
        }
        if (expr instanceof Multiplication mul) {
            return materializeBinary(mul.left(), mul.right(), block, Op.MUL);
        }
        if (expr instanceof Division div) {
            return materializeBinary(div.left(), div.right(), block, Op.DIV);
        }
        if (expr instanceof ast.FunctionCall call) {
            ir.tac.ValueList args = new ir.tac.ValueList();
            if (call.arguments() != null) {
                call.arguments().arguments().forEach(arg -> args.add(materializeExpression(arg, block)));
            }
            Variable tmp = newTemp();
            block.addInstruction(new Call(id.incrementAndGet(), tmp, call.function(), args));
            return tmp;
        }
        return new Literal(new ast.IntegerLiteral(0, 0, new mocha.Token("0", 0, 0)));
    }

    private enum Op { ADD, SUB, MUL, DIV }

    private Value materializeBinary(Expression l, Expression r, BasicBlock block, Op op) {
        Value left = materializeExpression(l, block);
        Value right = materializeExpression(r, block);
        Variable tmp = newTemp();
        switch (op) {
            case ADD -> block.addInstruction(new Add(id.incrementAndGet(), tmp, left, right));
            case SUB -> block.addInstruction(new Sub(id.incrementAndGet(), tmp, left, right));
            case MUL -> block.addInstruction(new Mul(id.incrementAndGet(), tmp, left, right));
            case DIV -> block.addInstruction(new Div(id.incrementAndGet(), tmp, left, right));
        }
        return tmp;
    }

    private int newBlockId() {
        return blockCounter++;
    }

    private Variable newTemp() {
        Symbol sym = new Symbol("t" + id.getAndIncrement(), null, false);
        return new Variable(sym);
    }
}
