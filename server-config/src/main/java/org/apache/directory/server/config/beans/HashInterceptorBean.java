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


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A bean used to store the hash interceptor configuration
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HashInterceptorBean extends InterceptorBean
{
    /** The hash algorithm */
    @ConfigurationElement(attributeType = "ads-hashAlgorithm", isOptional = true, defaultValue = "SSHA-256" )
    private String hashAlgorithm;

    /** The reference to the Password Policy component */
    @ConfigurationElement(attributeType = "ads-hashAttribute", isOptional = true, defaultValues = {"2.5.4.35"} )
    private Set<String> hashAttributes = new HashSet<String>();


    /**
     * Creates a new AuthenticationInterceptorBean instance
     */
    public HashInterceptorBean()
    {
        super();
    }


    /**
     * @param hashAttributes The attributes that need to be hashed
     */
    public void addHashAttributes( String[] hashAttributes )
    {
        if ( hashAttributes != null && hashAttributes.length > 0 ) 
        {
            if ( this.hashAttributes == null ) 
            {
                this.hashAttributes = new HashSet<String>();
            }
            this.hashAttributes.addAll( Arrays.asList( hashAttributes ) );
        }
    }


    /**
     * @return the hash algorithm
     */
    public String getHashAlgorithm()
    {
        return hashAlgorithm;
    }


    /**
     * @return the attributes to hash
     */
    public Set<String> getHashAttributes()
    {
        return hashAttributes;
    }


    /**
     * @param hashAlgorithm The hash algorithm to use
     */
    public void setHashAlgorithm( String hashAlgorithm )
    {
        this.hashAlgorithm = hashAlgorithm;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "HashInterceptor :\n" );
        sb.append( super.toString( tabs + "  " ) );

        if ( hashAlgorithm != null )
        {
            sb.append( tabs ).append( "  hashAlgorithm : " )
                    .append( hashAlgorithm ).append( "\n" );
        }
        if ( ( hashAttributes != null ) && ( hashAttributes.size() > 0 ) )
        {
            sb.append( tabs ).append( "  hashAttributes :\n" );

            for ( String hashAttribute : hashAttributes )
            {
                sb.append( tabs ).append( "    " ).append( hashAttribute );
            }
        }

        return sb.toString();
    }
}
