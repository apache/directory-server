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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;

import javax.naming.NamingException;
import java.util.Comparator;

import jdbm.helper.StringComparator;


/**
 * An attributeType used for testing situations when there is only an ordering
 * matchingRule for one.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class OrderingOnlyMatchingRuleAttributeType implements AttributeType
{
    private static final long serialVersionUID = 1L;


    public boolean isSingleValue()
    {
        return false;
    }


    public boolean isCanUserModify()
    {
        return false;
    }


    public boolean isCollective()
    {
        return false;
    }


    public UsageEnum getUsage()
    {
        return null;
    }


    public AttributeType getSuperior() throws NamingException
    {
        return null;
    }


    public Syntax getSyntax() throws NamingException
    {
        return new BogusSyntax();
    }


    public int getLength()
    {
        return 0;
    }


    public MatchingRule getEquality() throws NamingException
    {
        return null;
    }


    public MatchingRule getOrdering() throws NamingException
    {
        return new MatchingRule()
        {
            private static final long serialVersionUID = 1L;


            public Syntax getSyntax() throws NamingException
            {
                return new BogusSyntax();
            }


            @SuppressWarnings("unchecked")
            public Comparator<String> getComparator() throws NamingException
            {
                return new StringComparator();
            }


            public Normalizer getNormalizer() throws NamingException
            {
                return new NoOpNormalizer();
            }


            public boolean isObsolete()
            {
                return false;
            }


            public String getOid()
            {
                return null;
            }


            public String getName()
            {
                return null;
            }


            public String getDescription()
            {
                return null;
            }


            public String getSchema()
            {
                return null;
            }


            public void setSchema( String schemaName )
            {
            }


            public String[] getNamesRef()
            {
                return null;
            }
        };
    }


    public MatchingRule getSubstr() throws NamingException
    {
        return null;
    }


    public boolean isAncestorOf( AttributeType descendant ) throws NamingException
    {
        return false;
    }


    public boolean isDescendantOf( AttributeType ancestor ) throws NamingException
    {
        return false;
    }


    public boolean isObsolete()
    {
        return false;
    }


    public String getOid()
    {
        return SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".2000";
    }


    public String[] getNames()
    {
        return new String[] { "bogus" };
    }


    public String getName()
    {
        return "bogus";
    }


    public String getDescription()
    {
        return "";
    }


    public String getSchema()
    {
        return "other";
    }


    public void setSchema( String schemaName )
    {
    }


    public String[] getNamesRef()
    {
        return new String[] { "bogus" } ;
    }
}