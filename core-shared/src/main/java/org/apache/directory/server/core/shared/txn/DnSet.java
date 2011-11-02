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

import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.message.SearchScope;

/**
 * A class representing the set of Dns a read operation depends or the set of Dns a write 
 * operation affects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnSet
{
    /** Base Dn */
    private Dn baseDn;

    /** Scope of the set */
    SearchScope dnScope;


    public DnSet( Dn base, SearchScope scope )
    {
        baseDn = base;
        dnScope = scope;
    }


    public Dn getBaseDn()
    {
        return baseDn;
    }


    public SearchScope getScope()
    {
        return dnScope;
    }
}
