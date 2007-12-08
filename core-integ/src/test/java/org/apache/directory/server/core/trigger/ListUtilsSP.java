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
package org.apache.directory.server.core.trigger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ListUtilsSP
{
    private static final Logger LOG = LoggerFactory.getLogger( ListUtilsSP.class );


    public static void subscribeToGroup( Name addedEntryName, LdapContext groupCtx ) throws NamingException
    {
        LOG.info( "User \"" + addedEntryName + "\" will be subscribed to \"" + groupCtx + "\"" );
        groupCtx.modifyAttributes("", DirContext.ADD_ATTRIBUTE,
                new BasicAttributes( SchemaConstants.UNIQUE_MEMBER_AT, addedEntryName.toString(), true ) );
        LOG.info( "Subscription OK." );
    }
}
