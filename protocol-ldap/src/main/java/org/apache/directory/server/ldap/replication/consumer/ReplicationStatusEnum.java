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
package org.apache.directory.server.ldap.replication.consumer;

/**
 * This enum is used to describe the various status of the replication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ReplicationStatusEnum
{
    /** We get disconnected from the provider */
    DISCONNECTED,
    
    /** A full refresh should be done */
    REFRESH_REQUIRED,
    
    /** The replication loop has been interrupted */
    INTERRUPTED,
    
    /** The replication has been stopped */
    STOPPED,
    
    /** The replication has been cancelled */
    CANCELLED,
    
    /** We have got an unknown error */
    UNKOWN_ERROR;
}
