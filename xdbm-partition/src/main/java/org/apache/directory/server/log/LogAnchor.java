
package org.apache.directory.server.log;

import org.apache.directory.server.i18n.I18n;

/**
 * Implements a pointer in to the log files
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LogAnchor
{
    // TODO move this to logger 
    /** Invalid/unknown lsn. Log LSN starts at UNKNOWN_LSN + 1 and is ever increasing */
    public final static long UNKNOWN_LSN = Long.MIN_VALUE;
    
    /** Min log file number */
    public final static long MIN_LOG_NUMBER = 0;
    
    /** Min log file offset */
    public final static long MIN_LOG_OFFSET = 0;
    
    
    /** log file identifier of the anchor */
    private long logFileNumber = 0 ;
    
    /** Offset into the log file identified by logfilenumber */
    private long logFileOffset = 0;
    
    /** LSN corresponding to the logFileNumber and fileOffset */
    private long logLSN = UNKNOWN_LSN; 
    
    public LogAnchor()
    {
        
    }
    
    public LogAnchor( long logFileNumber, long logFileOffset, long logLSN )
    {
        this.resetLogAnchor( logFileNumber, logFileOffset, logLSN );
    }
    
    
    public void resetLogAnchor( long logFileNumber, long logFileOffset, long logLSN )
    {
        if ( logFileNumber < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_746, logFileNumber ) );
        }
        
        if ( logFileOffset < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_747, logFileOffset ) );
        }
        
        
        this.logFileNumber = logFileNumber;
        this.logFileOffset = logFileOffset;
        this.logLSN = logLSN;
    }
    
    public void resetLogAnchor( LogAnchor logAnchor )
    {
        this.resetLogAnchor( logAnchor.getLogFileNumber(), logAnchor.getLogFileOffset(), logAnchor.getLogLSN() );
    }
    
     
    public long getLogFileNumber()
    {
        return this.logFileNumber;
    }
    
    
    public long getLogFileOffset()
    {
        return this.logFileOffset;
    }
    
    
    public long getLogLSN()
    {
        return this.logLSN;
    }  
}
