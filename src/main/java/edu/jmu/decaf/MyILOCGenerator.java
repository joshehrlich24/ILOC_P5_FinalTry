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
    		emit(node, ILOCInstruction.Form.I2I, ILOCOperand.REG_SP, ILOCOperand.REG_BP);
    		emitLocalVarStackAdjustment(node); // allocate space for local variables (might be in the wrong spot)
       
    	// propagate code from body block to the function level
    			
    		copyCode(node, node.body);

        // TODO: emit epilogue
    		
    		 emit(node, ILOCInstruction.Form.I2I, ILOCOperand.REG_BP, ILOCOperand.REG_SP);
    	     emit(node, ILOCInstruction.Form.POP, ILOCOperand.REG_BP);
    	     emit(node, ILOCInstruction.Form.RETURN);
    }
    
    public void postVisit(ASTFunctionCall node)
    {
    	int numberOfArgs = node.arguments.size();
    	int offset = numberOfArgs * 4;
    	
    	if(numberOfArgs > 0)
    	{
    		//Load the arguments
    		
    		for(int i = 0; i < numberOfArgs; i ++)
    		{
    			ILOCOperand temp = ILOCOperand.newVirtualReg();
    			temp = getTempReg(node.arguments.get(i));
    			//System.out.println("Meow " + temp + node.arguments.get(i).toString());
    			copyCode(node, node.arguments.get(i));
    			System.out.println("Here " + getCode(node.arguments.get(i)));
    		}
    		
    		
    		
    		for(int i = numberOfArgs - 1; i >= 0; i --) // Loop through arguments in reverse order and push them onto the stack
    		{
    			ILOCOperand temp = getTempReg(node.arguments.get(i));
    			emit(node, ILOCInstruction.Form.PUSH, temp);
    		}
    		
    		
    	}
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
    	if (node.hasValue()) 
    	{
    		System.out.println("RET\n");
    		System.out.println(node.value + " " + getTempReg(node.value));
    		copyCode(node, node.value);
    		System.out.println(getCode(node));
    		emit(node, ILOCInstruction.Form.I2I, getTempReg(node.value), ILOCOperand.REG_RET);
    	}
    	
    	// TODO: emit epilogue
    		emit(node, ILOCInstruction.Form.RETURN);
    }
    
    @Override 
    public void postVisit(ASTLocation loc)
    {	
    	ILOCOperand reg = ILOCOperand.newVirtualReg();
    	setTempReg(loc, reg);	
    	emitLoad(loc);
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
    		copyCode(node.getParent(), node);		
    	}
    	
    }
    
    public void postVisit(ASTAssignment node)
    {
    	ILOCOperand reg = getTempReg(node.value); // get the value of the assignment
    	emitStore(node, reg); 
    	setTempReg(node.value, reg);
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
		//Add other cases
    		default:
    			
			break;
    		
    	}
    
    	setTempReg(node, destReg);
    }
    
//    public void postVisit(ASTLocation node)
//    {
//    	ILOCOperand reg = ILOCOperand.newVirtualReg();
//    	setTempReg(node, reg);
//    }
    
    

}
