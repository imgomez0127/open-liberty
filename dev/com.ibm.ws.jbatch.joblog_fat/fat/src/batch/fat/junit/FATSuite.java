/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package batch.fat.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({
               BatchJoblogTest.class,
               BatchJoblogConfig1Test.class,
               BatchJoblogConfig2Test.class,
               BatchJoblogConfig3Test.class,
               BatchJoblogConfig4Test.class,
               BatchJoblogAllTraceTest.class,
})
public class FATSuite {}
