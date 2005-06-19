package quickfix.test.acceptance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class AcceptanceTestSuite extends TestSuite {

    private String acceptanceTestBaseDir = "test/quickfix/test/acceptance/definitions/";

    private final class TestDefinitionFilter implements FileFilter {
        public boolean accept(File file) {
            return (file.getName().endsWith(".def") && !file.getParentFile().getName().equals(
                    "future"))
                    || file.isDirectory();
        }
    }

    public static class AcceptanceTest extends TestCase {
        private final String filename;
        private final String testname;

        public AcceptanceTest(String filename) {
            this.filename = filename;
            testname = filename.substring(filename.lastIndexOf(File.separatorChar + "fix") + 1);
            setName(testname);
        }

        public int countTestCases() {
            return 1;
        }

        public void run(TestResult result) {
            result.startTest(this);
            TestContext context = null;
            try {
                context = new TestContext();
                List testSteps = load(filename);
                for (int i = 0; i < testSteps.size(); i++) {
                    ((TestStep) testSteps.get(i)).run(result, context);
                }
            } catch (AssertionFailedError e) {
                result.addFailure(this, e);
            } catch (Throwable t) {
                result.addError(this, t);
            } finally {
                if (context != null) {
                    context.tearDown();
                }
            }
            result.endTest(this);
        }

        private List load(String filename) throws IOException {
            ArrayList steps = new ArrayList();
            System.out.println("load: " + filename);
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(filename));
                String line = in.readLine();
                while (line != null) {
                    if (line.matches("^.*#")) {
                        continue;
                    } else if (line.startsWith("I")) {
                        steps.add(new InitiateMessageStep(line));
                    } else if (line.startsWith("E")) {
                        steps.add(new ExpectMessageStep(line));
                    } else if (line.matches("^i\\d*,?CONNECT")) {
                        steps.add(new ConnectToServerStep(line));
                    } else if (line.matches("^e\\d*,?DISCONNECT")) {
                        steps.add(new ExpectDisconnectStep(line));
                    }
                    line = in.readLine();
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            return steps;
        }

        public String toString() {
            return testname;
        }
    }

    public AcceptanceTestSuite() {
        addTests(new File(acceptanceTestBaseDir + "server/fix40"));
        addTests(new File(acceptanceTestBaseDir + "server/fix41"));
        addTests(new File(acceptanceTestBaseDir + "server/fix42"));
        addTests(new File(acceptanceTestBaseDir + "server/fix43"));
        addTests(new File(acceptanceTestBaseDir + "server/fix44"));
        
        //addTest("fix40/10_MsgSeqNumEqual.def");
        //addTest("fix43/21_RepeatingGroupSpecifierWithValueOfZero.def");
        //addTest("fix44/21_RepeatingGroupSpecifierWithValueOfZero.def");
        //addTest("fix44/14b_RequiredFieldMissing.def");
        //addTest("fix44/RejectResentMessage.def");
        //addTest("fix42/SimpleLogon.def");
        //addTest("fix42/AlreadyLoggedOn.def");
    }

    protected void addTest(String name) {
        addTests(new File(acceptanceTestBaseDir + "server/" + name));
    }

    protected void addTests(File directory) {
        if (!directory.isDirectory()) {
            addTest(new AcceptanceTest(directory.getPath()));
        } else {
            if (directory.exists()) {
                File[] files = directory.listFiles(new TestDefinitionFilter());
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isDirectory()) {
                        addTest(new AcceptanceTest(files[i].getPath()));
                    }
                }
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        addTests(files[i]);
                    }
                }
            } else {
                System.err.println("directory does not exist: " + directory.getPath());
            }
        }
    }

    public static Test suite() {
        return new TestSetup(new AcceptanceTestSuite()) {
            private Thread serverThread;
            
            protected void setUp() throws Exception {
                super.setUp();
                ATServer server = new ATServer();
                serverThread = new Thread(server, "ATServer");
                serverThread.start();
                server.waitForInitialization();
            }
            
            protected void tearDown() throws Exception {
                serverThread.interrupt();
                super.tearDown();
            }
        };
    }
}