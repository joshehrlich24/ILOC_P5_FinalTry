package edu.jmu.decaf;

import java.io.*;
import java.util.*;

/**
 * Decaf compiler driver class.
 */
class DecafCompiler
{
    private File mainFile;

    /**
     * Program entry point.
     * @param args Command-line arguments
     */
    public static void main(String[] args)
    {
        try {
            (new DecafCompiler(args)).run();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } catch (InvalidTokenException ex) {
            System.out.println(ex.getMessage());
        } catch (InvalidSyntaxException ex) {
            System.out.println(ex.getMessage());
        } catch (InvalidProgramException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Create a new compiler instance
     * @param args Command-line arguments
     */
    public DecafCompiler(String[] args)
    {
    	mainFile = null;
        parseCompilerArguments(args);
    }

    /**
     * Run all phases of the compiler.
     *
     * @throws IOException Thrown if there is an I/O problem
     * @throws InvalidTokenException Thrown if there is a lexing problem
     */
    public void run() throws IOException, InvalidTokenException,
           InvalidSyntaxException, InvalidProgramException
    {
        // PHASE 1 - LEXER

        // tokenize (Decaf source => Queue<Token>)
        DecafLexer lexer = new MyDecafLexer();
        BufferedReader input = new BufferedReader(new FileReader(mainFile));
        Queue<Token> tokens = lexer.lex(input, mainFile.getName());

        // PHASE 2 - PARSER

        // LL(1) parse (Queue<Token> => ASTNode)
        DecafParser parser = new MyDecafParser();
        ASTProgram ast = parser.parse(tokens);

        // sample AST traversals (annotate ASTNode)
        ast.traverse(new BuildParentLinks());
        ast.traverse(new CalculateNodeDepths());

        // PHASE 3 - ANALYSIS

        // build symbol tables (annotate ASTNode)
        StaticAnalysis symTablePass = new BuildSymbolTables();
        ast.traverse(symTablePass);

        // perform type checking (check ASTNode)
        StaticAnalysis typeCheckPass = new MyDecafAnalysis();
        ast.traverse(typeCheckPass);

        // aggregate and report any errors found during static analysis
        if (StaticAnalysis.getErrors().size() > 0) {
            throw new InvalidProgramException(
                    StaticAnalysis.getErrorString());
        }

        // PHASE 4 - IR CODE GEN

        // allocate stack symbols (annotate ASTNode)
        ast.traverse(new AllocateSymbols());

        // generate intermediate representation (ASTNode => ILOCProgram)
        ILOCGenerator codegen = new MyILOCGenerator();
        ast.traverse(codegen);
        ILOCProgram ir = codegen.getProgram();

        // print ILOC
        System.out.println(ir.toString());

        // run ILOC
        ILOCInterpreter interp = new ILOCInterpreter();
        interp.process(ir);
        System.out.println("\nResult: " + interp.getReturnValue());
    }

    /**
     * Parse command line arguments and set member variables
     *
     * @param args Command-line arguments from {@code main()}
     */
    private void parseCompilerArguments(String[] args)
    {
        if (args.length != 1) {
            System.out.println("Usage: ./decaf.sh <file>");
            System.exit(-1);
        }

        mainFile = new File(args[0]);
    }

    /**
     * Render the given AST to a PNG diagram. Exports to DOT format and then
     * uses a utility from the GraphViz package to render the actual image.
     * Requires that GraphViz be installed and that the "dot" utility be
     * accessible in the current user's PATH.
     *
     * @param ast Program to dump
     * @throws IOException Thrown if there is an I/O problem
     */
    private void debugASTGraphicalOutput(ASTProgram ast) throws IOException
    {
        String dotFilename = "ast.dot";
        String pngFilename = "ast.png";
        ast.traverse(new ExportTreeDOT(new PrintStream(dotFilename)));
        String cmd = "dot -Tpng -Nshape=box -Nfontname=sans " +
                dotFilename + " -o " + pngFilename;
        runShellCommand(cmd);
    }

    /**
     * Runs a command as if it was executed from a command-line shell.
     * This is used to call "dot" from the GraphViz package to render the
     * AST diagram.
     *
     * @param command Text of command to execute
     * @return Exit code from the shell
     * @throws IOException Thrown if there is a problem with I/O
     */
    private static int runShellCommand(String command) throws IOException
    {
        Process cmdProc = Runtime.getRuntime().exec(command);

        BufferedReader stdoutReader = new BufferedReader(
                 new InputStreamReader(cmdProc.getInputStream()));
        while ((stdoutReader.readLine()) != null) { }

        BufferedReader stderrReader = new BufferedReader(
                 new InputStreamReader(cmdProc.getErrorStream()));
        while ((stderrReader.readLine()) != null) { }

        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            System.out.println("ERROR: " + ex.getMessage());
        }

        return cmdProc.exitValue();
    }
}

