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
    		
    		emit(node, ILOCInstruction.Form.PUSH, ILOCOperand.REG_BP);
    		addComment(node, "Prologue");
    		emit(node, ILOCInstruction.Form.I2I, ILOCOperand.REG_SP, ILOCOperand.REG_BP);
    		emitLocalVarStackAdjustment(node); // allocate space for local variables (might be in the wrong spot)
       
    	// propagate code from body block to the function level
    			
    		copyCode(node, node.body);

        // TODO: emit epilogue
    		
    		
    }
    
    public void postVisit(ASTFunctionCall node)
    {
    	int numberOfArgs = node.arguments.size();
    	int offset = numberOfArgs * 4;
    	ILOCOperand returnReg = ILOCOperand.newVirtualReg();
    	
    	if(numberOfArgs > 0)
    	{
    		//Load the arguments
    		
    		for(int i = 0; i < numberOfArgs; i ++)
    		{
    			ILOCOperand temp = ILOCOperand.newVirtualReg();
    			temp = getTempReg(node.arguments.get(i));
    			copyCode(node, node.arguments.get(i));
    		}
    		
    		
    		
    		for(int i = numberOfArgs - 1; i >= 0; i --) // Loop through arguments in reverse order and push them onto the stack
    		{
    			ILOCOperand temp = getTempReg(node.arguments.get(i));
    			emit(node, ILOCInstruction.Form.PUSH, temp);
    		}
    		
    		emit(node, ILOCInstruction.Form.CALL, ILOCOperand.newCallLabel(node.name));
    		emit(node, ILOCInstruction.Form.ADD_I, ILOCOperand.REG_SP, ILOCOperand.newIntConstant(offset), ILOCOperand.REG_SP);
    		emit(node, ILOCInstruction.Form.I2I, ILOCOperand.REG_RET, returnReg);
    		
    		
    		setTempReg(node, returnReg);
    		
    		
    	}
    }

    @Override
    public void postVisit(ASTBlock node)
    {
//    	emit(node, ILOCInstruction.Form.LABEL, ILOCOperand.newAnonymousLabel());
    	
        // concatenate the generated code for all child statements
        for (ASTStatement s : node.statements) {
            copyCode(node, s);
        }
    }

    @Override
    public void postVisit(ASTReturn node)
    {
    	if (node.hasValue()) 
    	{
    		copyCode(node, node.value);
    		emit(node, ILOCInstruction.Form.I2I, getTempReg(node.value), ILOCOperand.REG_RET);
    	}
    	
    	ILOCOperand label = ILOCOperand.newAnonymousLabel();
    	
    	// TODO: emit epilogue
    	
    	emit(node, ILOCInstruction.Form.JUMP, label); //This could be wrong
    	emit(node, ILOCInstruction.Form.LABEL, label);
    	
    	
    	emit(node, ILOCInstruction.Form.I2I, ILOCOperand.REG_BP, ILOCOperand.REG_SP);
    	addComment(node, "Epilogue");
	    emit(node, ILOCInstruction.Form.POP, ILOCOperand.REG_BP);
	    emit(node, ILOCInstruction.Form.RETURN);
    }
    
    @Override 
    public void postVisit(ASTLocation loc)
    {	
    	ILOCOperand reg = emitLoad(loc);
    	setTempReg(loc, reg);	   	
    }
    
    public void postVisit(ASTLiteral node)
    {
    	ILOCOperand destReg = ILOCOperand.newVirtualReg();
    	
    	
    	switch (node.type) 
    	{
			case INT:
				emit(node, ILOCInstruction.Form.LOAD_I, ILOCOperand.newIntConstant(((Integer)node.value).intValue()), destReg);
				break;
			
			case BOOL:
				//Something else
				break;

			default:
				break;
    	}
    	
    	
    	setTempReg(node, destReg);
    	
    	if(!node.getParent().getASTTypeStr().equals("Return"))
    	{
    		//copyCode(node.getParent(), node);		
    	}
    	
    }
    
    public void postVisit(ASTAssignment node)
    {
    	//System.out.println("Assignment node --> " + getCode(node));
    	ILOCOperand reg = getTempReg(node.value); // get the value of the assignment
    	
    	setTempReg(node.value, reg);
    	
    	copyCode(node, node.value);
    	
    	
    	//System.out.println("Assignment node After copy --> " + getCode(node));
    	
    	emitStore(node, reg); 
    }
    
    public void postVisit(ASTBinaryExpr node)
    {
    	ILOCOperand leftReg = getTempReg(node.leftChild);
    	ILOCOperand rightReg = getTempReg(node.rightChild);
    	ILOCOperand destReg = ILOCOperand.newVirtualReg();
    	
    	copyCode(node, node.leftChild);
    	copyCode(node, node.rightChild);
   
    	switch(node.operator)
    	{
    		case ADD:
    			
    			emit(node, ILOCInstruction.Form.ADD, leftReg, rightReg, destReg);
    			break;
    		
    		case SUB:
    	    	
    			emit(node, ILOCInstruction.Form.SUB, leftReg, rightReg, destReg);
    	    	break;
    		
    		case MUL:
    			
    			emit(node, ILOCInstruction.Form.MULT, leftReg, rightReg, destReg);
    			break;
        		
    		case DIV:
    			
    			emit(node, ILOCInstruction.Form.DIV, leftReg, rightReg, destReg);
    			break;
        		
    		case AND:
    			
    			emit(node, ILOCInstruction.Form.AND, leftReg, rightReg, destReg);
    			break;
    		
    		case EQ:
    			
    			emit(node, ILOCInstruction.Form.CMP_EQ, leftReg, rightReg, destReg);
    			break;
		
    		case GE:
    			
    			emit(node, ILOCInstruction.Form.CMP_GE, leftReg, rightReg, destReg);
    			break;
		
    		case GT:
    			
    			emit(node, ILOCInstruction.Form.CMP_GT, leftReg, rightReg, destReg);
    			break;
		
    		case LE:
    			emit(node, ILOCInstruction.Form.CMP_LE, leftReg, rightReg, destReg);
    	    	break;
		
    		case LT:

    			emit(node, ILOCInstruction.Form.CMP_LT, leftReg, rightReg, destReg);
    			//emit(node, ILOCInstruction.Form.CBR, destReg);
    	    	break;
		//Add other cases
    		default:
    			
			break;
    		
    	}
    
    	setTempReg(node, destReg);
    }
    
    public void postVisit(ASTUnaryExpr node)
    {
    	ILOCOperand childReg = getTempReg(node.child);
    	ILOCOperand destReg = ILOCOperand.newVirtualReg();
    	
    	copyCode(node, node.child);
    	
    	switch(node.operator)
    	{
    		case NOT:
    			emit(node, ILOCInstruction.Form.NOT, childReg, destReg);
    			break;
    		
    		case NEG: 
    			
    			emit(node, ILOCInstruction.Form.NEG, childReg, destReg);
    			break;
    		case INVALID:
    			
			break;
    	}
    	//System.out.println("UnaryExpression node after negation --> " + getCode(node));
    	
    	//copyCode(node.getParent(), node);
    	
    	setTempReg(node, destReg);
    }
    
    public void postVisit(ASTConditional node)
    {
    	
    	copyCode(node, node.condition);
    	
    	ILOCOperand label1 = ILOCOperand.newAnonymousLabel();
    	ILOCOperand label2 = ILOCOperand.newAnonymousLabel();
    	ILOCOperand label3 = ILOCOperand.newAnonymousLabel();
    	
    	
    	emit(node, ILOCInstruction.Form.CBR, getTempReg(node.condition), label1, label2);
    	emit(node, ILOCInstruction.Form.LABEL, label1);
    	
    	
    	copyCode(node, node.ifBlock);
    	
    	if(node.hasElseBlock())
    	{
    		emit(node, ILOCInstruction.Form.JUMP, label3); //This could be wrong
    	}
    
    	
    	
    	
    	emit(node, ILOCInstruction.Form.LABEL, label2);
    	
    	
    	
    	
    	 if(node.hasElseBlock())
    	 {
    		 
    		 if(node.elseBlock.statements.size() > 0)
    		 {
    			copyCode(node, node.elseBlock); 
    			emit(node, ILOCInstruction.Form.LABEL, label3);	
    		 }
    	 }
    	

    }
    
    public void postVisit(ASTWhileLoop node)
    {
    	ILOCOperand label1 = ILOCOperand.newAnonymousLabel();
    	ILOCOperand label2 = ILOCOperand.newAnonymousLabel();
    	ILOCOperand label3 = ILOCOperand.newAnonymousLabel();
    	
    	emit(node, ILOCInstruction.Form.LABEL, label1);
    	copyCode(node, node.guard);

    	emit(node, ILOCInstruction.Form.CBR, getTempReg(node.guard), label2, label3);

    	emit(node, ILOCInstruction.Form.LABEL, label2);
    	copyCode(node, node.body);
    	emit(node, ILOCInstruction.Form.JUMP, label1); //This could be wrong
    	emit(node, ILOCInstruction.Form.LABEL, label3);


    	

    
    }
}
