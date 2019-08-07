This project creates the DbServletApp servlet and other test utilities
for use by jbatch FATs.

The DbServlet can be used to execute arbitrary SQL on a given datasource
on the test server, e.g. to build the batch runtime tables.

This is not a shipped project.

BonusPayout Notes:

For the validation step, we have a very specific value used for Exit Status and persistent user data,
and we validate the values produced for these by the job against the number of records originally specified
as input via job parameters. 

Values:
  
   ExitStatus and Persistent UserData are now both set to the # of records cumulatively processed across all executions.
   And..for the partitioned steps, we have a wrapper object containing other data like the # of completed partitions, along
   with # of records cumulatively processed across all executions for this partition)
   
   There is no current value reflecting the number of records processed on just this execution.  This facilitates validation
   of the partitioned steps, since the analyzer may have missed certain calls from the collector (if the top-level server died during the call), making it not currently easy/possible
   to validate this upon a restart.
