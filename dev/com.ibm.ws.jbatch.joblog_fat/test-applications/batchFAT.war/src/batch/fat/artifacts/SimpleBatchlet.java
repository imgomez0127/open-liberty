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

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.jul.JULAppender;

/**
 *
 */
public class SimpleBatchlet extends AbstractBatchlet {

    @Inject
    @BatchProperty(name = "forceFailure")
    String forceFailure;

    @Inject
    JobContext ctx;

    public final static String SUCCESS_VAL = "SUCCESS:  BATCH BVT";

    @Override
    public String process() throws Exception {
        Logger logger = Logger.getLogger(SimpleBatchlet.class.getName());
        BasicConfigurator.configure(new JULAppender());
        NDC.push("BATCHLET");

        logger.info("**** BATCHLET ENTRY (LOGGER)");
        logger.info("*** LOG4J SUCCESS ***");

        if (Boolean.parseBoolean(forceFailure)) {
            throw new AppException();
        }
        ctx.setExitStatus(SUCCESS_VAL);

        BasicConfigurator.resetConfiguration();

        return "DONE";
    }

}
