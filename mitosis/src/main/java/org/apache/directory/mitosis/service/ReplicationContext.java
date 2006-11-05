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
package org.apache.directory.mitosis.service;


import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.mina.common.IoSession;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;


public interface ReplicationContext
{
    IoSession getSession();


    ReplicationConfiguration getConfiguration();


    ReplicationService getService();


    DirectoryServiceConfiguration getServiceConfiguration();


    int getNextSequence();


    Replica getPeer();


    void setPeer( Replica peer );


    State getState();


    void setState( State state );


    void scheduleExpiration( Object message );


    Object cancelExpiration( int sequence );


    void cancelAllExpirations();


    int getScheduledExpirations();

    public static class State
    {
        /**
         * Connection is established.
         */
        public static final State INIT = new State( "INIT" );

        /**
         * Client has logged in and is ready to exchange information.
         */
        public static final State READY = new State( "READY" );

        private final String value;


        private State( String value )
        {
            this.value = value;
        }


        public String toString()
        {
            return value;
        }
    }
}
