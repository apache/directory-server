/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.message.extended;

import java.nio.ByteBuffer;

import javax.naming.NamingException;
import javax.naming.ldap.ExtendedResponse;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureContainer;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedureDecoder;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An extended operation requesting the server to execute a stored procedure.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class StoredProcedureRequest extends ExtendedRequestImpl
{
    private static final Logger log = LoggerFactory.getLogger( StoredProcedureRequest.class );
    private static final long serialVersionUID = -4682291068700593492L;
    public static final String EXTENSION_OID = "1.2.6.1.4.1.18060.1.1.1.100.6";

    private StoredProcedure procedure;

    
    public StoredProcedureRequest( int messageId, String procedure, String language )
    {
        super( messageId );
        this.setOid( EXTENSION_OID );
        this.procedure = new StoredProcedure();
        this.procedure.setLanguage( language );
        this.procedure.setProcedure( StringTools.getBytesUtf8( procedure ) );
    }


    private void encodePayload() throws EncoderException
    {
        payload = procedure.encode( null ).array();
    }


    public void setPayload( byte[] payload )
    {
        StoredProcedureDecoder decoder = new StoredProcedureDecoder();
        StoredProcedureContainer container = new StoredProcedureContainer();
        
        try
        {
            decoder.decode( ByteBuffer.wrap( payload ), container );
            this.procedure = container.getStoredProcedure();
        }
        catch ( Exception e )
        {
            log.error( "failed to decode payload", e );
            throw new RuntimeException( e );
        }
    }


    public ExtendedResponse createExtendedResponse( String id, byte[] berValue, int offset, int length )
        throws NamingException
    {
        return ( ExtendedResponse ) getResultResponse();
    }


    public byte[] getEncodedValue()
    {
        return getPayload();
    }


    public byte[] getPayload()
    {
        if ( payload == null )
        {
            try
            {
                encodePayload();
            }
            catch ( EncoderException e )
            {
                log.error( "Failed to encode payload StoredProcedureRequest", e );
                throw new RuntimeException( e );
            }
        }

        return payload;
    }


    public ResultResponse getResultResponse()
    {
        if ( response == null )
        {
            StoredProcedureResponse spr = new StoredProcedureResponse( getMessageId() );
            response = spr;
        }

        return response;
    }


    // -----------------------------------------------------------------------
    // Parameters of the Extended Request Payload
    // -----------------------------------------------------------------------


    public String getLanguage()
    {
        return procedure.getLanguage();
    }
    
    
    public String getProcedureSpecification()
    {
        return StringTools.utf8ToString( procedure.getProcedure() );
    }
    
    
    public int size()
    {
        return this.procedure.getParameters().size();
    }
    
    
    public Object getParameterType( int index )
    {
        if ( ! this.procedure.getLanguage().equals( "java" ) )
        {
            return ( ( StoredProcedureParameter ) procedure.getParameters().get( index ) ).getType();
        }

        return getJavaParameterType( index );
    }
    
    
    public Class getJavaParameterType( int index )
    {
        throw new NotImplementedException( "class loading of procedure type not implemented" );
    }
    
    
    public Object getParameterValue( int index )
    {
        if ( ! this.procedure.getLanguage().equals( "java" ) )
        {
            return ( ( StoredProcedureParameter ) procedure.getParameters().get( index ) ).getValue();
        }

        return getJavaParameterValue( index );
    }
    
    
    public Object getJavaParameterValue( int index )
    {
        throw new NotImplementedException( "conversion of value to java type not implemented" );
    }
    
    
    public void addParameter( Object type, Object value )
    {
        if ( ! this.procedure.getLanguage().equals( "java" ) )
        {
            StoredProcedureParameter parameter = new StoredProcedureParameter();
            parameter.setType( ( byte[] ) type );
            parameter.setValue( ( byte[] ) value );
            this.procedure.addParameter( parameter );
        }

        // below here try to convert parameters to their appropriate byte[] representations
        throw new NotImplementedException( "conversion of value to java type not implemented" );
    }
}
