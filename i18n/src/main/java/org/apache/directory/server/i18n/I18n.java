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

package org.apache.directory.server.i18n;


import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * Provides i18n handling of error codes.
 * About formatting see also {@link MessageFormat}
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class I18n
{
    private final static ResourceBundle errBundle = ResourceBundle
        .getBundle( "org/apache/directory/server/i18n/errors" );
    private final static ResourceBundle msgBundle = ResourceBundle
        .getBundle( "org/apache/directory/server/i18n/messages" );

    public static String ERR_1 = "ERR_1";
    public static String ERR_2 = "ERR_2";
    public static String ERR_3 = "ERR_3";
    public static String ERR_4 = "ERR_4";
    public static String ERR_5 = "ERR_5";
    public static String ERR_6 = "ERR_6";
    public static String ERR_7 = "ERR_7";
    public static String ERR_8 = "ERR_8";
    public static String ERR_9 = "ERR_9";
    public static String ERR_10 = "ERR_10";
    public static String ERR_11 = "ERR_11";
    public static String ERR_12 = "ERR_12";
    public static String ERR_13 = "ERR_13";
    public static String ERR_14 = "ERR_14";
    public static String ERR_15 = "ERR_15";
    public static String ERR_16 = "ERR_16";
    public static String ERR_17 = "ERR_17";
    public static String ERR_18 = "ERR_18";
    public static String ERR_19 = "ERR_19";
    public static String ERR_20 = "ERR_20";
    public static String ERR_21 = "ERR_21";
    public static String ERR_22 = "ERR_22";
    public static String ERR_23 = "ERR_23";
    public static String ERR_24 = "ERR_24";
    public static String ERR_25 = "ERR_25";
    public static String ERR_26 = "ERR_26";
    public static String ERR_27 = "ERR_27";
    public static String ERR_28 = "ERR_28";
    public static String ERR_29 = "ERR_29";
    public static String ERR_30 = "ERR_30";
    public static String ERR_31 = "ERR_31";
    public static String ERR_32 = "ERR_32";
    public static String ERR_33 = "ERR_33";
    public static String ERR_34 = "ERR_34";
    public static String ERR_35 = "ERR_35";
    public static String ERR_36 = "ERR_36";
    public static String ERR_37 = "ERR_37";
    public static String ERR_38 = "ERR_38";
    public static String ERR_39 = "ERR_39";
    public static String ERR_40 = "ERR_40";
    public static String ERR_41 = "ERR_41";
    public static String ERR_42 = "ERR_42";
    public static String ERR_43 = "ERR_43";
    public static String ERR_44 = "ERR_44";
    public static String ERR_45 = "ERR_45";
    public static String ERR_46 = "ERR_46";
    public static String ERR_47 = "ERR_47";
    public static String ERR_48 = "ERR_48";
    public static String ERR_49 = "ERR_49";
    public static String ERR_50 = "ERR_50";
    public static String ERR_51 = "ERR_51";
    public static String ERR_52 = "ERR_52";
    public static String ERR_53 = "ERR_53";
    public static String ERR_54 = "ERR_54";
    public static String ERR_55 = "ERR_55";
    public static String ERR_56 = "ERR_56";
    public static String ERR_57 = "ERR_57";
    public static String ERR_58 = "ERR_58";
    public static String ERR_59 = "ERR_59";
    public static String ERR_60 = "ERR_60";
    public static String ERR_61 = "ERR_61";
    public static String ERR_62 = "ERR_62";
    public static String ERR_63 = "ERR_63";
    public static String ERR_64 = "ERR_64";
    public static String ERR_65 = "ERR_65";
    public static String ERR_66 = "ERR_66";
    public static String ERR_67 = "ERR_67";
    public static String ERR_68 = "ERR_68";
    public static String ERR_69 = "ERR_69";
    public static String ERR_70 = "ERR_70";
    public static String ERR_71 = "ERR_71";
    public static String ERR_72 = "ERR_72";
    public static String ERR_73 = "ERR_73";
    public static String ERR_74 = "ERR_74";
    public static String ERR_75 = "ERR_75";
    public static String ERR_76 = "ERR_76";
    public static String ERR_77 = "ERR_77";
    public static String ERR_78 = "ERR_78";
    public static String ERR_79 = "ERR_79";
    public static String ERR_80 = "ERR_80";
    public static String ERR_81 = "ERR_81";
    public static String ERR_82 = "ERR_82";
    public static String ERR_83 = "ERR_83";
    public static String ERR_84 = "ERR_84";
    public static String ERR_85 = "ERR_85";
    public static String ERR_86 = "ERR_86";
    public static String ERR_87 = "ERR_87";
    public static String ERR_88 = "ERR_88";
    public static String ERR_89 = "ERR_89";
    public static String ERR_90 = "ERR_90";
    public static String ERR_91 = "ERR_91";
    public static String ERR_92 = "ERR_92";
    public static String ERR_93 = "ERR_93";
    public static String ERR_94 = "ERR_94";
    public static String ERR_95 = "ERR_95";
    public static String ERR_96 = "ERR_96";
    public static String ERR_97 = "ERR_97";
    public static String ERR_98 = "ERR_98";
    public static String ERR_99 = "ERR_99";
    public static String ERR_100 = "ERR_100";
    public static String ERR_101 = "ERR_101";
    public static String ERR_102 = "ERR_102";
    public static String ERR_103 = "ERR_103";
    public static String ERR_104 = "ERR_104";
    public static String ERR_105 = "ERR_105";
    public static String ERR_106 = "ERR_106";
    public static String ERR_107 = "ERR_107";
    public static String ERR_108 = "ERR_108";
    public static String ERR_109 = "ERR_109";
    public static String ERR_110 = "ERR_110";
    public static String ERR_111 = "ERR_111";
    public static String ERR_112 = "ERR_112";
    public static String ERR_113 = "ERR_113";
    public static String ERR_114 = "ERR_114";
    public static String ERR_115 = "ERR_115";
    public static String ERR_116 = "ERR_116";
    public static String ERR_117 = "ERR_117";
    public static String ERR_118 = "ERR_118";
    public static String ERR_119 = "ERR_119";
    public static String ERR_120 = "ERR_120";
    public static String ERR_121 = "ERR_121";
    public static String ERR_122 = "ERR_122";
    public static String ERR_123 = "ERR_123";
    public static String ERR_124 = "ERR_124";
    public static String ERR_125 = "ERR_125";
    public static String ERR_126 = "ERR_126";
    public static String ERR_127 = "ERR_127";
    public static String ERR_128 = "ERR_128";
    public static String ERR_129 = "ERR_129";
    public static String ERR_130 = "ERR_130";
    public static String ERR_131 = "ERR_131";
    public static String ERR_132 = "ERR_132";
    public static String ERR_133 = "ERR_133";
    public static String ERR_134 = "ERR_134";
    public static String ERR_135 = "ERR_135";
    public static String ERR_136 = "ERR_136";
    public static String ERR_137 = "ERR_137";
    public static String ERR_138 = "ERR_138";
    public static String ERR_139 = "ERR_139";
    public static String ERR_140 = "ERR_140";
    public static String ERR_141 = "ERR_141";
    public static String ERR_142 = "ERR_142";
    public static String ERR_143 = "ERR_143";
    public static String ERR_144 = "ERR_144";
    public static String ERR_145 = "ERR_145";
    public static String ERR_146 = "ERR_146";
    public static String ERR_147 = "ERR_147";
    public static String ERR_148 = "ERR_148";
    public static String ERR_149 = "ERR_149";
    public static String ERR_150 = "ERR_150";
    public static String ERR_151 = "ERR_151";
    public static String ERR_152 = "ERR_152";
    public static String ERR_153 = "ERR_153";
    public static String ERR_154 = "ERR_154";
    public static String ERR_155 = "ERR_155";
    public static String ERR_156 = "ERR_156";
    public static String ERR_157 = "ERR_157";
    public static String ERR_158 = "ERR_158";
    public static String ERR_159 = "ERR_159";
    public static String ERR_160 = "ERR_160";
    public static String ERR_161 = "ERR_161";
    public static String ERR_162 = "ERR_162";
    public static String ERR_163 = "ERR_163";
    public static String ERR_164 = "ERR_164";
    public static String ERR_165 = "ERR_165";
    public static String ERR_166 = "ERR_166";
    public static String ERR_167 = "ERR_167";
    public static String ERR_168 = "ERR_168";
    public static String ERR_169 = "ERR_169";
    public static String ERR_170 = "ERR_170";
    public static String ERR_171 = "ERR_171";
    public static String ERR_172 = "ERR_172";
    public static String ERR_173 = "ERR_173";
    public static String ERR_174 = "ERR_174";
    public static String ERR_175 = "ERR_175";
    public static String ERR_176 = "ERR_176";
    public static String ERR_177 = "ERR_177";
    public static String ERR_178 = "ERR_178";
    public static String ERR_179 = "ERR_179";
    public static String ERR_180 = "ERR_180";
    public static String ERR_181 = "ERR_181";
    public static String ERR_182 = "ERR_182";
    public static String ERR_183 = "ERR_183";
    public static String ERR_184 = "ERR_184";
    public static String ERR_185 = "ERR_185";
    public static String ERR_186 = "ERR_186";
    public static String ERR_187 = "ERR_187";
    public static String ERR_188 = "ERR_188";
    public static String ERR_189 = "ERR_189";
    public static String ERR_190 = "ERR_190";
    public static String ERR_191 = "ERR_191";
    public static String ERR_192 = "ERR_192";
    public static String ERR_193 = "ERR_193";
    public static String ERR_194 = "ERR_194";
    public static String ERR_195 = "ERR_195";
    public static String ERR_196 = "ERR_196";
    public static String ERR_197 = "ERR_197";
    public static String ERR_198 = "ERR_198";
    public static String ERR_199 = "ERR_199";
    public static String ERR_200 = "ERR_200";
    public static String ERR_201 = "ERR_201";
    public static String ERR_202 = "ERR_202";
    public static String ERR_203 = "ERR_203";
    public static String ERR_204 = "ERR_204";
    public static String ERR_205 = "ERR_205";
    public static String ERR_206 = "ERR_206";
    public static String ERR_207 = "ERR_207";
    public static String ERR_208 = "ERR_208";
    public static String ERR_209 = "ERR_209";
    public static String ERR_210 = "ERR_210";
    public static String ERR_211 = "ERR_211";
    public static String ERR_212 = "ERR_212";
    public static String ERR_213 = "ERR_213";
    public static String ERR_214 = "ERR_214";
    public static String ERR_215 = "ERR_215";
    public static String ERR_216 = "ERR_216";
    public static String ERR_217 = "ERR_217";
    public static String ERR_218 = "ERR_218";
    public static String ERR_219 = "ERR_219";
    public static String ERR_220 = "ERR_220";
    public static String ERR_221 = "ERR_221";
    public static String ERR_222 = "ERR_222";
    public static String ERR_223 = "ERR_223";
    public static String ERR_224 = "ERR_224";
    public static String ERR_225 = "ERR_225";
    public static String ERR_226 = "ERR_226";
    public static String ERR_227 = "ERR_227";
    public static String ERR_228 = "ERR_228";
    public static String ERR_229 = "ERR_229";


    /**
     * 
     * Translate an error code with argument(s)
     *
     * @param err The error code
     * @param args The argument(s)
     * @return
     */
    public static String err( String err, Object... args )
    {
        try
        {
            return err + " " + MessageFormat.format( errBundle.getString( err ), args );
        }
        catch ( Exception e )
        {
            StringBuffer sb = new StringBuffer();
            boolean comma = false;
            for ( Object obj : args )
            {
                if ( comma )
                {
                    sb.append( "," );
                }
                else
                {
                    comma = true;
                }
                sb.append( obj );
            }
            return err + " (" + sb.toString() + ")";
        }
    }


    /**
     * 
     * Translate a message with argument(s)
     *
     * @param msg The message
     * @param args The argument(s)
     * @return
     */
    public static String msg( String msg, Object... args )
    {
        try
        {
            return MessageFormat.format( msgBundle.getString( msg ), args );
        }
        catch ( MissingResourceException mre )
        {
            try
            {
                return MessageFormat.format( msg, args );
            }
            catch ( Exception e )
            {
                StringBuffer sb = new StringBuffer();
                boolean comma = false;
                for ( Object obj : args )
                {
                    if ( comma )
                    {
                        sb.append( "," );
                    }
                    else
                    {
                        comma = true;
                    }
                    sb.append( obj );
                }
                return msg + " (" + sb.toString() + ")";
            }
        }
    }

}
