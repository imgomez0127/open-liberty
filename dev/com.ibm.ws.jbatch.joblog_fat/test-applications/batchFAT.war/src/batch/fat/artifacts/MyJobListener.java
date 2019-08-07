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

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.inject.Inject;

/**
 *
 */
public class MyJobListener extends AbstractJobListener {

    @Inject
    @BatchProperty(name = "beforeJobException")
    String beforeJobException = null;

    @Inject
    @BatchProperty(name = "afterJobException")
    String afterJobException = null;

    @Override
    public void beforeJob() throws Exception {
        boolean toThrowBeforeJob = Boolean.parseBoolean(beforeJobException);
        if (toThrowBeforeJob) {
            throw new AppException();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.batch.api.listener.JobListener#afterJob()
     */
    @Override
    public void afterJob() throws Exception {

        // OK to start checking that we're done now
        Object lock = MyJobListener.class;
        synchronized (lock) {
            lock.notifyAll();
        }
        boolean toThrowAfterJob = Boolean.parseBoolean(afterJobException);
        if (toThrowAfterJob) {
            throw new AppException();
        }
    }

}
