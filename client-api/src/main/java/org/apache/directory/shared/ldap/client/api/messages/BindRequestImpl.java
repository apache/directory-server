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
package org.apache.directory.shared.ldap.client.api.messages;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A client implementation of the client BindRequest LDAP message.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindRequestImpl extends AbstractRequest implements BindRequest
{
    /**
     * Distinguished name identifying the name of the authenticating subject -
     * defaults to the empty string
     */
    private String name;
    
    /** The passwords, keys or tickets used to verify user identity */
    private byte[] credentials;
    
    /** The mechanism used to decode user identity */
    private String saslMechanism;
    
    /** Simple vs. SASL authentication mode flag */
    private boolean isSimple = true;

    /** Returns the protocol version */
    private int version = 3;


    /**
     * Creates a new instance of BindRequestImpl.
     */
    public BindRequestImpl()
    {
        super();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public byte[] getCredentials()
    {
        return credentials;
    }

    
    /**
     * {@inheritDoc}
     */
    public BindRequest setCredentials( byte[] credentials )
    {
        this.credentials = credentials;
        
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public BindRequest setCredentials( String credentials )
    {
        this.credentials = StringTools.getBytesUtf8( credentials );
        
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }

    
    /**
     * {@inheritDoc}
     */
    public BindRequest setName( String name )
    {
        this.name = name;
        
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getSaslMechanism()
    {
        return saslMechanism;
    }

    
    /**
     * {@inheritDoc}
     */
    public BindRequest setSaslMechanism( String saslMechanism )
    {
        this.saslMechanism = saslMechanism;
        
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public int getVersion()
    {
        return version;
    }

    
    /**
     * {@inheritDoc}
     */
    public BindRequest setVersion( int version )
    {
        this.version = version;
        
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isSimple()
    {
        return isSimple;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isVersion3()
    {
        return version == 3;
    }
    

    /**
     * {@inheritDoc}
     */
    public BindRequest setSasl()
    {
        isSimple = false;
        
        return this;
    }
    
    
    /**
     * Get a String representation of a BindRequest
     * 
     * @return A BindRequest String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append( super.toString() );
        sb.append( "    BindRequest\n" );
        sb.append( "        Version : '" ).append( version ).append( "'\n" );

        if ( ( null == name ) || StringTools.isEmpty( name.toString() ) )
        {
            sb.append( "        Name : anonymous\n" );
        }
        else
        {
            sb.append( "        Name : '" ).append( name ).append( "'\n" );

            if ( isSimple )
            {
                sb.append( "        Simple authentication : '" ).append( StringTools.utf8ToString( credentials ) ).append( "'\n" );
            }
            else
            {
                sb.append( "        Sasl authentication : \n" );
                sb.append( "            mechanism : '" ).append(  saslMechanism ).append( "'\n" );
                sb.append( "            credentials : '" ).append( StringTools.utf8ToString( credentials ) ).append( "'\n" );
            }
        }
        
        return sb.toString();
    }
}
