
package org.apache.directory.server.log;


import java.io.IOException;

public interface LogScanner
{
    /**
     * Reads and returns the next user record from the log into a backing byte array 
     * and returns a reference to it. Returned array can be overwritten 
     * after the next call to getNextRecord()
     *
     * @param  log record to be filled in by
     * @return true if there is a next record
     * throws IOException
     * throws InvalidLogException thrown if the log content is invalid 
     */
    public boolean getNextRecord(UserLogRecord logRecord) throws IOException, InvalidLogException;
    
    
    /**
     * Returns the last successfully read log file number
     *
     * @return last successfully read log file number
     */
    public long getLastGoodFileNumber();
    
    /**
     * Returns the last successfully read log file number
     *
     * @return last successfully read log file number
     */
    public long getLastGoodOffset();
    
    /**
     * Closes the scanner and releases any
     * resources. 
     *
     */
    public void close();
}
