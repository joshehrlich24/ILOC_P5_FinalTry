package edu.jmu.decaf;

import java.util.*;

/**
 * Concrete ILOC generator class.
 */
public class MyILOCGenerator extends ILOCGenerator
{

    public MyILOCGenerator()
    {
    }

    @Override
    public void postVisit(ASTFunction node)
    {
        // TODO: emit prologue

        // propagate code from body block to the function level
        copyCode(node, node.body);

        // TODO: emit epilogue
    }

    @Override
    public void postVisit(ASTBlock node)
    {
        // concatenate the generated code for all child statements
        for (ASTStatement s : node.statements) {
            copyCode(node, s);
        }
    }

    @Override
    public void postVisit(ASTReturn node)
    {
        // TODO: handle return value and emit epilogue
        emit(node, ILOCInstruction.Form.RETURN);
    }

}
