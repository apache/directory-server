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
package org.apache.directory.server.core;

import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.name.DN;

public class MockOperation implements OperationContext
{
    final int count;
    final CoreSession session;


    public MockOperation( int count ) throws Exception 
    {
        this.count = count;
        this.session = new MockCoreSession( new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), 
            new MockDirectoryService( count ) );
    }


    public EntryFilteringCursor search( SearchOperationContext opContext )
        throws NamingException
    {
        return new BaseEntryFilteringCursor( new MockCursor( count ), opContext );
    }


    public EntryFilteringCursor search( SearchOperationContext opContext, Collection<String> bypass ) throws NamingException
    {
        return new BaseEntryFilteringCursor( new MockCursor( count ), opContext );
    }


    public void addRequestControl( Control requestControl )
    {
    }


    public void addRequestControls( Control[] requestControls )
    {
    }


    public void addResponseControl( Control responseControl )
    {
    }


    public Collection<String> getByPassed()
    {
        return null;
    }


    public DN getDn()
    {
        return null;
    }


    public String getName()
    {
        return null;
    }


    public Control getRequestControl( String numericOid )
    {
        return null;
    }


    public Control getResponseControl( String numericOid )
    {
        return null;
    }


    public int getResponseControlCount()
    {
        return 0;
    }


    public Control[] getResponseControls()
    {
        return null;
    }


    public CoreSession getSession()
    {
        return session;
    }


    public boolean hasBypass()
    {
        return false;
    }


    public boolean hasRequestControl( String numericOid )
    {
        return false;
    }


    public boolean hasRequestControls()
    {
        return false;
    }


    public boolean hasResponseControl( String numericOid )
    {
        return false;
    }


    public boolean hasResponseControls()
    {
        return false;
    }


    public boolean isBypassed( String interceptorName )
    {
        return false;
    }


    public boolean isCollateralOperation()
    {
        return false;
    }


    public ClonedServerEntry lookup( DN dn, Collection<String> bypass ) throws Exception
    {
        return null;
    }


    public ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws Exception
    {
        return null;
    }


    public LookupOperationContext newLookupContext( DN dn )
    {
        return null;
    }


    public void setByPassed( Collection<String> byPassed )
    {
    }


    public void setCollateralOperation( boolean collateralOperation )
    {
    }


    public void setDn( DN dn )
    {
    }


    public LdapPrincipal getEffectivePrincipal()
    {
        return null;
    }


    public OperationContext getFirstOperation()
    {
        return null;
    }


    public OperationContext getLastOperation()
    {
        return null;
    }


    public OperationContext getNextOperation()
    {
        return null;
    }


    public OperationContext getPreviousOperation()
    {
        return null;
    }


    public boolean isFirstOperation()
    {
        return false;
    }


    public void add( ServerEntry entry, Collection<String> bypass ) throws Exception
    {
    }


    public void delete( DN dn, Collection<String> bypass ) throws Exception
    {
    }


    public void modify( DN dn, List<Modification> mods, Collection<String> bypass ) throws Exception
    {
    }


    public boolean hasEntry( DN dn, Collection<String> byPass ) throws Exception
    {
        return false;
    }


    public ReferralHandlingMode getReferralHandlingMode()
    {
        return null;
    }


    public void setReferralHandlingMode( ReferralHandlingMode referralHandlingMode )
    {
    }


    public ClonedServerEntry getEntry()
    {
        return null;
    }


    public void setEntry( ClonedServerEntry entry )
    {
    }


    public void throwReferral()
    {
    }
    
    
    public boolean isReferralThrown()
    {
        return false;
    }


    public void ignoreReferral()
    {
    }


    public boolean isReferralIgnored()
    {
        return false;
    }
}
