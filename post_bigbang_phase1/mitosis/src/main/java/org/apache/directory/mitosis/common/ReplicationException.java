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
package org.apache.directory.mitosis.common;


/**
 * A {@link RuntimeException} which if thrown when a problem occurred during
 * the replication process.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicationException extends RuntimeException
{
    private static final long serialVersionUID = -347196060295426926L;

    /**
     * Creates a new instance.
     */
    public ReplicationException()
    {
        super();
    }

    /**
     * Creates a new instance with the specified <tt>message</tt> and
     * <tt>cause</tt>.
     */
    public ReplicationException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates a new instance with the specified <tt>message</tt>.
     */
    public ReplicationException( String message )
    {
        super( message );
    }

    /**
     * Creates a new instance with the specified <tt>cause</tt>.
     */
    public ReplicationException( Throwable cause )
    {
        super( cause );
    }
}
