/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package batch.fat.artifacts;

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

/**
 * For testing that j.u.l Loggers can be routed directly to the job log.
 */
public class JavaUtilLoggingBatchlet extends AbstractBatchlet {

    private static final Logger logger = Logger.getLogger(JavaUtilLoggingBatchlet.class.getName());

//    private static final Logger parentLogger = Logger.getLogger("batch.fat.artifacts");
//    private static final Logger grandParentLogger = Logger.getLogger("batch.fat");
//
//    private static final Logger batchJobLogger = Logger.getLogger("com.ibm.ws.batch.JobLogger");

    @Inject
    JobContext ctx;

    @Override
    public String process() throws Exception {

        logger.info("JavaUtilLoggingBatchlet.process: info level message: Logger: " + getLoggerInfo(logger));
//        logger.info("JavaUtilLoggingBatchlet.process: info level message: Parent Logger: " + getLoggerInfo(parentLogger));
//        logger.info("JavaUtilLoggingBatchlet.process: info level message: GrandParent Logger: " + getLoggerInfo(grandParentLogger));
//        logger.info("JavaUtilLoggingBatchlet.process: info level message: Batch JobLogger: " + getLoggerInfo(batchJobLogger));
        logger.fine("JavaUtilLoggingBatchlet.process: fine level message ");
        logger.finest("JavaUtilLoggingBatchlet.process: finest level message");

        // These will never end up in the job log since it's impossible for
        // the batch container to intercept System.out/err.
        System.out.println("JavaUtilLoggingBatchlet.process: System.out message: Logger: " + getLoggerInfo(logger));
//        System.out.println("JavaUtilLoggingBatchlet.process: System.out message: Parent Logger: " + getLoggerInfo(parentLogger));
//        System.out.println("JavaUtilLoggingBatchlet.process: System.out message: GrandParent Logger: " + getLoggerInfo(grandParentLogger));
//        System.out.println("JavaUtilLoggingBatchlet.process: System.out message: Batch JobLogger: " + getLoggerInfo(batchJobLogger));
        System.err.println("JavaUtilLoggingBatchlet.process: System.err message");

        ctx.setExitStatus(SimpleBatchlet.SUCCESS_VAL);

        return "DONE";
    }

    /**
     * @return a String containing details about the given Logger.
     */
    private String getLoggerInfo(Logger logger) {
        return "name=" + logger.getName() + ";level=" + logger.getLevel() + ";useParentHandlers=" + logger.getUseParentHandlers();
    }

}
