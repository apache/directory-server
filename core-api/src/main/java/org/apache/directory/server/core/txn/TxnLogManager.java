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
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.txn.logedit.LogEdit;

import org.apache.directory.server.core.log.UserLogRecord;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

import java.io.IOException;

public interface TxnLogManager<ID>
{
    public void log( LogEdit<ID> logEdit, boolean sync ) throws IOException;
    
    public void log( UserLogRecord logRecord, boolean sync ) throws IOException;
    
    public Entry mergeUpdates(Dn partitionDN, ID entryID,  Entry entry );
    
    public IndexCursor<Object, Entry, ID> wrap( Dn partitionDn, IndexCursor<Object, Entry, ID> wrappedCursor, IndexComparator<Object,ID> comparator, String attributeOid, boolean forwardIndex, Object onlyValueKey, ID onlyIDKey ) throws Exception;
}
