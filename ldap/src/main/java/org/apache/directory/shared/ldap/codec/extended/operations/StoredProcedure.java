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

package org.apache.directory.shared.ldap.codec.extended.operations;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Stored Procedure Extended Operation bean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedure extends Asn1Object
{
    private String language;

    private byte[] procedure;

    private ArrayList parameters = new ArrayList();

    private transient StoredProcedureParameter currentParameter;
    
    /** The stored procedure length */
    private transient int storedProcedureLength;
    
    /** The parameters length */
    private transient int parametersLength;

    /** The list of all parameter lengths */
    private transient List parameterLength;

    /** The list of all parameter type lengths */
    private transient List paramTypeLength;

    /** The list of all parameter value lengths */
    private transient List paramValueLength;

    public String getLanguage()
    {
        return language;
    }


    public void setLanguage( String language )
    {
        this.language = language;
    }


    public byte[] getProcedure()
    {
        return procedure;
    }


    public void setProcedure( byte[] procedure )
    {
        this.procedure = procedure;
    }


    public List getParameters()
    {
        return parameters;
    }


    public void addParameter( StoredProcedureParameter parameter )
    {
        parameters.add( parameter );
    }


    public StoredProcedureParameter getCurrentParameter()
    {
        return currentParameter;
    }


    public void setCurrentParameter( StoredProcedureParameter currentParameter )
    {
        this.currentParameter = currentParameter;
    }
    

    /**
     * Bean for representing a Stored Procedure Parameter
     */
    public static class StoredProcedureParameter
    {
        private byte[] type;

        private byte[] value;


        public byte[] getType()
        {
            return type;
        }


        public void setType( byte[] type )
        {
            this.type = type;
        }


        public byte[] getValue()
        {
            return value;
        }


        public void setValue( byte[] value )
        {
            this.value = value;
        }
    }

    /**
     * Compute the StoredProcedure length 
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x04 L2 language
     *   +--> 0x04 L3 procedure
     *  [+--> 0x30 L4 (parameters)
     *          |
     *          +--> 0x30 L5-1 (parameter)
     *          |      |
     *          |      +--> 0x04 L6-1 type
     *          |      +--> 0x04 L7-1 value
     *          |      
     *          +--> 0x30 L5-2 (parameter)
     *          |      |
     *          |      +--> 0x04 L6-2 type
     *          |      +--> 0x04 L7-2 value
     *          |
     *          +--> ...
     *          |      
     *          +--> 0x30 L5-m (parameter)
     *                 |
     *                 +--> 0x04 L6-m type
     *                 +--> 0x04 L7-m value
     */
    public int computeLength()
    {
        // The language
    	byte[] languageBytes = StringTools.getBytesUtf8( language );
    	
        int languageLength = 1 + Length.getNbBytes( languageBytes.length )
            + languageBytes.length;
    	
        // The procedure
        int procedureLength = 1 + Length.getNbBytes( procedure.length )
            + procedure.length;
    	
        //int localParametersLength = 0;
        
    	// Compute parameters length value
    	if ( parameters != null )
    	{
            parameterLength = new LinkedList();
            paramTypeLength = new LinkedList();
            paramValueLength = new LinkedList();
            
    		Iterator params = parameters.iterator();
    		
    		while ( params.hasNext() )
    		{
    			int localParameterLength = 0;
    			int localParamTypeLength = 0;
    			int localParamValueLength = 0;
    			
    			StoredProcedureParameter spParam = (StoredProcedureParameter)params.next();
    			
    			localParamTypeLength = 1 + Length.getNbBytes( spParam.type.length ) + spParam.type.length;
    			localParamValueLength = 1 + Length.getNbBytes( spParam.value.length ) + spParam.value.length;
    			
    			localParameterLength = localParamTypeLength + localParamValueLength;
    			
    			parametersLength += 1 + Length.getNbBytes( localParameterLength ) + localParameterLength;
    			
    			parameterLength.add( new Integer( localParameterLength ) );
    			paramTypeLength.add( new Integer( localParamTypeLength ) );
    			paramValueLength.add( new Integer( localParamValueLength ) );
    		}
    	}
    	
    	int localParametersLength = 1 + Length.getNbBytes( parametersLength ) + parametersLength; 
    	storedProcedureLength = languageLength + procedureLength + localParametersLength;

    	return 1 + Length.getNbBytes( storedProcedureLength ) + storedProcedureLength; 
    }

    /**
     * Encode the StoredProcedure message to a PDU. 
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );

        try
        {
            // The StoredProcedure Tag
            bb.put( UniversalTag.SEQUENCE_TAG );
            bb.put( Length.getBytes( storedProcedureLength ) );

            // The language
            Value.encode( bb, language );

            // The procedure
            Value.encode( bb, procedure );
            
            // The parameters sequence
            bb.put( UniversalTag.SEQUENCE_TAG );
            bb.put( Length.getBytes( parametersLength ) );

            // The parameters list
            if ( ( parameters != null ) && ( parameters.size() != 0 ) )
            {
            	int parameterNumber = 0;
        		Iterator params = parameters.iterator();
        		
        		while ( params.hasNext() )
        		{
        			StoredProcedureParameter spParam = (StoredProcedureParameter)params.next();

                    // The parameter sequence
                    bb.put( UniversalTag.SEQUENCE_TAG );
                    int localParameterLength = ( ( Integer ) parameterLength.get( parameterNumber ) ).intValue();
                    bb.put( Length.getBytes( localParameterLength ) );

                    // The parameter type
                    Value.encode( bb, spParam.type );

                    // The parameter value
                    Value.encode( bb, spParam.value );

                    // Go to the next parameter;
                    parameterNumber++;
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return bb;
    }


    /**
     * Returns the StoredProcedure string
     * 
     * @return The StoredProcedure string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    StoredProcedure\n" );
        sb.append( "        Language : '" ).append( language ).append( "'\n" );
        sb.append( "        Procedure\n" ).append( StringTools.utf8ToString( procedure ) ).append( "'\n" );

        if ( ( parameters == null ) || ( parameters.size() == 0 ) )
        {
            sb.append( "        No parameters\n" );
        }
        else
        {
            sb.append( "        Parameters\n" );

            Iterator params = parameters.iterator();
            int i = 1;
    		
    		while ( params.hasNext() )
    		{
    			StoredProcedureParameter spParam = (StoredProcedureParameter)params.next();
    			
    			sb.append( "            type[" ).append( i ) .append( "] : '" ).append( spParam.type ).append( "'\n" );
    			sb.append( "            value[" ).append( i ) .append( "] : '" ).
    				append( StringTools.dumpBytes( spParam.value ) ).append( "'\n" );
    		}
        }

        return sb.toString();
    }
}
