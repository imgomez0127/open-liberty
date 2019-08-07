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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PartitionBatchlet extends AbstractBatchlet {

    @Inject
    @BatchProperty(name = "forceFailure")
    String forceFailure;

    @Inject
    JobContext ctx;

    public final static String SUCCESS_VAL = "SUCCESS:  BATCH BVT";
    private int count = 1;

    @Override
    public String process() throws Exception {
        Logger logger = LoggerFactory.getLogger(PartitionBatchlet.class.getName());

        logger.info("**** BATCHLET HIT COUNT: " + count + " threadid: " + Thread.currentThread().getId());
        count++;
        logger.info("**** BATCHLET UPDATED COUNT: " + count + " threadid: " + Thread.currentThread().getId());

        if (Boolean.parseBoolean(forceFailure)) {
            throw new AppException();
        }

        ctx.setExitStatus(SUCCESS_VAL);
        return "DONE";
    }
}
