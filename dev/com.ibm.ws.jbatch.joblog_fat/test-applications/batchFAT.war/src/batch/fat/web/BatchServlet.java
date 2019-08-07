/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package batch.fat.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.SimpleBatchlet;

@WebServlet(name = "BatchBVTServlet", urlPatterns = { "/*" })
@ServletSecurity(value = @HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL),
                 httpMethodConstraints = { @HttpMethodConstraint(value = "POST", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT),
                                          @HttpMethodConstraint(value = "GET", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT),
                                          @HttpMethodConstraint(value = "PUT", emptyRoleSemantic = ServletSecurity.EmptyRoleSemantic.PERMIT) })
public class BatchServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -189207824014358889L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();

        String jsl = req.getParameter("jsl");

        executeJobAndWaitToCompletion(pw, jsl);
    }

    private void executeJobAndWaitToCompletion(PrintWriter pw, String jslName) {

        //
        // 1. Start job
        // 
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long execID = jobOperator.start(jslName, null);

        // 
        // 2. Wait for notify from end of job job listener
        // 
        Class thisClass = batch.fat.artifacts.MyJobListener.class;
        synchronized (thisClass) {
            try {
                thisClass.wait(60 * 1000);
            } catch (InterruptedException e) {
                pw.println("ERROR: InterruptedException message " + e.getMessage());
                return;
            }
        }

        // 
        // 3. We shouldn't have to wait long now, but will still try three times in case we just miss 
        // the status transitioning to COMPLETED
        // 
        int numTries = 3;
        for (int i = 0; i < numTries; i++) {
            JobExecution jobExec = jobOperator.getJobExecution(execID);
            if (jobExec == null) {
                pw.println("ERROR: Null JobExecution found");
                return;
            }
            // Verify batch status
            BatchStatus status = jobExec.getBatchStatus();
            if (status == BatchStatus.COMPLETED) {
                // Verify exit status
                String es = jobExec.getExitStatus();
                if (SimpleBatchlet.SUCCESS_VAL.equals(es) || es.equals("COMPLETED")) {
                    pw.println("TEST PASSED");
                    pw.println("jobName=" + jobExec.getJobName() +
                               " instanceID=" + jobOperator.getJobInstance(execID).getInstanceId() +
                               " execID=" + execID + "  ");
                    return;
                } else {
                    pw.println("ERROR: Got unexpected exit status: " + es);
                    return;
                }
            } else if (status == BatchStatus.STARTED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    pw.println("ERROR: InterruptedException message " + e.getMessage());
                    return;
                }
            } else {
                pw.println("ERROR: Job executed but resulted in bad status: " + status);
                return;
            }
        }
        pw.println("ERROR: timed out waiting for job completion");
        return;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
        resp.setStatus(200);
    }

}
