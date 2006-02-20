/*
 *   Copyright 2004 The Apache Software Foundation
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

/*
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.message;


/**
 * LdapResult implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class LdapResultImpl implements LdapResult
{
    static final long serialVersionUID = -1446626887394613213L;

    /** Lowest matched entry Dn - defaults to empty string */
    private String matchedDn;

    /** Referral associated with this LdapResult if the errorCode is REFERRAL */
    private Referral referral;

    /** Decriptive error message - defaults to empty string */
    private String errorMessage;

    /** Resultant operation error code - defaults to SUCCESS */
    private ResultCodeEnum resultCode = ResultCodeEnum.SUCCESS;


    // ------------------------------------------------------------------------
    // LdapResult Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the descriptive error message associated with the error code. May be
     * null for SUCCESS, COMPARETRUE, COMPAREFALSE and REFERRAL operations.
     * 
     * @return the descriptive error message.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }


    /**
     * Sets the descriptive error message associated with the error code. May be
     * null for SUCCESS, COMPARETRUE, and COMPAREFALSE operations.
     * 
     * @param errorMessage
     *            the descriptive error message.
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }


    /**
     * Gets the lowest entry in the directory that was matched. For result codes
     * of noSuchObject, aliasProblem, invalidDNSyntax and
     * aliasDereferencingProblem, the matchedDN field is set to the name of the
     * lowest entry (object or alias) in the directory that was matched. If no
     * aliases were dereferenced while attempting to locate the entry, this will
     * be a truncated form of the name provided, or if aliases were
     * dereferenced, of the resulting name, as defined in section 12.5 of X.511
     * [8]. The matchedDN field is to be set to a zero length string with all
     * other result codes.
     * 
     * @return the Dn of the lowest matched entry.
     */
    public String getMatchedDn()
    {
        return matchedDn;
    }


    /**
     * Sets the lowest entry in the directory that was matched.
     * 
     * @see #getMatchedDn()
     * @param matchedDn
     *            the Dn of the lowest matched entry.
     */
    public void setMatchedDn( String matchedDn )
    {
        this.matchedDn = matchedDn;
    }


    /**
     * Gets the result code enumeration associated with the response.
     * Corresponds to the <b> resultCode </b> field within the LDAPResult ASN.1
     * structure.
     * 
     * @return the result code enum value.
     */
    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }


    /**
     * Sets the result code enumeration associated with the response.
     * Corresponds to the <b> resultCode </b> field within the LDAPResult ASN.1
     * structure.
     * 
     * @param resultCode
     *            the result code enum value.
     */
    public void setResultCode( ResultCodeEnum resultCode )
    {
        this.resultCode = resultCode;
    }


    /**
     * Gets the Referral associated with this LdapResult if the resultCode
     * property is set to the REFERRAL ResultCodeEnum.
     * 
     * @return the referral on REFERRAL errors, null on all others.
     */
    public Referral getReferral()
    {
        return referral;
    }


    /**
     * Gets whether or not this result represents a Referral. For referrals the
     * error code is set to REFERRAL and the referral property is not null.
     * 
     * @return true if this result represents a referral.
     */
    public boolean isReferral()
    {
        return referral != null;
    }


    /**
     * Sets the Referral associated with this LdapResult if the resultCode
     * property is set to the REFERRAL ResultCodeEnum. Setting this property
     * will result in a true return from isReferral and the resultCode should be
     * set to REFERRAL.
     * 
     * @param referral
     *            optional referral on REFERRAL errors.
     */
    public void setReferral( Referral referral )
    {
        this.referral = referral;
    }


    /**
     * @param obj
     * @return
     */
    public boolean equals( Object obj )
    {
        // quickly return true if this is the obj
        if ( obj == this )
        {
            return true;
        }

        // return false if object does not implement interface
        if ( !( obj instanceof LdapResult ) )
        {
            return false;
        }

        // compare all the like elements of the two LdapResult objects
        LdapResult result = ( LdapResult ) obj;

        if ( referral == null && result.getReferral() != null )
        {
            return false;
        }

        if ( result.getReferral() == null && referral != null )
        {
            return false;
        }

        if ( referral != null && result.getReferral() != null )
        {
            if ( !referral.equals( result.getReferral() ) )
            {
                return false;
            }
        }

        if ( !resultCode.equals( result.getResultCode() ) )
        {
            return false;
        }

        // Handle Error Messages where "" is considered equivalent to null
        String errMsg0 = errorMessage;
        String errMsg1 = result.getErrorMessage();

        if ( errMsg0 == null )
        {
            errMsg0 = "";
        }

        if ( errMsg1 == null )
        {
            errMsg1 = "";
        }

        if ( !errMsg0.equals( errMsg1 ) )
        {
            return false;
        }

        if ( matchedDn != null )
        {
            if ( !matchedDn.equals( result.getMatchedDn() ) )
            {
                return false;
            }
        }
        else if ( result.getMatchedDn() != null ) // one is null other is not
        {
            return false;
        }

        return true;
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

        if ( resultCode != null )
        {

            if ( resultCode.getValue() == ResultCodeEnum.SUCCESS_VAL )
            {
                sb.append( " success\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.OPERATIONSERROR_VAL )
            {
                sb.append( " operationsError\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.PROTOCOLERROR_VAL )
            {
                sb.append( " protocolError\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.TIMELIMITEXCEEDED_VAL )
            {
                sb.append( " timeLimitExceeded\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.SIZELIMITEXCEEDED_VAL )
            {
                sb.append( " sizeLimitExceeded\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.COMPAREFALSE_VAL )
            {
                sb.append( " compareFalse\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.COMPARETRUE_VAL )
            {
                sb.append( " compareTrue\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.AUTHMETHODNOTSUPPORTED_VAL )
            {
                sb.append( " authMethodNotSupported\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.STRONGAUTHREQUIRED_VAL )
            {
                sb.append( " strongAuthRequired\n" );
            }
            else if ( resultCode.getValue() == 9 )
            {
                sb.append( " -- 9 reserved --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.REFERRAL_VAL )
            {
                sb.append( " referral -- new\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.ADMINLIMITEXCEEDED_VAL )
            {
                sb.append( " adminLimitExceeded -- new\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.UNAVAILABLECRITICALEXTENSION_VAL )
            {
                sb.append( " unavailableCriticalExtension -- new\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.CONFIDENTIALITYREQUIRED_VAL )
            {
                sb.append( " confidentialityRequired -- new\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.SASLBINDINPROGRESS_VAL )
            {
                sb.append( " saslBindInProgress -- new\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.NOSUCHATTRIBUTE_VAL )
            {
                sb.append( " noSuchAttribute\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.UNDEFINEDATTRIBUTETYPE_VAL )
            {
                sb.append( " undefinedAttributeType\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.INAPPROPRIATEMATCHING_VAL )
            {
                sb.append( " inappropriateMatching\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.CONSTRAINTVIOLATION_VAL )
            {
                sb.append( " constraintViolation\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.ATTRIBUTEORVALUEEXISTS_VAL )
            {
                sb.append( " attributeOrValueExists\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.INVALIDATTRIBUTESYNTAX_VAL )
            {
                sb.append( " invalidAttributeSyntax\n" );
            }
            else if ( ( resultCode.getValue() >= 22 ) && ( resultCode.getValue() <= 31 ) )
            {
                sb.append( " -- 22-31 unused --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.NOSUCHOBJECT_VAL )
            {
                sb.append( " noSuchObject\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.ALIASPROBLEM_VAL )
            {
                sb.append( " aliasProblem\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.INVALIDDNSYNTAX_VAL )
            {
                sb.append( " invalidDNSyntax\n" );
            }
            else if ( resultCode.getValue() == 35 )
            {
                sb.append( " -- 35 reserved for undefined isLeaf --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.ALIASDEREFERENCINGPROBLEM_VAL )
            {
                sb.append( " aliasDereferencingProblem\n" );
            }
            else if ( ( resultCode.getValue() >= 37 ) && ( resultCode.getValue() <= 47 ) )
            {
                sb.append( " -- 37-47 unused --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.INAPPROPRIATEAUTHENTICATION_VAL )
            {
                sb.append( " inappropriateAuthentication\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.INVALIDCREDENTIALS_VAL )
            {
                sb.append( " invalidCredentials\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.INSUFFICIENTACCESSRIGHTS_VAL )
            {
                sb.append( " insufficientAccessRights\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.BUSY_VAL )
            {
                sb.append( " busy\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.UNAVAILABLE_VAL )
            {
                sb.append( " unavailable\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.UNWILLINGTOPERFORM_VAL )
            {
                sb.append( " unwillingToPerform\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.LOOPDETECT_VAL )
            {
                sb.append( " loopDetect\n" );
            }
            else if ( ( resultCode.getValue() >= 55 ) && ( resultCode.getValue() <= 63 ) )
            {
                sb.append( " -- 55-63 unused --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.NAMINGVIOLATION_VAL )
            {
                sb.append( " namingViolation\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.OBJECTCLASSVIOLATION_VAL )
            {
                sb.append( " objectClassViolation\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.NOTALLOWEDONNONLEAF_VAL )
            {
                sb.append( " notAllowedOnNonLeaf\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.NOTALLOWEDONRDN_VAL )
            {
                sb.append( " notAllowedOnRDN\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.ENTRYALREADYEXISTS_VAL )
            {
                sb.append( " entryAlreadyExists\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.OBJECTCLASSMODSPROHIBITED_VAL )
            {
                sb.append( " objectClassModsProhibited\n" );
            }
            else if ( resultCode.getValue() == 70 )
            {
                sb.append( " -- 70 reserved for CLDAP --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.AFFECTSMULTIPLEDSAS_VAL )
            {
                sb.append( " affectsMultipleDSAs -- new\n" );
            }
            else if ( ( resultCode.getValue() >= 72 ) && ( resultCode.getValue() <= 79 ) )
            {
                sb.append( " -- 72-79 unused --\n" );
            }
            else if ( resultCode.getValue() == ResultCodeEnum.OTHER_VAL )
            {
                sb.append( " other\n" );
            }
            else if ( ( resultCode.getValue() >= 81 ) && ( resultCode.getValue() <= 90 ) )
            {
                sb.append( " -- 81-90 reserved for APIs --" );
            }
            else
            {
                sb.append( "Unknown error code : " ).append( resultCode );
            }
        }

        sb.append( "            Matched DN : '" ).append( matchedDn ).append( "'\n" );
        sb.append( "            Error message : '" ).append( errorMessage ).append( "'\n" );

        if ( referral != null )
        {
            sb.append( "            Referrals :\n" );

            sb.append( "                Referral :" ).append( referral.toString() ).append( '\n' );
        }

        return sb.toString();
    }
}
