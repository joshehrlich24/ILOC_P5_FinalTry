package edu.jmu.decaf;

/**
 * Error encountered during execution.
 */
public class InvalidInstructionException extends Exception
{
    public static final long serialVersionUID = 1L;

    public InvalidInstructionException(String msg)
    {
        super(msg);
    }
}

