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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;

import javax.naming.NamingException;


/**
 * TODO doc me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class NoEqMatchAttribute implements AttributeType
{
    public boolean isSingleValue()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean isCanUserModify()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean isCollective()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public UsageEnum getUsage()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public AttributeType getSuperior() throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public LdapSyntax getSyntax() throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public int getLength()
    {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public MatchingRule getEquality() throws NamingException
    {
        throw new NamingException( "Just for testing." );
    }


    public MatchingRule getOrdering() throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public MatchingRule getSubstr() throws NamingException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean isAncestorOf( AttributeType descendant ) throws NamingException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean isDescendantOf( AttributeType ancestor ) throws NamingException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean isObsolete()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getOid()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String[] getNames()
    {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getName()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getDescription()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public String getSchema()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void setSchema( String schemaName )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public String[] getNamesRef()
    {
        return null;
    }
}
