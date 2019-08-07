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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.jbatch.test.BatchRestUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.junit.utils.BatchJoblogFatUtils;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

public class BatchJoblogTest {

    private static final String appName = "batchFAT";
    private static final String SUCCESS_MESSAGE = "TEST PASSED";
    private static final int TIMEOUT = 10000;
    private String curMethod = "";
    private String curDate;
    private static String previousLogRoot;

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.joblog.fat");

    public String _testName = "";
    private final Class<?> c = BatchJoblogTest.class;

    private static final String step1 = "step1";

    // These should match the ID fields in splitflow.xml
    private static final String split1 = "split1";
    private static final String flow1 = "flow1";
    private static final String flow2 = "flow2";

    private static final String splitA = "splitA";
    private static final String flowA1 = "flowA1";
    private static final String flowA2 = "flowA2";
    private static final String stepA1 = "stepA1";
    private static final String stepA2 = "stepA2";

    private static final String splitB = "splitB";
    private static final String flowB1 = "flowB1";
    private static final String flowB2 = "flowB2";
    private static final String stepB1 = "stepB1";
    private static final String stepB2 = "stepB2";

    protected final static String ADMIN_NAME = "bob";
    protected final static String ADMIN_PASSWORD = "bobpwd";

    private static RemoteFile traceFile;
    private static RemoteFile consoleLogFile;
    private static RemoteFile messagesLogFile;

