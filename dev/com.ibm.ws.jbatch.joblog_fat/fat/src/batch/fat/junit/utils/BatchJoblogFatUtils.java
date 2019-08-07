/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package batch.fat.junit.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 * Various utils used by jbatch.joblog_fat.
 */
public class BatchJoblogFatUtils {

    /**
     * Assert that the given file exists and is not empty.
     */
    public static void verifyLogFile(File file) {
        assertTrue("File does not exist: " + file.getAbsolutePath(), file.exists());
        assertTrue("Log file is empty: " + file.getAbsolutePath(), file.length() > 0);
    }

    /**
     * @return the contents of the given BufferedReader as a String.
     */
    public static String readResponse(BufferedReader br) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();
        return response.toString();
    }

    /**
     * Call the servlet and return its response.
     */
    public static String callServlet(LibertyServer server, String path, String jslName) throws IOException {
        log("callServlet", "Calling servlet for jsl " + jslName);

        Map<String, String> adminHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode("bob:bobpwd"));

        HttpURLConnection con = HttpUtils.getHttpConnection(buildUrl(server, "/" + path + "?jsl=" + jslName),
                                                            HttpURLConnection.HTTP_OK,
                                                            new int[0],
                                                            10 * 1000,
                                                            HTTPRequestMethod.GET,
                                                            adminHeaderMap,
                                                            null);

        String output = readResponse(HttpUtils.getConnectionStream(con));
        log("callServlet", "response: " + output);

        assertNotNull(output);

        return output;
    }

    public static void verifyExceptionMessage(LibertyServer server, RemoteFile logFile, String message) throws Exception {

        Thread.sleep(3000); // Defect 150611: it's possible for the job thread cleanup to still be running after we're notified of job completion

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, message, 1, logFile);
        assertFalse("Job exception message was not found", numMatches == 0);

    }

    public static void verifyExceptionMessageNotPresent(LibertyServer server, RemoteFile logFile, String message) throws Exception {

        Thread.sleep(3000); // Defect 150611: it's possible for the job thread cleanup to still be running after we're notified of job completion

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, message, 1, logFile);
        assertTrue("Job exception message was found", numMatches == 0);

    }

    public static void verifyJobMessages(LibertyServer server, RemoteFile logFile) throws Exception {
        Thread.sleep(3000); // Defect 150611: it's possible for the job thread cleanup to still be running after we're notified of job completion

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, "Started invoking execution for a job", 1, logFile);
        assertTrue("Job starting message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0009I: Job", 1, logFile);
        assertTrue("Job started NLS message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0009I: Job", 1, logFile);
        assertTrue("Job ended NLS message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "Job Batch Status = COMPLETED", 1, logFile);
        assertTrue("Job completed message was not found", numMatches == 1);

    }

    public static void verifyJobMessagesNotPresent(LibertyServer server, RemoteFile logFile) throws Exception {
        Thread.sleep(3000); // Defect 150611: it's possible for the job thread cleanup to still be running after we're notified of job completion

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, "Started invoking execution for a job", 1, logFile);
        assertTrue("Job started message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0009I: Job", 1, logFile);
        assertTrue("Job started NLS message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0009I: Job", 1, logFile);
        assertTrue("Job ended NLS message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "Job Batch Status = COMPLETED", 1, logFile);
        assertTrue("Job completed message was found", numMatches == 0);

    }

    public static void verifyStepMessages(LibertyServer server, RemoteFile logFile, String stepName) throws Exception {

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0018I: Step " + stepName + " started for job instance", 1, logFile);
        assertTrue("Step 1 started message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0020I: Step " + stepName + " ended with batch status COMPLETED and exit status", 1, logFile);
        assertTrue("Step 1 ended message was not found", numMatches == 1);

    }

    public static void verifyStepMessagesNotPresent(LibertyServer server, RemoteFile logFile, String stepName) throws Exception {

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0018I: Step " + stepName + " started for job instance", 1, logFile);
        assertTrue("Step 1 started message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0020I: Step " + stepName + " ended with batch status COMPLETED and exit status", 1, logFile);
        assertTrue("Step 1 ended message was found", numMatches == 0);

    }

    public static void verifyChunkMessages(LibertyServer server, RemoteFile logFile, String stepName) throws Exception {

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0026I: The item reader for step " + stepName + " was opened.", 1, logFile);
        assertTrue("Reader open message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0028I: The item writer for step " + stepName + " was opened.", 1, logFile);
        assertTrue("Writer open message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0027I: The item reader for step " + stepName + " was closed.", 1, logFile);
        assertTrue("Reader close message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0029I: The item writer for step " + stepName + " was closed.", 1, logFile);
        assertTrue("Writer close message was not found", numMatches == 1);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0022I: The chunk successfully ended for step " + stepName, 1000, logFile);
        assertTrue("Chunk ended messages were not found", numMatches == 6);

    }

    public static void verifyChunkMessagesNotPresent(LibertyServer server, RemoteFile logFile, String stepName) throws Exception {

        int numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0026I: The item reader for step " + stepName + " was opened.", 1, logFile);
        assertTrue("Reader open message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0028I: The item writer for step " + stepName + " was opened.", 1, logFile);
        assertTrue("Writer open message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0027I: The item reader for step " + stepName + " was closed.", 1, logFile);
        assertTrue("Reader close message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0029I: The item writer for step " + stepName + " was closed.", 1, logFile);
        assertTrue("Writer close message was found", numMatches == 0);

        numMatches = server.waitForMultipleStringsInLogUsingMark(100, "CWWKY0022I: The chunk successfully ended for step " + stepName, 1, logFile);
        assertTrue("Chunk ended messages were found", numMatches == 0);

    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, Object msg) {
        Log.info(BatchJoblogFatUtils.class, method, String.valueOf(msg));
    }

    public static URL buildUrl(LibertyServer server, String path) throws MalformedURLException {
        URL retMe = new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + path);
        log("buildUrl", retMe.toString());
        return retMe;
    }

    /**
     * @return today's date in "yyyy-MM-dd" format.
     */
    public static String getCurrentDateString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    /**
     * @param server
     * @param logFile
     *
     * @return true if JobLogger trace is found in the given file.
     */
    public static boolean findJobLoggerFineTrace(LibertyServer server, RemoteFile logFile) throws Exception {
        return isTraceInFile("CWWKY0009I: Job", server, logFile);
    }

    /**
     * @param server
     * @param logFile
     *
     * @return true if JavaUtilLoggingBatchlet trace is found in the given file.
     */
    public static boolean findBatchletAllTrace(LibertyServer server, RemoteFile logFile) throws Exception {

        return isTraceInFile("JavaUtilLoggingBatchlet.process: info level message", server, logFile)
               && isTraceInFile("JavaUtilLoggingBatchlet.process: fine level message", server, logFile)
               && isTraceInFile("JavaUtilLoggingBatchlet.process: finest level message", server, logFile);
    }

    /**
     * @param server
     * @param logFile
     *
     * @return true if JavaUtilLoggingBatchlet System.out trace is found in the given file.
     */
    public static boolean findBatchletSystemOutTrace(LibertyServer server, RemoteFile logFile) throws Exception {
        return isTraceInFile("JavaUtilLoggingBatchlet.process: System.out message", server, logFile)
               & isTraceInFile("JavaUtilLoggingBatchlet.process: System.err message", server, logFile);
    }

    /**
     * @param server
     * @param jobname
     * @param instanceID
     * @param execID
     *
     * @return The first job log part (part.1.log).
     */
    public static File verifyJoblogsExist(LibertyServer server, String jobName, int instanceID, int execID) {
        File joblogsDir = new File(server.getLogsRoot() + File.separator + "joblogs");
        assertTrue("Could not find " + joblogsDir.getAbsolutePath(), joblogsDir.exists());

        File jobDir = new File(joblogsDir, jobName);
        assertTrue("Could not find " + jobDir.getAbsolutePath(), jobDir.exists());

        File dateDir = new File(jobDir, BatchJoblogFatUtils.getCurrentDateString());
        assertTrue("Could not find " + dateDir.getAbsolutePath(), dateDir.exists());

        File instanceDir = new File(dateDir, "instance." + instanceID);
        assertTrue("Could not find " + instanceDir.getAbsolutePath(), instanceDir.exists());

        File execDir = new File(instanceDir, "execution." + execID);
        assertTrue("Could not find " + execDir.getAbsolutePath(), execDir.exists());

        File joblogFile = new File(execDir, "part.1.log");
        return joblogFile;
    }

    /**
     * @param msg
     * @param server
     * @param logFile
     *
     * @return true if the given msg is in the given logFile.
     */
    public static boolean isTraceInFile(String msg, LibertyServer server, RemoteFile logFile) throws Exception {
        return !server.findStringsInLogs(msg, logFile).isEmpty();
    }

    /*
     * Change the schema value to user1 in case of a oracle database,
     * or dbuser1 in the case of a sql server database
     */
    public static void updateSchemaIfNecessary(LibertyServer server) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        Bootstrap bs = Bootstrap.getInstance();
        String dbType = bs.getValue(BootstrapProperty.DB_VENDORNAME.getPropertyName());
        if (dbType != null && (dbType.equalsIgnoreCase("oracle") || dbType.equalsIgnoreCase("sqlserver"))) {

            String user1 = bs.getValue(BootstrapProperty.DB_USER1.getPropertyName());
            for (DatabaseStore ds : config.getDatabaseStores()) {
                ds.setSchema(user1);
            }
        }

        server.updateServerConfiguration(config);
    }

}
