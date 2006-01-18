/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.ldap.common.codec;


import java.util.HashMap;
import java.util.Map;

import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.asn1.ber.tlv.Value;
import org.apache.asn1.codec.DecoderException;
import org.apache.ldap.common.codec.search.controls.PSearchControlDecoder;
import org.apache.ldap.common.codec.search.controls.SubEntryControlDecoder;
import org.apache.ldap.common.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to set the value of a control.  This is an extension point 
 * where different controls can be plugged in (at least eventually).  For now
 * we hard code controls.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ControlValueAction extends GrammarAction
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ControlValueAction.class );
    private static Map controlDecoders = new HashMap(); 
    
    
    public ControlValueAction()
    {
        super( "Sets the control value" );

        ControlDecoder decoder;
        decoder = new PSearchControlDecoder();
        controlDecoders.put( decoder.getControlType(), decoder );
        
        decoder = new ManageDsaITControlDecoder();
        controlDecoders.put( decoder.getControlType(), decoder );
        
        decoder = new SubEntryControlDecoder();
        controlDecoders.put( decoder.getControlType(), decoder );
    }

    
    public void action( IAsn1Container container ) throws DecoderException
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();
        TLV tlv = ldapMessageContainer.getCurrentTLV();
        
        // Get the current control
        Control control = ldapMessage.getCurrentControl();
        Value value = tlv.getValue();

        ControlDecoder decoder = ( ControlDecoder ) controlDecoders.get( control.getControlType() );
        
        // Store the value - have to handle the special case of a 0 length value
        if ( tlv.getLength().getLength() == 0 )
        {
            control.setControlValue( new byte[]{} );
        }
        else
        {
            Object decoded;
            
            if ( decoder != null )
            {
                decoded = decoder.decode( value.getData() );
            }
            else
            {
                decoded = value.getData();
            }
            
            control.setEncodedValue( value.getData() );
            control.setControlValue( decoded );
        }
        
        // We can have an END transition
        ldapMessageContainer.grammarEndAllowed( true );
        
        if ( log.isDebugEnabled() )
        {
            if ( control.getControlValue() instanceof byte[] )
            {
                log.debug( "Control value : " + StringTools.dumpBytes( (byte[]) control.getControlValue() ) );
            }
            else if ( control.getControlValue() instanceof String )
            {
                log.debug( "Control value : " + ( String ) control.getControlValue() );
            }
            else
            {
                log.debug( "Control value : " + control.getControlValue() );
            }
        }
    }
}
