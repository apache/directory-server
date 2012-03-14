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


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/** 
 * A user log record that can be used to pass user record between the clients and the logger. 
 * It contains a byte array which may not be used completely, and a position in the Log file.<br/>
 * <br/>
 * Here, we can see a RecordHolder containing some data, where the data's length is smaller
 * than the RecordHolder's length :
 * <pre>
 * +--------------------------+----------+
 * |XXXXXXXXXXXXXXXXXXXXXXXXXX|          |
 * +--------------------------+----------+
 *  <---------data----------->
 *  <-------------RecordHolder---------->
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UserLogRecord implements Externalizable
{
    /**
     * An enum used to distinguished the data type being serialized in the UserLogRecord
     */
    public enum LogEditType
    {
        TXN,
        DATA;
    }

    /** The serialized LogEdit type */
    private LogEditType dataType;

    /** array used to hold user log records */
    private byte[] recordHolder;

    /** length of the user record in the byte array */
    private int length;

    /** Position of the log record in the log */
    private LogAnchor logAnchor = new LogAnchor();


    /**
     * Store some data into the record. The buffer may be bigger than the 
     * data it contains, the length gives the real size of the stored data.
     * 
     * @param data The buffer containing the data
     * @param length The length of valid data in the buffer
     */
    public void setData( byte[] data, int length )
    {
        this.recordHolder = data;
        this.length = length;
    }


    /**
     * @return The stored buffer, containing the data
     */
    public byte[] getDataBuffer()
    {
        return recordHolder;
    }


    /**
     * @return The data length
     */
    public int getDataLength()
    {
        return length;
    }


    /**
     * @return The position in the buffer
     */
    public LogAnchor getLogAnchor()
    {
        return logAnchor;
    }


    /**
     * Read back the UserLogRecord from the stream.
     */
    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the DataType : TXN or DATA
        int type = in.read();

        dataType = LogEditType.values()[type];

        // The data size
        length = in.readInt();

        // The buffer size
        int bufferSize = in.readInt();

        recordHolder = new byte[bufferSize];

        // The buffer
        in.readFully( recordHolder );

        // The position
        logAnchor = new LogAnchor();
        logAnchor.readExternal( in );
    }


    /**
     * Write the UserLogRecord in a stream. The format is : <br/>
     * <ul>
     * <li>length of the stored data into the buffer</li>
     * <li>length of the buffer
     * <li>the buffer containing the data</li>
     * <li>The logAnchor</li>
     * </ul>
     */
    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // The inner data type : TXN or DATA
        out.write( dataType.ordinal() );

        // The size of the stored data
        out.writeInt( length );

        // The size of the container buffer
        out.writeInt( recordHolder.length );

        // The buffer
        out.write( recordHolder );

        // The position
        logAnchor.writeExternal( out );
    }


    public void setType( LogEditType dataType )
    {
        this.dataType = dataType;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Length: " + length + ", anchor: {" + logAnchor + "}";
    }
}
