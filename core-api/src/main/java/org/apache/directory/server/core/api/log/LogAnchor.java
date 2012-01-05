/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.api.log;


import org.apache.directory.server.i18n.I18n;


/**
 * Implements a pointer in to the log files.
 * 
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
    private long logFileNumber = 0;

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
        resetLogAnchor( logAnchor.getLogFileNumber(), logAnchor.getLogFileOffset(), logAnchor.getLogLSN() );
    }


    public long getLogFileNumber()
    {
        return logFileNumber;
    }


    public long getLogFileOffset()
    {
        return logFileOffset;
    }


    public long getLogLSN()
    {
        return logLSN;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "File number: " + logFileNumber + ", offset: " + logFileOffset + ", LSN: " + Long.toHexString( logLSN );
    }
}
