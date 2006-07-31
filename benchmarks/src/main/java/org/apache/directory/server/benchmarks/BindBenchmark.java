/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.benchmarks;


import java.util.ArrayList;
import java.util.Date;

import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPException;

import com.sun.slamd.job.JobClass;
import com.sun.slamd.job.UnableToRunException;
import com.sun.slamd.parameter.BooleanParameter;
import com.sun.slamd.parameter.IntegerParameter;
import com.sun.slamd.parameter.InvalidValueException;
import com.sun.slamd.parameter.Parameter;
import com.sun.slamd.parameter.ParameterList;
import com.sun.slamd.parameter.PasswordParameter;
import com.sun.slamd.parameter.PlaceholderParameter;
import com.sun.slamd.parameter.StringParameter;
import com.sun.slamd.stat.IncrementalTracker;
import com.sun.slamd.stat.RealTimeStatReporter;
import com.sun.slamd.stat.StatTracker;
import com.sun.slamd.stat.TimeTracker;


/**
 * A simple bind benchmark.  Here the same bindDn and password is 
 * used to bind to the directory.  The connection to the directory is
 * by default shared across iterations of a thread but this can be 
 * changed.  This is not a real world experiment but a way for us to
 * stress test the server, profile it, optimize it and regression test
 * our results under stress for more reliable feedback.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindBenchmark extends JobClass
{
    /**
     * The name of the stat tracker that will be used to count the number of
     * authentication attempts.
     */
    public static final String STAT_TRACKER_AUTHENTICATION_ATTEMPTS = "Authentication Attempts";

    /**
     * The name of the stat tracker that will be used to keep track of the time 
     * required to perform each authentication.
     */
    public static final String STAT_TRACKER_AUTHENTICATION_TIME = "Authentication Time";

    /**
     * The name of the stat tracker that will be used to count the number of
     * failed authentications.
     */
    public static final String STAT_TRACKER_FAILED_AUTHENTICATIONS = "Failed Authentications";

    /**
     * The name of the stat tracker that will be used to count the number of
     * successful authentications.
     */
    public static final String STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS = "Successful Authentications";

    // -----------------------------------------------------------------------
    // Extracted paramter values for the whole Job
    // -----------------------------------------------------------------------

    // The connection is estabilished anew every time for each bind request
    // if this option is not selected instead of reusing the existing connection
    // for each iteration.  The default is to share the connection.
    static boolean shareConnections;
    
    // Indicates whether bind failures because of invalid credentials will be
    // ignored (so we can optimize for authn failures as well).
    static boolean ignoreInvalidCredentials;

    // The maximum number of iterations per thread.
    static int iterations;

    // The maximum length of time that any single LDAP operation will be allowed
    // to take before it is cancelled.
    static int timeLimit;

    // The time to start working before beginning statistics collection.
    static int warmUpTime;

    // The delay in milliseconds between authentication attempts.
    static long delay;

    // The port number of the directory server.
    static int directoryPort;

    // The address of the directory server.
    static String directoryHost;

    // The DN to use to bind to the directory 
    static String bindDN;

    // The password for the bind DN.
    static String bindPW;

    // -----------------------------------------------------------------------
    // Paramters definitions
    // -----------------------------------------------------------------------

    // The parameter controlling where or not connections are reused
    BooleanParameter shareConnectionsParameter = new BooleanParameter( "shareConnectionsParameter",
        "Share Connections", 
        "Specifies whether or not the connection to the LDAP server is shared or " +
        "re-estabilished every time for each iteration.  If true the iteration creates" +
        "and destroys a new connection before issueing a bind request.", true );
    
    // The parameter that indicates the delay that should be used between each
    // authentication attempt.
    IntegerParameter delayParameter = new IntegerParameter( "delay", "Time Between Authentications (ms)",
        "Specifies the length of time in milliseconds " + "each thread should wait between authentication "
            + "attempts.  Note that this delay will be " + "between the starts of consecutive attempts and "
            + "not between the end of one attempt and the " + "beginning of the next.  If an authentication "
            + "takes longer than this length of time, then " + "there will be no delay.", true, 0, true, 0, false, 0 );

    // The parameter that indicates the number of iterations to perform.
    IntegerParameter iterationsParameter = new IntegerParameter( "num_iterations", "Number of Iterations",
        "The number of authentications that should be " + "performed by each thread", false, -1 );

    // The parameter used to indicate the maximum length of time that any single
    // LDAP operation will be allowed to take.
    IntegerParameter timeLimitParameter = new IntegerParameter( "time_limit", "Operation Time Limit",
        "The maximum length of time in seconds that any " + "single LDAP operation will be allowed to take "
            + "before it is cancelled.", true, 0, true, 0, false, 0 );

    // The parmeter that specifies the cool-down time in seconds.
    IntegerParameter warmUpParameter = new IntegerParameter( "warm_up", "Warm Up Time",
        "The time in seconds that the job should " + "search before beginning statistics collection.", true, 0, true,
        0, false, 0 );

    // The placeholder parameter used as a spacer in the admin interface.
    PlaceholderParameter placeholder = new PlaceholderParameter();

    // The parameter used to indicate the port number for the directory server.
    IntegerParameter portParameter = new IntegerParameter( "ldap_port", "Directory Server Port",
        "The port number for the directory server.", true, 389, true, 1, true, 65535 );

    // The parameter used to indicate the address of the directory server.
    StringParameter hostParameter = new StringParameter( "ldap_host", "Directory Server Address",
        "The address for the directory server.", true, "" );

    // The parameter used to indicate the bind DN.
    StringParameter bindDNParameter = new StringParameter( "binddn", "Directory Bind DN",
        "The DN to use when binding to the directory server.", false, "" );

    // The parameter used to indicate the bind DN.
    PasswordParameter bindPWParameter = new PasswordParameter( "bindpw", "Bind Password",
        "The password to use when binding.", false, "" );

    // -----------------------------------------------------------------------
    // Stat trakers for each thread.
    // -----------------------------------------------------------------------

    // The stat tracker that will count the # of authentication attempts.
    IncrementalTracker attemptCounter;

    // The stat tracker that will count the # of failed authentications.
    IncrementalTracker failureCounter;

    // The stat tracker that will count the # of successful authentications.
    IncrementalTracker successCounter;

    // The stat tracker that will time each authentication.
    TimeTracker authTimer;

    // -----------------------------------------------------------------------
    // Connection and other parameters for each thread
    // -----------------------------------------------------------------------

    // The LDAP connection that will be used for bind operations by this thread.
    LDAPConnection bindConnection;

    // The set of constraints that will be used for bind operations.
    LDAPConstraints bindConstraints;


    public String getJobDescription()
    {
        return "Does a bind using a single user name then immediately unbinds.";
    }


    public String getJobName()
    {
        return "Bind/Unbind Optimization Test";
    }


    public String getJobCategoryName()
    {
        return "ApacheDS Optimization Tests";
    }


    /**
     * Returns the set of parameters whose value may be specified by the end user.
     *
     * @return  The set of configurable parameters for this job class.
     */
    public ParameterList getParameterStubs()
    {
        Parameter[] parameterArray = new Parameter[]
            { placeholder, hostParameter, portParameter, bindDNParameter, bindPWParameter, placeholder,
                warmUpParameter, timeLimitParameter, delayParameter, placeholder,
                iterationsParameter, shareConnectionsParameter };

        return new ParameterList( parameterArray );
    }


    public StatTracker[] getStatTrackerStubs( String clientID, String threadID, int collectionInterval )
    {
        return new StatTracker[]
            {
                new IncrementalTracker( clientID, threadID, STAT_TRACKER_AUTHENTICATION_ATTEMPTS, collectionInterval ),
                new IncrementalTracker( clientID, threadID, STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS, collectionInterval ),
                new IncrementalTracker( clientID, threadID, STAT_TRACKER_FAILED_AUTHENTICATIONS, collectionInterval ),
                new TimeTracker( clientID, threadID, STAT_TRACKER_AUTHENTICATION_TIME, collectionInterval ) };
    }


    public StatTracker[] getStatTrackers()
    {
        return new StatTracker[]
            { attemptCounter, successCounter, failureCounter, authTimer };
    }


    public void validateJobInfo( int numClients, int threadsPerClient, int threadStartupDelay, Date startTime,
        Date stopTime, int duration, int collectionInterval, ParameterList parameters ) throws InvalidValueException
    {
        // might want to add something here later
    }


    public boolean providesParameterTest()
    {
        return true;
    }


    /**
     * Provides a means of testing the provided job parameters to determine
     * whether they are valid (e.g., to see if the server is reachable) before
     * scheduling the job for execution.  This method will be executed by the
     * SLAMD server system itself and not by any of the clients.
     *
     * @param  parameters      The job parameters to be tested.
     * @param  outputMessages  The lines of output that were generated as part of
     *                         the testing process.  Each line of output should
     *                         be added to this list as a separate string, and
     *                         empty strings (but not <CODE>null</CODE> values)
     *                         are allowed to provide separation between
     *                         different messages.  No formatting should be
     *                         provided for these messages, however, since they
     *                         may be displayed in either an HTML or plain text
     *                         interface.
     *
     * @return  <CODE>true</CODE> if the test completed successfully, or
     *          <CODE>false</CODE> if not. 
     */
    public boolean testJobParameters( ParameterList parameters, ArrayList outputMessages )
    {
        // Get all the parameters that we might need to perform the test.
        StringParameter hostParam = parameters.getStringParameter( hostParameter.getName() );
        if ( ( hostParam == null ) || ( !hostParam.hasValue() ) )
        {
            outputMessages.add( "ERROR:  No directory server address was provided." );
            return false;
        }
        String host = hostParam.getStringValue();

        IntegerParameter portParam = parameters.getIntegerParameter( portParameter.getName() );
        if ( ( portParam == null ) || ( !hostParam.hasValue() ) )
        {
            outputMessages.add( "ERROR:  No directory server port was provided." );
            return false;
        }
        int port = portParam.getIntValue();

        String bindDN = "";
        StringParameter bindDNParam = parameters.getStringParameter( bindDNParameter.getName() );
        if ( ( bindDNParam != null ) && bindDNParam.hasValue() )
        {
            bindDN = bindDNParam.getStringValue();
        }

        String bindPassword = "";
        PasswordParameter bindPWParam = parameters.getPasswordParameter( bindPWParameter.getName() );
        if ( ( bindPWParam != null ) && bindPWParam.hasValue() )
        {
            bindPassword = bindPWParam.getStringValue();
        }

        // Create the LDAPConnection object that we will use to communicate with the directory server.
        LDAPConnection conn = new LDAPConnection();

        // Attempt to establish a connection to the directory server.
        try
        {
            outputMessages.add( "Attempting to establish a connection to " + host + ":" + port + "...." );
            conn.connect( host, port );
            outputMessages.add( "Connected successfully." );
            outputMessages.add( "" );
        }
        catch ( Exception e )
        {
            outputMessages.add( "ERROR:  Unable to connect to the directory " + "server:  " + stackTraceToString( e ) );
            return false;
        }

        // Attempt to bind to the directory server using the bind DN and password.
        try
        {
            outputMessages.add( "Attempting to perform an LDAPv3 bind to the " + "directory server with a DN of '"
                + bindDN + "'...." );
            conn.bind( 3, bindDN, bindPassword );
            outputMessages.add( "Bound successfully." );
            outputMessages.add( "" );
        }
        catch ( Exception e )
        {
            try
            {
                conn.disconnect();
            }
            catch ( Exception e2 )
            {
            }

            outputMessages.add( "ERROR:  Unable to bind to the directory server:  " + stackTraceToString( e ) );
            return false;
        }

        // At this point, all tests have passed.  Close the connection and return true.
        try
        {
            conn.disconnect();
        }
        catch ( Exception e )
        {
        }

        outputMessages.add( "All tests completed successfully." );
        return true;
    }


    /**
     * Performs initialization for this job on each client immediately before each
     * thread is created to actually run the job.
     *
     * @param  clientID    The ID assigned to the client running this job.
     * @param  parameters  The set of parameters provided to this job that can be
     *                     used to customize its behavior.
     *
     * @throws  UnableToRunException  If the client initialization could not be
     *                                completed successfully and the job is unable
     *                                to run.
     */
    public void initializeClient( String clientID, ParameterList parameters ) throws UnableToRunException
    {
        // Get the shareConnections boolean parameter
        shareConnectionsParameter = parameters.getBooleanParameter( shareConnectionsParameter.getName() );
        if ( hostParameter == null )
        {
            shareConnections = true; // the default
        }
        else
        {
            shareConnections = shareConnectionsParameter.getBooleanValue();
        }
        
        
        // Get the directory server address
        hostParameter = parameters.getStringParameter( hostParameter.getName() );
        if ( hostParameter == null )
        {
            throw new UnableToRunException( "No directory server host provided." );
        }
        else
        {
            directoryHost = hostParameter.getStringValue();
        }

        // Get the directory server port
        portParameter = parameters.getIntegerParameter( portParameter.getName() );
        if ( portParameter != null )
        {
            directoryPort = portParameter.getIntValue();
        }

        // Get the DN to use to bind to the directory server.
        bindDNParameter = parameters.getStringParameter( bindDNParameter.getName() );
        if ( bindDNParameter == null )
        {
            bindDN = "";
        }
        else
        {
            bindDN = bindDNParameter.getStringValue();
        }

        // Get the password to use to bind to the directory server.
        bindPWParameter = parameters.getPasswordParameter( bindPWParameter.getName() );
        if ( bindPWParameter == null )
        {
            bindPW = "";
        }
        else
        {
            bindPW = bindPWParameter.getStringValue();
        }

        // Get the warm up time.
        warmUpTime = 0;
        warmUpParameter = parameters.getIntegerParameter( warmUpParameter.getName() );
        if ( warmUpParameter != null )
        {
            warmUpTime = warmUpParameter.getIntValue();
        }

        // Get the max operation time limit.
        timeLimitParameter = parameters.getIntegerParameter( timeLimitParameter.getName() );
        if ( timeLimitParameter != null )
        {
            timeLimit = timeLimitParameter.getIntValue();
        }

        // Get the delay between authentication attempts.
        delay = 0;
        delayParameter = parameters.getIntegerParameter( delayParameter.getName() );
        if ( delayParameter != null )
        {
            delay = delayParameter.getIntValue();
        }

        // Get the number of iterations to perform.
        iterations = -1;
        iterationsParameter = parameters.getIntegerParameter( iterationsParameter.getName() );
        if ( ( iterationsParameter != null ) && ( iterationsParameter.hasValue() ) )
        {
            iterations = iterationsParameter.getIntValue();
        }
    }


    public void initializeThread( String clientID, String threadID, int collectionInterval, ParameterList parameters )
        throws UnableToRunException
    {
        if ( shareConnections )
        {
            bindConnection = new LDAPConnection();
    
            try
            {
                bindConnection.connect( 3, directoryHost, directoryPort, "", "" );
            }
            catch ( Exception e )
            {
                throw new UnableToRunException( "Unable to establish the connections " + "to the directory server:  " + e,
                    e );
            }
    
            // Initialize the constraints.
            bindConstraints = bindConnection.getConstraints();
            bindConstraints.setTimeLimit( 1000 * timeLimit );
        }
    
        // Create the stat trackers.
        attemptCounter = new IncrementalTracker( clientID, threadID, STAT_TRACKER_AUTHENTICATION_ATTEMPTS,
            collectionInterval );
        successCounter = new IncrementalTracker( clientID, threadID, STAT_TRACKER_SUCCESSFUL_AUTHENTICATIONS,
            collectionInterval );

        failureCounter = new IncrementalTracker( clientID, threadID, STAT_TRACKER_FAILED_AUTHENTICATIONS,
            collectionInterval );
        authTimer = new TimeTracker( clientID, threadID, STAT_TRACKER_AUTHENTICATION_TIME, collectionInterval );

        // Enable real-time reporting of the data for these stat trackers.
        RealTimeStatReporter statReporter = getStatReporter();
        if ( statReporter != null )
        {
            String jobID = getJobID();
            attemptCounter.enableRealTimeStats( statReporter, jobID );
            successCounter.enableRealTimeStats( statReporter, jobID );
            failureCounter.enableRealTimeStats( statReporter, jobID );
            authTimer.enableRealTimeStats( statReporter, jobID );
        }
    }


    /**
     * Performs the work of actually running the job.  When this method completes,
     * the job will be done.
     */
    public void runJob()
    {
        // Determine the range of time for which we should collect statistics.
        long currentTime = System.currentTimeMillis();
        boolean collectingStats = false;
        long startCollectingTime = currentTime + ( 1000 * warmUpTime );
        long stopCollectingTime = Long.MAX_VALUE;

        // See if this thread should operate "infinitely" (i.e., not a fixed number of iterations)
        boolean infinite = ( iterations <= 0 );

        // Loop until it is time to stop.
        for ( int ii = 0; !shouldStop() && ( infinite || ii < iterations ); ii++ )
        {
            currentTime = System.currentTimeMillis();

            if ( ( !collectingStats ) && ( currentTime >= startCollectingTime ) && ( currentTime < stopCollectingTime ) )
            {
                // Start all the stat trackers.
                attemptCounter.startTracker();
                successCounter.startTracker();
                failureCounter.startTracker();
                authTimer.startTracker();
                collectingStats = true;
            }
            else if ( ( collectingStats ) && ( currentTime >= stopCollectingTime ) )
            {
                // Stop all the stat trackers.
                attemptCounter.stopTracker();
                successCounter.stopTracker();
                failureCounter.stopTracker();
                authTimer.stopTracker();
                collectingStats = false;
            }

            // See if we need to sleep before the next attempt
            if ( delay > 0 )
            {
                long now = System.currentTimeMillis();
                long sleepTime = delay - now;

                if ( sleepTime > 0 )
                {
                    try
                    {
                        Thread.sleep( sleepTime );
                    }
                    catch ( InterruptedException ie )
                    {
                    }

                    if ( shouldStop() )
                    {
                        break;
                    }
                }
            }

            if ( ! shareConnections )
            {
                bindConnection = new LDAPConnection();
                
                try
                {
                    bindConnection.connect( 3, directoryHost, directoryPort, "", "" );
                }
                catch ( Exception e )
                {
                    throw new IllegalStateException( "Unable to establish the connections " 
                        + "to the directory server:  " + e, e );
                }
        
                // Initialize the constraints.
                bindConstraints = bindConnection.getConstraints();
                bindConstraints.setTimeLimit( 1000 * timeLimit );
            }

            if ( collectingStats )
            {
                attemptCounter.increment();
                authTimer.startTimer();
            }

            // Increment the number of authentication attempts and start the timer
            try
            {
                // Perform a bind as the user to verify that the provided password is
                // valid.
                bindConnection.authenticate( 3, bindDN, bindPW );
                if ( collectingStats )
                {
                    successCounter.increment();
                    authTimer.stopTimer();
                }
            }
            catch ( LDAPException le )
            {
                if ( !( ignoreInvalidCredentials && ( le.getLDAPResultCode() == LDAPException.INVALID_CREDENTIALS ) ) )
                {
                    if ( collectingStats )
                    {
                        failureCounter.increment();
                        authTimer.stopTimer();
                    }
                }
                
                StringBuffer buf = new StringBuffer();
                buf.append( "LDAPException: " ).append( le.getMessage() )
                    .append( " - " ).append( le.getLDAPErrorMessage() );
                writeVerbose( buf.toString() );
            }
            finally
            {
                if ( ! shareConnections )
                {
                    if ( bindConnection != null )
                    {
                        try
                        {
                            bindConnection.disconnect();
                        }
                        catch ( Exception e )
                        {
                        }
            
                        bindConnection = null;
                    }
                }
            }
        }

        attemptCounter.stopTracker();
        successCounter.stopTracker();
        failureCounter.stopTracker();
        authTimer.stopTracker();
    }


    /**
     * Attempts to force this thread to exit by closing the connections to the
     * directory server and setting them to <CODE>null</CODE>.
     */
    public void destroy()
    {
        if ( shareConnections )
        {
            if ( bindConnection != null )
            {
                try
                {
                    bindConnection.disconnect();
                }
                catch ( Exception e )
                {
                }
    
                bindConnection = null;
            }
        }
    }
}
