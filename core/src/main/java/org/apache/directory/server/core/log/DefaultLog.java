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
package org.apache.directory.server.core.log;

import java.io.IOException;


public class DefaultLog implements Log
{
    /** Log manager */
    LogManager logManager;
    
    /** Log File Manager */
    LogFileManager logFileManager;
    
    
    /** LogFlushManager */
    LogFlushManager logFlushManager;
    
    /**
     * {@inheritDoc}
     */
   public void init( String logFilepath, String suffix, int logBufferSize, long logFileSize ) throws IOException, InvalidLogException
   {
       logFileManager = new DefaultLogFileManager();
       logFileManager.init( logFilepath, suffix );
       
       logManager = new LogManager( logFileManager );
       logManager.initLogManager();
       
       logFlushManager = new LogFlushManager( logManager, logBufferSize, logFileSize );
   }
    
   /**
    * {@inheritDoc}
    */
    public void log( UserLogRecord userRecord, boolean sync ) throws IOException, InvalidLogException
    {
        logFlushManager.append( userRecord, sync );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public LogScanner beginScan( LogAnchor startPoint )
    {
        LogScannerInternal logScanner = new DefaultLogScanner();
        logScanner.init( startPoint, logFileManager );
        return logScanner;
    }
    
    /**
     * {@inheritDoc}
     */
    public void advanceMinNeededLogPosition( LogAnchor newAnchor )
    {
       logManager.advanceMinLogAnchor( newAnchor ); 
    }
}
