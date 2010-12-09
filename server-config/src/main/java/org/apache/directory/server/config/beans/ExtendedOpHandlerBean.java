/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.config.beans;


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the LdapServerExtendedOpHandler configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedOpHandlerBean extends AdsBaseBean
{
    /** The Extended Operation ID */
    @ConfigurationElement(attributeType = "ads-extendedOpId", isRdn = true)
    private String extendedOpId;

    /** The Extended Operation FQCN */
    @ConfigurationElement(attributeType = "ads-extendedOpHandlerClass")
    private String extendedOpHandlerClass;


    /**
     * Create a new LdapServerExtendedOpHandlerBean instance
     */
    public ExtendedOpHandlerBean()
    {
        super();
    }


    /**
     * @return the extendedOpId
     */
    public String getExtendedOpId()
    {
        return extendedOpId;
    }


    /**
     * @param extendedOpId the extendedOpId to set
     */
    public void setExtendedOpId( String extendedOpId )
    {
        this.extendedOpId = extendedOpId;
    }


    /**
     * @return the ldapServerExtendedOpHandlerClass
     */
    public String getExtendedOpHandlerClass()
    {
        return extendedOpHandlerClass;
    }


    /**
     * @param extendedOpHandlerClass the ExtendedOpHandlerClass to set
     */
    public void setExtendedOpHandlerClass( String extendedOpHandlerClass )
    {
        this.extendedOpHandlerClass = extendedOpHandlerClass;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "Extended operation handler :\n" );
        sb.append( tabs ).append( "  extended operation ID : " ).append( extendedOpId ).append( '\n' );
        sb.append( tabs ).append( "  extended operation handler class : " ).append( extendedOpHandlerClass )
            .append( '\n' );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
