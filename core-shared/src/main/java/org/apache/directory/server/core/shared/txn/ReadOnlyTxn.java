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
package org.apache.directory.server.core.shared.txn;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** Package protected */
class ReadOnlyTxn extends AbstractTransaction
{
    private int nbRef = 0;


    public void releaseTxn()
    {
        nbRef--;
    }


    public void acquireTxn()
    {
        nbRef++;
    }


    public boolean isReused()
    {
        return nbRef > 1;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ROTxn[" );

        // The state
        sb.append( "state:" ).append( getState() );

        // The ref count
        sb.append( ", ref:" ).append( nbRef );

        // The start time
        sb.append( ", start:" ).append( getStartTime() );

        // The commit time
        sb.append( ", commit:" ).append( getCommitTime() );

        sb.append( "]\n" );

        return sb.toString();
    }
}
