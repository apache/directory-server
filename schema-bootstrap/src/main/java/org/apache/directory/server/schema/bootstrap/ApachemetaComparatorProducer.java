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
package org.apache.directory.server.schema.bootstrap;


import java.io.Serializable;
import java.util.Comparator;

import javax.naming.NamingException;

import jdbm.helper.StringComparator;

import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.NormalizingComparator;
import org.apache.directory.shared.ldap.util.StringTools;



/**
 * A producer of Comparator objects for the apachemeta schema.  This code has been
 * automatically generated using schema files in the OpenLDAP format along with
 * the directory plugin for maven.  This has been done to facilitate
 * OpenLDAP schema interoperability.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApachemetaComparatorProducer extends AbstractBootstrapProducer
{
    public ApachemetaComparatorProducer()
    {
        super( ProducerTypeEnum.COMPARATOR_PRODUCER );
    }

    
    public static class DeepTrimToLowerNormalizingComparator extends NormalizingComparator
    {
        public DeepTrimToLowerNormalizingComparator()
        {
            super( new DeepTrimToLowerNormalizer(), new StringComparator() );
        }
    }

    
    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------


    /**
     * @see BootstrapProducer#produce(Registries, ProducerCallback)
     */
    public void produce( Registries registries, ProducerCallback cb )
        throws NamingException
    {
        Comparator comparator = null;
        
        comparator = new NameOrNumericIdComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.0.1.0", comparator );

        comparator = new ObjectClassTypeComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.0.1.1", comparator );
        
        comparator = new StringComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.0.1.2", comparator );
        
        comparator = new DeepTrimToLowerNormalizingComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.0.1.3", comparator );
        
        comparator = new DeepTrimToLowerNormalizingComparator();
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.18060.0.4.0.1.4", comparator );
    }


    public static class ObjectClassTypeComparator implements Comparator<Object>, Serializable
    {
        private static final long serialVersionUID = 1L;


        public int compare( Object o1, Object o2 )
        {
            String s1 = getString( o1 );
            String s2 = getString( o2 );
            
            if ( s1 == null && s2 == null )
            {
                return 0;
            }
            
            if ( s1 == null )
            {
                return -1;
            }
            
            if ( s2 == null )
            {
                return 1;
            }
            
            return s1.compareTo( s2 );
        }
        
        
        String getString( Object obj )
        {
            String strValue;

            if ( obj == null )
            {
                return null;
            }
            
            if ( obj instanceof String )
            {
                strValue = ( String ) obj;
            }
            else if ( obj instanceof byte[] )
            {
                strValue = StringTools.utf8ToString( ( byte[] ) obj ); 
            }
            else
            {
                strValue = obj.toString();
            }

            return strValue;
        }
    }
}
