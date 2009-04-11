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
package org.apache.directory.shared.ldap.message.control;


import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.search.controls.subEntry.SubEntryControlCodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The control for the visibility of subentries with search operations. For
 * convenience we attach section 3 from <a
 * href="http://www.faqs.org/rfcs/rfc3672.html"> RFC 3672</a> where the control
 * is defined:
 * 
 * <pre>
 *   3.  Subentries Control
 *  
 *   The subentries control MAY be sent with a searchRequest to control
 *   the visibility of entries and subentries which are within scope.
 *   Non-visible entries or subentries are not returned in response to the
 *   request.
 *  
 *   The subentries control is an LDAP Control whose controlType is
 *   1.3.6.1.4.1.4203.1.10.1, criticality is TRUE or FALSE (hence absent),
 *   and controlValue contains a BER-encoded BOOLEAN indicating
 *   visibility.  A controlValue containing the value TRUE indicates that
 *   subentries are visible and normal entries are not.  A controlValue
 *   containing the value FALSE indicates that normal entries are visible
 *   and subentries are not.
 *  
 *   Note that TRUE visibility has the three octet encoding { 01 01 FF }
 *   and FALSE visibility has the three octet encoding { 01 01 00 }.
 *   
 *   The controlValue SHALL NOT be absent.
 *   
 *   In absence of this control, subentries are not visible to singleLevel
 *   and wholeSubtree scope Search requests but are visible to baseObject
 *   scope Search requests.
 *   
 *   There is no corresponding response control.
 *   
 *   This control is not appropriate for non-Search operations.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentriesControl extends InternalAbstractControl
{
    private static final long serialVersionUID = -2356861450876343999L;

    private static final Logger log = LoggerFactory.getLogger( SubentriesControl.class );

    public static final String CONTROL_OID = "1.3.6.1.4.1.4203.1.10.1";

    /** the visibility for this control */
    private boolean visibility = false;


    public SubentriesControl()
    {
        super();
        setID( CONTROL_OID );
    }


    public void setVisibility( boolean visibility )
    {
        this.visibility = visibility;
    }


    public boolean isVisible()
    {
        return visibility;
    }


    public byte[] getEncodedValue()
    {
        SubEntryControlCodec ctl = new SubEntryControlCodec();
        ctl.setVisibility( isVisible() );

        try
        {
            return ctl.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            log.error( "Failed to encode SubentriesControl", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }
}
