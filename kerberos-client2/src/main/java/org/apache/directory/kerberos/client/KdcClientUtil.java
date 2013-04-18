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
package org.apache.directory.kerberos.client;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.etypeInfo.ETypeInfoContainer;
import org.apache.directory.shared.kerberos.codec.etypeInfo2.ETypeInfo2Container;
import org.apache.directory.shared.kerberos.codec.methodData.MethodDataContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.ETypeInfo;
import org.apache.directory.shared.kerberos.components.ETypeInfo2;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;
import org.apache.directory.shared.kerberos.components.MethodData;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.messages.KrbError;


/**
 * A class with utility methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcClientUtil
{
    public static String extractRealm( String principal )
    {
        int pos = principal.indexOf( '@' );

        if ( pos > 0 )
        {
            return principal.substring( pos + 1 );
        }

        throw new IllegalArgumentException( "Not a valid principal, missing realm name" );
    }


    public static String extractName( String principal )
    {
        int pos = principal.indexOf( '@' );

        if ( pos < 0 )
        {
            return principal;
        }

        return principal.substring( 0, pos );
    }


    public static Set<EncryptionType> getEtypesFromError( KrbError error )
    {
        try
        {
            ByteBuffer stream = ByteBuffer.wrap( error.getEData() );

            Asn1Decoder decoder = new Asn1Decoder();
            MethodDataContainer container = new MethodDataContainer();
            container.setStream( stream );
            decoder.decode( stream, container );
            
            MethodData methodData = container.getMethodData();
            
            for( PaData pd : methodData.getPaDatas() )
            {
                if( pd.getPaDataType() == PaDataType.PA_ENCTYPE_INFO2 )
                {
                    return parseEtpeInfo2( pd.getPaDataValue() );
                }
                else if( pd.getPaDataType() == PaDataType.PA_ENCTYPE_INFO )
                {
                    return parseEtpeInfo( pd.getPaDataValue() );
                }
            }
        }
        catch ( Exception e )
        {
            // shouldn't happen, but iff happens blast off
            throw new RuntimeException( e );
        }
        
        return Collections.EMPTY_SET;
    }
    
    
    private static Set<EncryptionType> parseEtpeInfo2( byte[] data ) throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.wrap( data );
        
        Asn1Decoder decoder = new Asn1Decoder();
        ETypeInfo2Container container = new ETypeInfo2Container();
        container.setStream( stream );
        decoder.decode( stream, container );
        
        ETypeInfo2 info2 = container.getETypeInfo2();

        Set<EncryptionType> lstEtypes = new LinkedHashSet<EncryptionType>();
        
        for( ETypeInfo2Entry e2e : info2.getETypeInfo2Entries() )
        {
            lstEtypes.add( e2e.getEType() );
        }
        
        return lstEtypes;
    }
    
    
    private static Set<EncryptionType> parseEtpeInfo( byte[] data ) throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.wrap( data );
        
        Asn1Decoder decoder = new Asn1Decoder();
        ETypeInfoContainer container = new ETypeInfoContainer();
        container.setStream( stream );
        decoder.decode( stream, container );
        
        ETypeInfo einfo = container.getETypeInfo();

        Set<EncryptionType> lstEtypes = new LinkedHashSet<EncryptionType>();
        
        for( ETypeInfoEntry eie : einfo.getETypeInfoEntries() )
        {
            lstEtypes.add( eie.getEType() );
        }
        
        return lstEtypes;
    }
    
}
