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
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.util.Strings;


/**
 * A class used to store the Base ADS configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AdsBaseBean
{
    /** The enabled flag */
    @ConfigurationElement(attributeType = "ads-enabled")
    private boolean enabled = false;

    /** The description */
    @ConfigurationElement(attributeType = "description")
    private String description;


    /**
     * Create a new BaseBean instance
     */
    protected AdsBaseBean()
    {
    }


    /**
     * @return <code>true</code> if the component is enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * Enable or disable the component
     * @param enabled if <code>true</code>, the component is enabled.
     */
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    /**
     * @return the description for this component
     */
    public String getDescription()
    {
        return description;
    }


    /**
     * Sets the component description
     * 
     * @param description The description
     */
    public void setDescription( String description )
    {
        this.description = description;
    }


    /**
     * Formated print of a boolean
     */
    protected String toString( String tabs, String name, boolean value )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( name ).append( " : " );

        if ( value )
        {
            sb.append( "TRUE" );
        }
        else
        {
            sb.append( "FALSE" );
        }

        sb.append( '\n' );

        return sb.toString();
    }


    /**
     * Formated print of a String that can be null
     */
    protected String toString( String tabs, String name, String value )
    {
        if ( value != null )
        {
            return tabs + name + " : " + value + "\n";
        }
        else
        {
            return "";
        }
    }


    /**
     * Formated print of a DN that can be null
     */
    protected String toString( String tabs, String name, DN value )
    {
        if ( value != null )
        {
            return tabs + name + " : " + value.getName() + "\n";
        }
        else
        {
            return "";
        }
    }


    /**
     * a convenient method to finding if this bean was disabled in the config
     * 
     * @return true if the bean was disabled, false otherwise
     */
    public final boolean isDisabled()
    {
        return !enabled;
    }


    /**
     * Formated print of a long
     */
    protected String toString( String tabs, String name, long value )
    {
        return tabs + name + " : " + value + "\n";
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( toString( tabs, "enabled", enabled ) );

        if ( !Strings.isEmpty(description) )
        {
            sb.append( tabs ).append( "description : '" ).append( description ).append( "'\n" );
        }

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