    //Instance fields
    private final Map<String, String> adminHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(ADMIN_NAME + ":" + ADMIN_PASSWORD));

    @Rule
    public TestName name = new TestName();

    /**
     * Set up the batch tables.
     */
    @BeforeClass
    public static void beforeClass() throws Exception {

        HttpUtils.trustAllCertificates();
        previousLogRoot = server.getLogsRoot();

        // Change default log location
        createServerEnvWithLOG_DIR(server);

        // Need to set this for the LibertyServer; otherwise it uses the default
        // location in the waitForStringInLog method.
        server.setLogsRoot(server.getServerRoot() + "/NewServerOutputDir/");

        FatUtils.checkJava7();

        FatUtils.changeDatabase(server);
        BatchJoblogFatUtils.updateSchemaIfNecessary(server);

        server.startServer();
        FatUtils.waitForStartupAndSsl(server);

        traceFile = server.getMostRecentTraceFile();
        consoleLogFile = server.getConsoleLogFile();
        messagesLogFile = server.getDefaultLogFile();

    }

    @Before
    public void before() throws Exception {
        _testName = name.getMethodName();
        Log.info(c, _testName, "===== Starting test " + _testName + " =====");

        //Set mark for each test
        server.setMarkToEndOfLog(traceFile, consoleLogFile, messagesLogFile);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0011W");
            server.setLogsRoot(previousLogRoot);
            server.deleteFileFromLibertyServerRoot("server.env");
        }
    }

    private static void createServerEnvWithLOG_DIR(LibertyServer server) throws Exception {
        String path = server.getServerRoot() + "/NewServerOutputDir";
        File file = new File(server.getServerRoot() + File.separator + "server.env");
        Writer output = new BufferedWriter(new FileWriter(file));
        output.write("LOG_DIR=" + path);
        output.close();
    }

    @Test
    public void testBasicJobLogging() throws Exception {
        curMethod = "testBasicJobLogging()";

        String output = callServlet("simple");

        // Need to do this here to narrow a timing window where the date could have changed.
        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String[] params = output.substring(output.indexOf("jobName=")).split(" ");
        String jobName = params[0].substring(params[0].indexOf("=") + 1);
        int instanceID = Integer.parseInt(params[1].substring(params[1].indexOf("=") + 1));
        int execID = Integer.parseInt(params[2].substring(params[2].indexOf("=") + 1));

        logVerificationPoint("Verifying directory structure and job log presence");
        File joblogsDir = new File(server.getLogsRoot() + File.separator + "joblogs");
        assertTrue("Could not find " + joblogsDir.getAbsolutePath(),
                   joblogsDir.exists());

        File jobDir = new File(joblogsDir, jobName);
        assertTrue("Could not find " + jobDir.getAbsolutePath(),
                   jobDir.exists());

        File dateDir = new File(jobDir, curDate);
        assertTrue("Could not find " + dateDir.getAbsolutePath(),
                   dateDir.exists());

        File instanceDir = new File(dateDir, "instance." + instanceID);
        assertTrue("Could not find " + instanceDir.getAbsolutePath(),
                   instanceDir.exists());

        File execDir = new File(instanceDir, "execution." + execID);
        assertTrue("Could not find " + execDir.getAbsolutePath(),
                   execDir.exists());
        logVerificationPassed();

        File joblogFile = new File(execDir, "part.1.log");
        verifyLogFile(joblogFile);

        logVerificationPoint("Verifying log4j logging was successful");
        RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), joblogFile.getAbsolutePath());
        List<String> matches = server.findStringsInLogs("LOG4J SUCCESS", logFile);
        assertFalse("No log4j success message was found",
                    matches.isEmpty());
        logVerificationPassed();

        BatchJoblogFatUtils.verifyJobMessages(server, logFile);
        BatchJoblogFatUtils.verifyStepMessages(server, logFile, step1);

        BatchJoblogFatUtils.verifyJobMessagesNotPresent(server, traceFile);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, traceFile, step1);

        BatchJoblogFatUtils.verifyJobMessagesNotPresent(server, consoleLogFile);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, consoleLogFile, step1);

        BatchJoblogFatUtils.verifyJobMessagesNotPresent(server, messagesLogFile);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, messagesLogFile, step1);

        logStepCompleted();
    }

    @Test
    public void testFailingPartitionedJobLogging() throws Exception {
        curMethod = "testPartitionedJobLoggingFailing()";

        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        BatchRestUtils restUtils = new BatchRestUtils(server);

        Properties props = new Properties();
        props.put("forceFailure", "true");

        JsonObject jobInstance = restUtils.submitJob("batchFAT", "partition", props, BatchRestUtils.BATCH_BASE_URL);

        String jobName = jobInstance.getString("jobName");

        long instanceId = jobInstance.getJsonNumber("instanceId").longValue();

        JsonObject jobExecution = restUtils.waitForFirstJobExecution(instanceId, BatchRestUtils.BATCH_BASE_URL);

        long executionId = jobExecution.getJsonNumber("executionId").longValue();

        jobInstance = restUtils.waitForJobInstanceToFinish(jobInstance.getJsonNumber("instanceId").longValue(), BatchRestUtils.BATCH_BASE_URL);

        assertEquals("FAILED", jobInstance.getString("batchStatus"));

        logStep("Verifying directory structure for partitioned step");
        File execDir = new File(server.getLogsRoot() + File.separator
                                + "joblogs" + File.separator
                                + jobName + File.separator
                                + curDate + File.separator
                                + "instance." + instanceId + File.separator
                                + "execution." + executionId);

        assertTrue("Could not find " + execDir.getAbsolutePath(),
                   execDir.exists());

        for (int i = 0; i < 3; i++) {

            File partitionJobLogFile = new File(execDir, "step1" + File.separator + i + File.separator + "part.1.log");
            verifyLogFile(partitionJobLogFile);

            RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), partitionJobLogFile.getAbsolutePath());
            BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0030I: An exception occurred");
            BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, traceFile, "CWWKY0030I: An exception occurred");
            BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, messagesLogFile, "CWWKY0030I: An exception occurred");
            BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, consoleLogFile, "CWWKY0030I: An exception occurred");

        }

        RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), execDir.getAbsolutePath() + File.separator + "part.1.log");

        BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0030I: An exception occurred");

        BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, traceFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, messagesLogFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, consoleLogFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");

        logStepCompleted();
    }

    @Test
    public void testFailingSimpleJobLogging() throws Exception {
        //TODO
        curMethod = "testPartitionedJobLoggingFailing()";
        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        BatchRestUtils restUtils = new BatchRestUtils(server);

        Properties props = new Properties();
        props.put("forceFailure", "true");

        JsonObject jobInstance = restUtils.submitJob("batchFAT", "simple", props, BatchRestUtils.BATCH_BASE_URL);
        String jobName = jobInstance.getString("jobName");

        long instanceId = jobInstance.getJsonNumber("instanceId").longValue();

        JsonObject jobExecution = restUtils.waitForFirstJobExecution(instanceId, BatchRestUtils.BATCH_BASE_URL);

        long executionId = jobExecution.getJsonNumber("executionId").longValue();

        jobInstance = restUtils.waitForJobInstanceToFinish(jobInstance.getJsonNumber("instanceId").longValue(), BatchRestUtils.BATCH_BASE_URL);

        assertEquals("FAILED", jobInstance.getString("batchStatus"));
        logVerificationPoint("Verifying directory structure and job log presence");
        File joblogsDir = new File(server.getLogsRoot() + File.separator + "joblogs");
        assertTrue("Could not find " + joblogsDir.getAbsolutePath(), joblogsDir.exists());

        File jobDir = new File(joblogsDir, jobName);
        assertTrue("Could not find " + jobDir.getAbsolutePath(), jobDir.exists());

        File dateDir = new File(jobDir, curDate);
        assertTrue("Could not find " + dateDir.getAbsolutePath(), dateDir.exists());

        File instanceDir = new File(dateDir, "instance." + instanceId);
        assertTrue("Could not find " + instanceDir.getAbsolutePath(), instanceDir.exists());

        File execDir = new File(instanceDir, "execution." + executionId);
        assertTrue("Could not find " + execDir.getAbsolutePath(), execDir.exists());

        logVerificationPassed();

        File joblogFile = new File(execDir, "part.1.log");

        verifyLogFile(joblogFile);

        logVerificationPoint("Verifying log4j logging was successful");
        RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), joblogFile.getAbsolutePath());
        List<String> matches = server.findStringsInLogs("LOG4J SUCCESS", logFile);
        assertFalse("No log4j success message was found", matches.isEmpty());
        logVerificationPassed();

        BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0030I: An exception occurred");
        BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, traceFile, "CWWKY0030I: An exception occurred");
        BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, messagesLogFile, "CWWKY0030I: An exception occurred");
        BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, consoleLogFile, "CWWKY0030I: An exception occurred");

        BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, traceFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, messagesLogFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, consoleLogFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");

        logStepCompleted();
    }

    @Test
    public void testPartitionedJobLogging() throws Exception {
        curMethod = "testPartitionedJobLogging()";

        String output = callServlet("partition");

        // Need to do this here to narrow a timing window where the date could have changed.
        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String[] params = output.substring(output.indexOf("jobName=")).split(" ");
        String jobName = params[0].substring(params[0].indexOf("=") + 1);
        int instanceID = Integer.parseInt(params[1].substring(params[1].indexOf("=") + 1));
        int execID = Integer.parseInt(params[2].substring(params[2].indexOf("=") + 1));

        logStep("Verifying directory structure for partitioned step");
        File execDir = new File(server.getLogsRoot() + File.separator
                                + "joblogs" + File.separator
                                + jobName + File.separator
                                + curDate + File.separator
                                + "instance." + instanceID + File.separator
                                + "execution." + execID);

        assertTrue("Could not find " + execDir.getAbsolutePath(),
                   execDir.exists());

        verifyLogFile(new File(execDir, "step1" + File.separator + 0 + File.separator + "part.1.log"));
        verifyLogFile(new File(execDir, "step1" + File.separator + 1 + File.separator + "part.1.log"));
        verifyLogFile(new File(execDir, "step1" + File.separator + 2 + File.separator + "part.1.log"));

        RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), execDir.getAbsolutePath() + File.separator + "part.1.log");

        BatchJoblogFatUtils.verifyJobMessages(server, logFile);
        BatchJoblogFatUtils.verifyJobMessagesNotPresent(server, traceFile);
        BatchJoblogFatUtils.verifyJobMessagesNotPresent(server, consoleLogFile);
        BatchJoblogFatUtils.verifyJobMessagesNotPresent(server, messagesLogFile);

        BatchJoblogFatUtils.verifyStepMessages(server, logFile, step1);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, traceFile, step1);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, consoleLogFile, step1);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, messagesLogFile, step1);

        logStepCompleted();
    }

    @Test
    public void testSplitFlowJobLogging() throws Exception {
        curMethod = "testSplitFlowJobLogging()";

        String output = callServlet("splitflow");

        // Need to do this here to narrow a timing window where the date could have changed.
        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String[] params = output.substring(output.indexOf("jobName=")).split(" ");
        String jobName = params[0].substring(params[0].indexOf("=") + 1);
        int instanceID = Integer.parseInt(params[1].substring(params[1].indexOf("=") + 1));
        int execID = Integer.parseInt(params[2].substring(params[2].indexOf("=") + 1));

        logStep("Verifying directory structure for split flows");
        File execDir = new File(server.getLogsRoot() + File.separator
                                + "joblogs" + File.separator
                                + jobName + File.separator
                                + curDate + File.separator
                                + "instance." + instanceID + File.separator
                                + "execution." + execID);

        assertTrue("Could not find " + execDir.getAbsolutePath(),
                   execDir.exists());

        BatchJoblogFatUtils.verifyJobMessages(server, new RemoteFile(Machine.getLocalMachine(), execDir.getAbsolutePath() + File.separator + "part.1.log"));

        verifyLogFile(new File(execDir, split1 + File.separator + flow1 + File.separator + "part.1.log"));
        verifyLogFile(new File(execDir, split1 + File.separator + flow2 + File.separator + "part.1.log"));

        File fileA1 = new File(execDir, splitA + File.separator + flowA1 + File.separator + "part.1.log");
        verifyLogFile(fileA1);
        BatchJoblogFatUtils.verifyStepMessages(server, new RemoteFile(Machine.getLocalMachine(), fileA1.getAbsolutePath()), stepA1);

        File fileA2 = new File(execDir, splitA + File.separator + flowA2 + File.separator + "part.1.log");
        verifyLogFile(fileA2);
        BatchJoblogFatUtils.verifyStepMessages(server, new RemoteFile(Machine.getLocalMachine(), fileA2.getAbsolutePath()), stepA2);

        File fileB1 = new File(execDir, splitB + File.separator + flowB1 + File.separator + "part.1.log");
        verifyLogFile(fileB1);
        BatchJoblogFatUtils.verifyStepMessages(server, new RemoteFile(Machine.getLocalMachine(), fileB1.getAbsolutePath()), stepB1);

        File fileB2 = new File(execDir, splitB + File.separator + flowB2 + File.separator + "part.1.log");
        verifyLogFile(fileB2);

        BatchJoblogFatUtils.verifyStepMessages(server, new RemoteFile(Machine.getLocalMachine(), fileB2.getAbsolutePath()), stepB2);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, traceFile, stepB2);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, consoleLogFile, stepB2);
        BatchJoblogFatUtils.verifyStepMessagesNotPresent(server, messagesLogFile, stepB2);

        logStepCompleted();
    }

    @Test
    public void testFailingSplitFlowJobLogging() throws Exception {

        curMethod = "testFailingSplitFlowJobLogging()";
        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        BatchRestUtils restUtils = new BatchRestUtils(server);

        Properties props = new Properties();
        props.put("afterJobException", "true");

        JsonObject jobInstance = restUtils.submitJob("batchFAT", "splitflow", props, BatchRestUtils.BATCH_BASE_URL);
        String jobName = jobInstance.getString("jobName");

        long instanceId = jobInstance.getJsonNumber("instanceId").longValue();

        JsonObject jobExecution = restUtils.waitForFirstJobExecution(instanceId, BatchRestUtils.BATCH_BASE_URL);

        long executionId = jobExecution.getJsonNumber("executionId").longValue();

        jobInstance = restUtils.waitForJobInstanceToFinish(jobInstance.getJsonNumber("instanceId").longValue(), BatchRestUtils.BATCH_BASE_URL);

        assertEquals("FAILED", jobInstance.getString("batchStatus"));
        logVerificationPoint("Verifying directory structure and job log presence");
        File joblogsDir = new File(server.getLogsRoot() + File.separator + "joblogs");
        assertTrue("Could not find " + joblogsDir.getAbsolutePath(), joblogsDir.exists());

        File jobDir = new File(joblogsDir, jobName);
        assertTrue("Could not find " + jobDir.getAbsolutePath(), jobDir.exists());

        File dateDir = new File(jobDir, curDate);
        assertTrue("Could not find " + dateDir.getAbsolutePath(), dateDir.exists());

        File instanceDir = new File(dateDir, "instance." + instanceId);
        assertTrue("Could not find " + instanceDir.getAbsolutePath(), instanceDir.exists());

        File execDir = new File(instanceDir, "execution." + executionId);
        assertTrue("Could not find " + execDir.getAbsolutePath(), execDir.exists());

        logVerificationPassed();

        File joblogFile = new File(execDir, "part.1.log");

        verifyLogFile(joblogFile);

        logVerificationPoint("Verifying exception message is present");
        RemoteFile logFile = new RemoteFile(Machine.getLocalMachine(), joblogFile.getAbsolutePath());

        BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0034I: An exception occurred");
        BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, traceFile, "CWWKY0034I: An exception occurred");
        BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, messagesLogFile, "CWWKY0034I: An exception occurred");
        BatchJoblogFatUtils.verifyExceptionMessageNotPresent(server, consoleLogFile, "CWWKY0034I: An exception occurred");

        BatchJoblogFatUtils.verifyExceptionMessage(server, logFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, traceFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, messagesLogFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");
        BatchJoblogFatUtils.verifyExceptionMessage(server, consoleLogFile, "CWWKY0011W: Job " + jobName + " failed with batch status FAILED");

        logVerificationPassed();

        logStepCompleted();

    }

    @Test
    public void testChunkJobLogging() throws Exception {
        curMethod = "testChunkJobLogging()";

        String output = callServlet("chunk");

        // Need to do this here to narrow a timing window where the date could have changed.
        curDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String[] params = output.substring(output.indexOf("jobName=")).split(" ");
        String jobName = params[0].substring(params[0].indexOf("=") + 1);
        int instanceID = Integer.parseInt(params[1].substring(params[1].indexOf("=") + 1));
        int execID = Integer.parseInt(params[2].substring(params[2].indexOf("=") + 1));

        logStep("Verifying directory structure for chunk processing");
        File execDir = new File(server.getLogsRoot() + File.separator
                                + "joblogs" + File.separator
                                + jobName + File.separator
                                + curDate + File.separator
                                + "instance." + instanceID + File.separator
                                + "execution." + execID);

        assertTrue("Could not find " + execDir.getAbsolutePath(),
                   execDir.exists());

        File logFile = new File(execDir.getAbsolutePath() + File.separator + "part.1.log");
        verifyLogFile(logFile);

        BatchJoblogFatUtils.verifyChunkMessages(server, new RemoteFile(Machine.getLocalMachine(), logFile.getAbsolutePath()), step1);
        BatchJoblogFatUtils.verifyChunkMessagesNotPresent(server, traceFile, step1);
        BatchJoblogFatUtils.verifyChunkMessagesNotPresent(server, messagesLogFile, step1);
        BatchJoblogFatUtils.verifyChunkMessagesNotPresent(server, consoleLogFile, step1);

        logStepCompleted();
    }

    private void verifyLogFile(File file) {
        logVerificationPoint("Verifying " + file.getAbsolutePath());
        assertTrue("File does not exist: " + file.getAbsolutePath(),
                   file.exists());
        assertTrue("Log file is empty: " + file.getAbsolutePath(),
                   file.length() > 0);
        logVerificationPassed();
    }

    private String callServlet(String jslName) throws IOException {
        logStep("Calling servlet for jsl " + jslName);

        HttpURLConnection con = getConnection("/" + appName + "?jsl=" + jslName,
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              adminHeaderMap);

        BufferedReader br = HttpUtils.getConnectionStream(con);

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();

        String output = response.toString();

        logVerificationPoint("Verifying success in servlet response");
        assertNotNull(output);
        assertNotNull(output.trim());
        assertTrue("' appname:'" + appName + "' output:'" + output + "' testUri:'" + _testName + "'",
                   output.trim().contains(SUCCESS_MESSAGE));
        logVerificationPassed();
        logStepCompleted();

        return output;
    }

    private void logStep(String msg) {
        Log.info(c, curMethod, "===== Step started: " + msg + " =====");
    }

    private void logStepCompleted() {
        Log.info(c, curMethod, "===== Step completed! =====");
    }

    private void logVerificationPoint(String msg) {
        Log.info(c, curMethod, "== Verification point: " + msg);
    }

    private void logVerificationPassed() {
        Log.info(c, curMethod, "== Verification passed!");
    }

    private static String getPort() {
        return System.getProperty("HTTP_default.secure", "8020");
    }

    private static URL getURL(String path) throws MalformedURLException {
        URL myURL = new URL("https://localhost:" + getPort() + path);
        System.out.println("Built URL: " + myURL.toString());
        return myURL;
    }

    protected static HttpURLConnection getConnection(String path, int expectedResponseCode, HTTPRequestMethod method, InputStream streamToWrite,
                                                     Map<String, String> map) throws IOException {
        return HttpUtils.getHttpConnection(getURL(path), expectedResponseCode, new int[0], TIMEOUT, method, map, streamToWrite);
    }

}
