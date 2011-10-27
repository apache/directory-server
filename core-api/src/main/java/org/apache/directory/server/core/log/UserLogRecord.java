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
    
    /** length of the user record in the byte array */
    private int length;
    
    /** Position of the log record in the log */
    private LogAnchor logAnchor = new LogAnchor();
    
    public void setData( byte[] data, int length )
    {
        this.recordHolder = data;
        this.length = length;
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
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Length: " + length + ", anchor: {" + logAnchor + "}";
    }
}
