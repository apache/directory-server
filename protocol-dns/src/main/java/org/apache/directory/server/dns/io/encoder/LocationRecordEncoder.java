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

package org.apache.directory.server.dns.io.encoder;


/**
 * 2. RDATA Format
 * 
 *        MSB                                           LSB
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *       0|        VERSION        |         SIZE          |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *       2|       HORIZ PRE       |       VERT PRE        |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *       4|                   LATITUDE                    |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *       6|                   LATITUDE                    |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *       8|                   LONGITUDE                   |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *      10|                   LONGITUDE                   |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *      12|                   ALTITUDE                    |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *      14|                   ALTITUDE                    |
 *        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *    (octet)
 * 
 * where:
 * 
 * VERSION      Version number of the representation.  This must be zero.
 *              Implementations are required to check this field and make
 *              no assumptions about the format of unrecognized versions.
 * 
 * SIZE         The diameter of a sphere enclosing the described entity, in
 *              centimeters, expressed as a pair of four-bit unsigned
 *              integers, each ranging from zero to nine, with the most
 *              significant four bits representing the base and the second
 *              number representing the power of ten by which to multiply
 *              the base.  This allows sizes from 0e0 (<1cm) to 9e9
 *              (90,000km) to be expressed.  This representation was chosen
 *              such that the hexadecimal representation can be read by
 *              eye; 0x15 = 1e5.  Four-bit values greater than 9 are
 *              undefined, as are values with a base of zero and a non-zero
 *              exponent.
 * 
 *              Since 20000000m (represented by the value 0x29) is greater
 *              than the equatorial diameter of the WGS 84 ellipsoid
 *              (12756274m), it is therefore suitable for use as a
 *              "worldwide" size.
 * 
 * HORIZ PRE    The horizontal precision of the data, in centimeters,
 *              expressed using the same representation as SIZE.  This is
 *              the diameter of the horizontal "circle of error", rather
 *              than a "plus or minus" value.  (This was chosen to match
 *              the interpretation of SIZE; to get a "plus or minus" value,
 *              divide by 2.)
 * 
 * VERT PRE     The vertical precision of the data, in centimeters,
 *              expressed using the sane representation as for SIZE.  This
 *              is the total potential vertical error, rather than a "plus
 *              or minus" value.  (This was chosen to match the
 *              interpretation of SIZE; to get a "plus or minus" value,
 *              divide by 2.)  Note that if altitude above or below sea
 *              level is used as an approximation for altitude relative to
 *              the [WGS 84] ellipsoid, the precision value should be
 *              adjusted.
 * 
 * LATITUDE     The latitude of the center of the sphere described by the
 *              SIZE field, expressed as a 32-bit integer, most significant
 *              octet first (network standard byte order), in thousandths
 *              of a second of arc.  2^31 represents the equator; numbers
 *              above that are north latitude.
 * 
 * LONGITUDE    The longitude of the center of the sphere described by the
 *              SIZE field, expressed as a 32-bit integer, most significant
 *              octet first (network standard byte order), in thousandths
 *              of a second of arc, rounded away from the prime meridian.
 *              2^31 represents the prime meridian; numbers above that are
 *              east longitude.
 * 
 * ALTITUDE     The altitude of the center of the sphere described by the
 *              SIZE field, expressed as a 32-bit integer, most significant
 *              octet first (network standard byte order), in centimeters,
 *              from a base of 100,000m below the [WGS 84] reference
 *              spheroid used by GPS (semimajor axis a=6378137.0,
 *              reciprocal flattening rf=298.257223563).  Altitude above
 *              (or below) sea level may be used as an approximation of
 *              altitude relative to the the [WGS 84] spheroid, though due
 *              to the Earth's surface not being a perfect spheroid, there
 *              will be differences.  (For example, the geoid (which sea
 *              level approximates) for the continental US ranges from 10
 *              meters to 50 meters below the [WGS 84] spheroid.
 *              Adjustments to ALTITUDE and/or VERT PRE will be necessary
 *              in most cases.  The Defense Mapping Agency publishes geoid
 *              height values relative to the [WGS 84] ellipsoid.
 */
public class LocationRecordEncoder
{
}
