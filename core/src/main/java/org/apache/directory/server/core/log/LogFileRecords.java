
package org.apache.directory.server.core.log;


public class LogFileRecords
{
    /** 
     * When a user appends a log record, a header and 
     * footer that surrounds the log record is inserted
     * into the log. Header and footer information makes
     * it easier to scan the log, detect the end of 
     * log during log scan and verify the integrity of log.  
     */  
    
    /**
     * Record Header marker
     * int RECORD_HEADER_MAGIC_NUMBER
     * int length  length of header + user log record + legnth of footer
     * long recordLSN     lsn of the log record   
     * long headerChecksum checksum to verify header
     */
    
    /** Header magic number */
    final static int RECORD_HEADER_MAGIC_NUMBER = 0x010F010F;
     
    /** Total header size */
    final static int RECORD_HEADER_SIZE = 24;
    
    /**
     * Record Footer marker 
     * int checksum 
     * int RECORD_FOOTER_MAGIC_NUMBER
     */
    
    /** Footer magic number */
    final static int RECORD_FOOTER_MAGIC_NUMBER = 0x0F010F01;
   
    /** Total header size */
    final static int RECORD_FOOTER_SIZE = 8;
    
    /**
     * LogFileHeader marker
     * long log file number
     * int LOG_FILE_HEADER_MAGIC_NUMBER 0xFF00FF00
     */
    
    /** Log file header marker size */
    final static int LOG_FILE_HEADER_SIZE = 12;
    
    /** Log file header magic number */
    final static int LOG_FILE_HEADER_MAGIC_NUMBER = 0xFF00FF00;
    
    /** Maximum marker size */
    final static int MAX_MARKER_SIZE;
    
    static
    {
        int markerSize = Math.max( RECORD_HEADER_SIZE, RECORD_FOOTER_SIZE );
        MAX_MARKER_SIZE = Math.max( markerSize, LOG_FILE_HEADER_SIZE );
    }
    
}
