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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.util.LdapResultEnum;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * A ldapObject to store the LdapResult
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapResult extends Asn1Object
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /**
     * The result code. The different values are : 
     * 
     * success                                  (0), 
     * operationsError                          (1), 
     * protocolError                            (2), 
     * timeLimitExceeded                        (3), 
     * sizeLimitExceeded                        (4),
     * compareFalse                             (5), 
     * compareTrue                              (6), 
     * authMethodNotSupported                   (7),
     * strongAuthRequired                       (8), 
     *                                          -- 9 reserved -- 
     * referral                                 (10), -- new 
     * adminLimitExceeded                       (11), -- new 
     * unavailableCriticalExtension             (12), -- new 
     * confidentialityRequired                  (13), -- new 
     * saslBindInProgress                       (14), -- new
     * noSuchAttribute                          (16), 
     * undefinedAttributeType                   (17), 
     * inappropriateMatching                    (18), 
     * constraintViolation                      (19), 
     * attributeOrValueExists                   (20),
     * invalidAttributeSyntax                   (21), 
     *                                          -- 22-31 unused -- 
     * noSuchObject                             (32),
     * aliasProblem                             (33), 
     * invalidDNSyntax                          (34), 
     *                                          -- 35 reserved for undefined isLeaf -- 
     * aliasDereferencingProblem                (36), 
     *                                          -- 37-47 unused --
     * inappropriateAuthentication              (48), 
     * invalidCredentials                       (49),
     * insufficientAccessRights                 (50), 
     * busy                                     (51), 
     * unavailable                              (52),
     * unwillingToPerform                       (53), 
     * loopDetect                               (54), 
     *                                          -- 55-63 unused --
     * namingViolation                          (64), 
     * objectClassViolation                     (65), 
     * notAllowedOnNonLeaf                      (66), 
     * notAllowedOnRDN                          (67), 
     * entryAlreadyExists                       (68),
     * objectClassModsProhibited                (69), 
     *                                          -- 70 reserved for CLDAP --
     * affectsMultipleDSAs                      (71), -- new 
     *                                          -- 72-79 unused -- 
     * other                                    (80) 
     * }                                        -- 81-90 reserved for APIs --
     */
    private int resultCode;

    /** The DN that is matched by the Bind */
    private LdapDN matchedDN;

    /** Temporary storage of the byte[] representing the matchedDN */
    private transient byte[] matchedDNBytes;

    /** The error message */
    private String errorMessage;
    
    /** Temporary storage for message bytes */
    private transient byte[] errorMessageBytes;

    /** The referrals, if any. This is an optional element */
    private ArrayList referrals;

    /** The inner size of the referrals sequence */
    private transient int referralsLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new BindResponse object.
     */
    public LdapResult()
    {
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Initialize the referrals list
     */
    public void initReferrals()
    {
        referrals = new ArrayList();
    }
    
    /**
     * Get the error message
     * 
     * @return Returns the errorMessage.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }


    /**
     * Set the error message
     * 
     * @param errorMessage The errorMessage to set.
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }


    /**
     * Get the matched DN
     * 
     * @return Returns the matchedDN.
     */
    public String getMatchedDN()
    {
        return ( ( matchedDN == null ) ? "" : matchedDN.toString() );
    }


    /**
     * Set the Matched DN
     * 
     * @param matchedDN The matchedDN to set.
     */
    public void setMatchedDN( LdapDN matchedDN )
    {
        this.matchedDN = matchedDN;
    }


    /**
     * Get the referrals
     * 
     * @return Returns the referrals.
     */
    public ArrayList getReferrals()
    {
        return referrals;
    }


    /**
     * Add a referral
     * 
     * @param referral The referral to add.
     */
    public void addReferral( LdapURL referral )
    {
        referrals.add( referral );
    }


    /**
     * Get the result code
     * 
     * @return Returns the resultCode.
     */
    public int getResultCode()
    {
        return resultCode;
    }


    /**
     * Set the result code
     * 
     * @param resultCode The resultCode to set.
     */
    public void setResultCode( int resultCode )
    {
        this.resultCode = resultCode;
    }


    /**
     * Compute the LdapResult length 
     * 
     * LdapResult : 
     * 0x0A 01 resultCode (0..80)
     *   0x04 L1 matchedDN (L1 = Length(matchedDN)) 
     *   0x04 L2 errorMessage (L2 = Length(errorMessage)) 
     *   [0x83 L3] referrals 
     *     | 
     *     +--> 0x04 L4 referral 
     *     +--> 0x04 L5 referral 
     *     +--> ... 
     *     +--> 0x04 Li referral 
     *     +--> ... 
     *     +--> 0x04 Ln referral 
     *     
     * L1 = Length(matchedDN) 
     * L2 = Length(errorMessage) 
     * L3 = n*Length(0x04) + sum(Length(L4) .. Length(Ln)) + sum(L4..Ln) 
     * L4..n = Length(0x04) + Length(Li) + Li 
     * Length(LdapResult) = Length(0x0x0A) +
     *      Length(0x01) + 1 + Length(0x04) + Length(L1) + L1 + Length(0x04) +
     *      Length(L2) + L2 + Length(0x83) + Length(L3) + L3
     */
    public int computeLength()
    {
        int ldapResultLength = 0;

        // The result code : always 3 bytes
        ldapResultLength = 1 + 1 + 1;

        // The matchedDN length
        if ( matchedDN == null )
        {
            ldapResultLength += 1 + 1;
        }
        else
        {
            matchedDNBytes = StringTools.getBytesUtf8( matchedDN.getUpName() );
            ldapResultLength += 1 + TLV.getNbBytes( matchedDNBytes.length ) + matchedDNBytes.length;
        }

        // The errorMessage length
        errorMessageBytes = StringTools.getBytesUtf8( errorMessage ); 
        ldapResultLength += 1 + TLV.getNbBytes( errorMessageBytes.length ) + errorMessageBytes.length;

        if ( ( referrals != null ) && ( referrals.size() != 0 ) )
        {
            Iterator referralIterator = referrals.iterator();

            referralsLength = 0;

            // Each referral
            while ( referralIterator.hasNext() )
            {
                LdapURL referral = ( LdapURL ) referralIterator.next();

                referralsLength += 1 + TLV.getNbBytes( referral.getNbBytes() ) + referral.getNbBytes();
            }

            // The referrals
            ldapResultLength += 1 + TLV.getNbBytes( referralsLength ) + referralsLength;
        }

        return ldapResultLength;
    }


    /**
     * Encode the LdapResult message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The result code
            buffer.put( UniversalTag.ENUMERATED_TAG );
            buffer.put( ( byte ) 1 );
            buffer.put( ( byte ) resultCode );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        // The matchedDN
        Value.encode( buffer, matchedDNBytes );

        // The error message
        Value.encode( buffer, errorMessageBytes );

        // The referrals, if any
        if ( ( referrals != null ) && ( referrals.size() != 0 ) )
        {
            // Encode the referrals sequence
            // The referrals length MUST have been computed before !
            buffer.put( ( byte ) LdapConstants.LDAP_RESULT_REFERRAL_SEQUENCE_TAG );
            buffer.put( TLV.getBytes( referralsLength ) );

            // Each referral
            Iterator referralIterator = referrals.iterator();

            while ( referralIterator.hasNext() )
            {
                LdapURL referral = ( LdapURL ) referralIterator.next();

                // Ecode the current referral
                Value.encode( buffer, referral.getBytes() );
            }
        }

        return buffer;
    }


    /**
     * Get a String representation of a LdapResult
     * 
     * @return A LdapResult String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "        Ldap Result\n" );
        sb.append( "            Result code : (" ).append( resultCode ).append( ')' );

        switch ( resultCode )
        {

            case LdapResultEnum.SUCCESS:
                sb.append( " success\n" );
                break;

            case LdapResultEnum.OPERATIONS_ERROR:
                sb.append( " operationsError\n" );
                break;

            case LdapResultEnum.PROTOCOL_ERROR:
                sb.append( " protocolError\n" );
                break;

            case LdapResultEnum.TIME_LIMIT_EXCEEDED:
                sb.append( " timeLimitExceeded\n" );
                break;

            case LdapResultEnum.SIZE_LIMIT_EXCEEDED:
                sb.append( " sizeLimitExceeded\n" );
                break;

            case LdapResultEnum.COMPARE_FALSE:
                sb.append( " compareFalse\n" );
                break;

            case LdapResultEnum.COMPARE_TRUE:
                sb.append( " compareTrue\n" );
                break;

            case LdapResultEnum.AUTH_METHOD_NOT_SUPPORTED:
                sb.append( " authMethodNotSupported\n" );
                break;

            case LdapResultEnum.STRONG_AUTH_REQUIRED:
                sb.append( " strongAuthRequired\n" );
                break;

            case LdapResultEnum.RESERVED_9:
                sb.append( " -- 9 reserved --\n" );
                break;

            case LdapResultEnum.REFERRAL:
                sb.append( " referral -- new\n" );
                break;

            case LdapResultEnum.ADMIN_LIMIT_EXCEEDED:
                sb.append( " adminLimitExceeded -- new\n" );
                break;

            case LdapResultEnum.UNAVAILABLE_CRITICAL_EXTENSION:
                sb.append( " unavailableCriticalExtension -- new\n" );
                break;

            case LdapResultEnum.CONFIDENTIALITY_REQUIRED:
                sb.append( " confidentialityRequired -- new\n" );
                break;

            case LdapResultEnum.SASL_BIND_IN_PROGRESS:
                sb.append( " saslBindInProgress -- new\n" );
                break;

            case LdapResultEnum.NO_SUCH_ATTRIBUTE:
                sb.append( " noSuchAttribute\n" );
                break;

            case LdapResultEnum.UNDEFINED_ATTRIBUTE_TYPE:
                sb.append( " undefinedAttributeType\n" );
                break;

            case LdapResultEnum.INAPPROPRIATE_MATCHING:
                sb.append( " inappropriateMatching\n" );
                break;

            case LdapResultEnum.CONSTRAINT_VIOLATION:
                sb.append( " constraintViolation\n" );
                break;

            case LdapResultEnum.ATTRIBUTE_OR_VALUE_EXISTS:
                sb.append( " attributeOrValueExists\n" );
                break;

            case LdapResultEnum.INVALID_ATTRIBUTE_SYNTAX:
                sb.append( " invalidAttributeSyntax\n" );
                break;

            case LdapResultEnum.UNUSED_22:
            case LdapResultEnum.UNUSED_23:
            case LdapResultEnum.UNUSED_24:
            case LdapResultEnum.UNUSED_25:
            case LdapResultEnum.UNUSED_26:
            case LdapResultEnum.UNUSED_27:
            case LdapResultEnum.UNUSED_28:
            case LdapResultEnum.UNUSED_29:
            case LdapResultEnum.UNUSED_30:
            case LdapResultEnum.UNUSED_31:
                sb.append( " -- 22-31 unused --\n" );
                break;

            case LdapResultEnum.NO_SUCH_OBJECT:
                sb.append( " noSuchObject\n" );
                break;

            case LdapResultEnum.ALIAS_PROBLEM:
                sb.append( " aliasProblem\n" );
                break;

            case LdapResultEnum.INVALID_DN_SYNTAX:
                sb.append( " invalidDNSyntax\n" );
                break;

            case LdapResultEnum.RESERVED_FOR_UNDEFINED_IS_LEAF:
                sb.append( " -- 35 reserved for undefined isLeaf --\n" );
                break;

            case LdapResultEnum.ALIAS_DEREFERENCING_PROBLEM:
                sb.append( " aliasDereferencingProblem\n" );
                break;

            case LdapResultEnum.UNUSED_37:
            case LdapResultEnum.UNUSED_38:
            case LdapResultEnum.UNUSED_39:
            case LdapResultEnum.UNUSED_40:
            case LdapResultEnum.UNUSED_41:
            case LdapResultEnum.UNUSED_42:
            case LdapResultEnum.UNUSED_43:
            case LdapResultEnum.UNUSED_44:
            case LdapResultEnum.UNUSED_45:
            case LdapResultEnum.UNUSED_46:
            case LdapResultEnum.UNUSED_47:
                sb.append( " -- 37-47 unused --\n" );
                break;

            case LdapResultEnum.INAPPROPRIATE_AUTHENTICATION:
                sb.append( " inappropriateAuthentication\n" );
                break;

            case LdapResultEnum.INVALID_CREDENTIALS:
                sb.append( " invalidCredentials\n" );
                break;

            case LdapResultEnum.INSUFFICIENT_ACCESS_RIGHTS:
                sb.append( " insufficientAccessRights\n" );
                break;

            case LdapResultEnum.BUSY:
                sb.append( " busy\n" );
                break;

            case LdapResultEnum.UNAVAILABLE:
                sb.append( " unavailable\n" );
                break;

            case LdapResultEnum.UNWILLING_TO_PERFORM:
                sb.append( " unwillingToPerform\n" );
                break;

            case LdapResultEnum.LOOP_DETECT:
                sb.append( " loopDetect\n" );
                break;

            case LdapResultEnum.UNUSED_55:
            case LdapResultEnum.UNUSED_56:
            case LdapResultEnum.UNUSED_57:
            case LdapResultEnum.UNUSED_58:
            case LdapResultEnum.UNUSED_59:
            case LdapResultEnum.UNUSED_60:
            case LdapResultEnum.UNUSED_61:
            case LdapResultEnum.UNUSED_62:
            case LdapResultEnum.UNUSED_63:
                sb.append( " -- 55-63 unused --\n" );
                break;

            case LdapResultEnum.NAMING_VIOLATION:
                sb.append( " namingViolation\n" );
                break;

            case LdapResultEnum.OBJECT_CLASS_VIOLATION:
                sb.append( " objectClassViolation\n" );
                break;

            case LdapResultEnum.NOT_ALLOWED_ON_NON_LEAF:
                sb.append( " notAllowedOnNonLeaf\n" );
                break;

            case LdapResultEnum.NOT_ALLOWED_ON_RDN:
                sb.append( " notAllowedOnRDN\n" );
                break;

            case LdapResultEnum.ENTRY_ALREADY_EXISTS:
                sb.append( " entryAlreadyExists\n" );
                break;

            case LdapResultEnum.OBJECT_CLASS_MODS_PROHIBITED:
                sb.append( " objectClassModsProhibited\n" );
                break;

            case LdapResultEnum.RESERVED_FOR_CLDAP:
                sb.append( " -- 70 reserved for CLDAP --\n" );
                break;

            case LdapResultEnum.AFFECTS_MULTIPLE_DSAS:
                sb.append( " affectsMultipleDSAs -- new\n" );
                break;

            case LdapResultEnum.UNUSED_72:
            case LdapResultEnum.UNUSED_73:
            case LdapResultEnum.UNUSED_74:
            case LdapResultEnum.UNUSED_75:
            case LdapResultEnum.UNUSED_76:
            case LdapResultEnum.UNUSED_77:
            case LdapResultEnum.UNUSED_78:
            case LdapResultEnum.UNUSED_79:
                sb.append( " -- 72-79 unused --\n" );
                break;

            case LdapResultEnum.OTHER:
                sb.append( " other\n" );
                break;

            case LdapResultEnum.RESERVED_FOR_APIS_81:
            case LdapResultEnum.RESERVED_FOR_APIS_82:
            case LdapResultEnum.RESERVED_FOR_APIS_83:
            case LdapResultEnum.RESERVED_FOR_APIS_84:
            case LdapResultEnum.RESERVED_FOR_APIS_85:
            case LdapResultEnum.RESERVED_FOR_APIS_86:
            case LdapResultEnum.RESERVED_FOR_APIS_87:
            case LdapResultEnum.RESERVED_FOR_APIS_88:
            case LdapResultEnum.RESERVED_FOR_APIS_89:
            case LdapResultEnum.RESERVED_FOR_APIS_90:
                sb.append( " -- 81-90 reserved for APIs --" );
                break;

            default:
                sb.append( "Unknown error code : " ).append( resultCode );
        }

        sb.append( "            Matched DN : '" ).append( matchedDN == null ? "": matchedDN.toString() ).append( "'\n" );
        sb.append( "            Error message : '" ).append( errorMessage == null ? "" : errorMessage.toString() ).append( "'\n" );

        
        if ( ( referrals != null ) && ( referrals.size() != 0 ) )
        {
            sb.append( "            Referrals :\n" );

            for ( int i = 0; i < referrals.size(); i++ )
            {

                LdapURL referral = ( LdapURL ) referrals.get( i );

                sb.append( "                Referral[" ).append( i ).append( "] :" ).append( referral == null ? "" : referral.toString() )
                    .append( '\n' );
            }
        }

        return sb.toString();
    }
}
