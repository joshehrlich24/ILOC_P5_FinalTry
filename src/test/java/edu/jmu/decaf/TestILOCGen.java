package edu.jmu.decaf;

import java.io.*;
import java.time.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for ILOC code generation
 */
public class TestILOCGen extends TestCase
{
    /**
     * Initialization
     *
     * @param testName name of the test case
     */
    public TestILOCGen(String testName)
    {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(TestILOCGen.class);
    }

    /**
     * Helper class: runs a single test in a thread so that it can be
     * interrupted if it times out.
     */
    private static class TimeoutTest implements Runnable
    {
        String text;
        int retVal;

        TimeoutTest(String text)
        {
            this.text = text;
        }

        public int getReturnValue()
        {
            return retVal;
        }

        public void run()
        {
            StaticAnalysis.resetErrors();
            ASTProgram program = null;
            try {
                program = (new MyDecafParser()).parse(
                        (new MyDecafLexer()).lex(text));
                program.traverse(new BuildParentLinks());
                program.traverse(new BuildSymbolTables());
                program.traverse(new MyDecafAnalysis());
                String errors = StaticAnalysis.getErrorString();
                if (errors.length() > 0) {
                    throw new InvalidProgramException(errors);
                }
            } catch (IOException ex) {
                assertTrue(false);
            } catch (InvalidTokenException ex) {
                assertTrue(false);
            } catch (InvalidSyntaxException ex) {
                assertTrue(false);
            } catch (InvalidProgramException ex) {
                assertTrue(false);
            }
            program.traverse(new AllocateSymbols());
            ILOCGenerator codegen = new MyILOCGenerator();
            program.traverse(codegen);
            ILOCInterpreter interp = new ILOCInterpreter();
            interp.process(codegen.getProgram());
            retVal = interp.getReturnValue();
        }
    }

    /**
     * Parse, analyze, and generate ILOC for the given Decaf source code. Also
     * runs the resulting ILOC in the interpreter and returns the result. If the
     * test takes longer than a second, it is interrupted and counted as a
     * failure.
     * @param text Decaf source code
     * @return Integer program return value
     */
    public static int runProgram(String text)
    {
        TimeoutTest test = new TimeoutTest(text);
        Thread t = new Thread(test);
        Instant start = Instant.now();
        t.start();
        while (t.isAlive()) {
            Instant curr = Instant.now();
            Duration len = Duration.between(start, curr);
            if (len.getSeconds() > 1) {
                t.interrupt();
                assertTrue(false);
            }
        }
        return test.getReturnValue();
    }

    /**
     * Wrapper for {@code runProgram()} for testing simple expressions
     * @param expr Decaf expression
     * @return Integer program return value
     */
    public static int runExpr(String expr)
    {
        return runProgram("def int main() { return (" + expr + "); }");
    }

    public void testExprInt() { assertEquals(7, runExpr("7")); }
    public void testExprAdd() { assertEquals(5, runExpr("2+3")); }

    public void testAssign() { assertEquals(14, runProgram(
                "def int main() { " +
                "  int a; a = 2 + 3 * 4; " +
                "  return a; }")); }

    public void testIf() { assertEquals(2, runProgram(
                "def int main() { " +
                "  int r; " +
                "  if (true) { r = 2; } " +
                "  else { r = 3; } " +
                "  return r; }")); }

    public void testWhile() { assertEquals(10, runProgram(
                "def int main() { " +
                "  int a; a = 0; " +
                "  while (a < 10) { a = a + 1; } " +
                "  return a; }")); }

    public void testFuncCall() { assertEquals(5, runProgram(
                "def int add(int a, int b) { return a + b; } " +
                "def int main() { return add(2,3); }")); }

}
