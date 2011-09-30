
package org.apache.directory.server.log;

/** 
 * A user log record that can be used to pass user record between the clients and the logger
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UserLogRecord
{
    private final static int INITIAL_SIZE =  1024;
    
    /** array used to hold user log records */
    private byte[] recordHolder;
    
    /** offset int the  byte array where user record starts */
    int offset;
    
    /** length of the user record in the byte array */
    int length;
    
    /** Position of the log record in the log */
    private LogAnchor logAnchor = new LogAnchor();
    
    public void setData( byte[] data, int length )
    {
        this.recordHolder = recordHolder;
    }
    
    public byte[] getDataBuffer()
    {
        return recordHolder;
    }
    
   
    public int getDataLength()
    {
        return length;
    }
    
    
    public LogAnchor getLogAnchor()
    {
        return logAnchor;
    }
    
}
