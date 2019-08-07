package batch.fat.junit;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.junit.utils.BatchJoblogFatUtils;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class BatchJoblogConfig2Test {

    private static final String appName = "batchFAT";
    private static final String SUCCESS_MESSAGE = "TEST PASSED";

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.joblog.config2.fat");

    @Rule
    public TestName testName = new TestName();

    /**
     * Set up the batch tables.
     */
    @BeforeClass
    public static void beforeClass() throws Exception {

        HttpUtils.trustAllCertificates();

        FatUtils.changeDatabase(server);
        BatchJoblogFatUtils.updateSchemaIfNecessary(server);

        FatUtils.checkJava7();

        server.startServer();

        FatUtils.waitForStartupAndSsl(server);

    }

    @Before
    public void before() throws Exception {
        log(testName.getMethodName(), "===== Starting test " + testName.getMethodName() + " =====");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(LibertyServer.DISABLE_FAILURE_CHECKING);
        }
    }

    @Test
    public void testBasicJobLogging() throws Exception {

        String output = BatchJoblogFatUtils.callServlet(server, appName, "javaUtilLoggingBatchlet");
        assertTrue("' appname:'" + appName + "' output:'" + output + "' testUri:'" + testName.getMethodName() + "'",
                   output.trim().contains(SUCCESS_MESSAGE));

        String[] params = output.substring(output.indexOf("jobName=")).split(" ");
        String jobName = params[0].substring(params[0].indexOf("=") + 1);
        int instanceID = Integer.parseInt(params[1].substring(params[1].indexOf("=") + 1));
        int execID = Integer.parseInt(params[2].substring(params[2].indexOf("=") + 1));

        File joblogFile = BatchJoblogFatUtils.verifyJoblogsExist(server, jobName, instanceID, execID);

        BatchJoblogFatUtils.verifyLogFile(joblogFile);

        // Verify the app logger wrote to the job log
        RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), joblogFile.getAbsolutePath());

        assertTrue("Should find batchlet trace in job log", BatchJoblogFatUtils.findBatchletAllTrace(server, logFile));
        assertFalse("Should not find batchlet SystemOut trace in job log", BatchJoblogFatUtils.findBatchletSystemOutTrace(server, logFile));

        assertTrue("Should find batchlet trace in server log", BatchJoblogFatUtils.findBatchletAllTrace(server, server.getMostRecentTraceFile()));
        assertTrue("Should find batchlet SystemOut trace in server log", BatchJoblogFatUtils.findBatchletSystemOutTrace(server, server.getMostRecentTraceFile()));

        // verify internal batch JobLogger wrote to the job log AND the server log
        assertTrue("Should find JobLogger trace in job log", BatchJoblogFatUtils.findJobLoggerFineTrace(server, logFile));
        assertTrue("Should find JobLogger trace in server log", BatchJoblogFatUtils.findJobLoggerFineTrace(server, server.getMostRecentTraceFile()));

        // verify BatchKernelImpl trace wrote to the job log AND the server log
        // Note: include spaces to ensure we match BatchKernelImpl trace records.
        assertTrue("Should find BatchKernelImpl trace in job log",
                   BatchJoblogFatUtils.isTraceInFile("com.ibm.jbatch.container.impl.BatchKernelImpl   ", server, logFile));
        assertTrue("Should find BatchKernelImpl trace in server log",
                   BatchJoblogFatUtils.isTraceInFile("com.ibm.jbatch.container.impl.BatchKernelImpl   ", server, server.getMostRecentTraceFile()));

        BatchJoblogFatUtils.verifyJobMessages(server, logFile);
        BatchJoblogFatUtils.verifyStepMessages(server, logFile, "step1");
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, Object msg) {
        Log.info(BatchJoblogConfig2Test.class, method, String.valueOf(msg));
    }

}
