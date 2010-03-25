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
    // public static String ERR_45 = "ERR_45";
    // public static String ERR_46 = "ERR_46";
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
    // public static String ERR_67 = "ERR_67";
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
    // public static String ERR_92 = "ERR_92";
    // public static String ERR_93 = "ERR_93";
    // public static String ERR_94 = "ERR_94";
    // public static String ERR_95 = "ERR_95";
    // public static String ERR_96 = "ERR_96";
    // public static String ERR_97 = "ERR_97";
    // public static String ERR_98 = "ERR_98";
    // public static String ERR_99 = "ERR_99";
    // public static String ERR_100 = "ERR_100";
    // public static String ERR_101 = "ERR_101";
    // public static String ERR_102 = "ERR_102";
    // public static String ERR_103 = "ERR_103";
    // public static String ERR_104 = "ERR_104";
    // public static String ERR_105 = "ERR_105";
    // public static String ERR_106 = "ERR_106";
    // public static String ERR_107 = "ERR_107";
    // public static String ERR_108 = "ERR_108";
    // public static String ERR_109 = "ERR_109";
    // public static String ERR_110 = "ERR_110";
    // public static String ERR_111 = "ERR_111";
    // public static String ERR_112 = "ERR_112";
    // public static String ERR_113 = "ERR_113";
    // public static String ERR_114 = "ERR_ 114";
    public static String ERR_115 = "ERR_115";
    public static String ERR_116 = "ERR_116";
    public static String ERR_117 = "ERR_117";
    public static String ERR_118 = "ERR_118";
    public static String ERR_119 = "ERR_119";
    public static String ERR_120 = "ERR_120";
    public static String ERR_121 = "ERR_121";
    public static String ERR_122 = "ERR_122";
    // public static String ERR_123 = "ERR_123";
    public static String ERR_124 = "ERR_124";
    public static String ERR_125 = "ERR_125";
    public static String ERR_126 = "ERR_126";
    public static String ERR_127 = "ERR_127";
    public static String ERR_128 = "ERR_128";
    // public static String ERR_129 = "ERR_129";
    // public static String ERR_130 = "ERR_130";
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
    public static String ERR_230 = "ERR_230";
    public static String ERR_231 = "ERR_231";
    public static String ERR_232 = "ERR_232";
    public static String ERR_233 = "ERR_233";
    public static String ERR_234 = "ERR_234";
    public static String ERR_235 = "ERR_235";
    public static String ERR_236 = "ERR_236";
    public static String ERR_237 = "ERR_237";
    public static String ERR_238 = "ERR_238";
    public static String ERR_239 = "ERR_239";
    public static String ERR_240 = "ERR_240";
    public static String ERR_241 = "ERR_241";
    public static String ERR_242 = "ERR_242";
    public static String ERR_243 = "ERR_243";
    public static String ERR_244 = "ERR_244";
    public static String ERR_245 = "ERR_245";
    public static String ERR_246 = "ERR_246";
    public static String ERR_247 = "ERR_247";
    public static String ERR_248 = "ERR_248";
    public static String ERR_249 = "ERR_249";
    public static String ERR_250 = "ERR_250";
    public static String ERR_251 = "ERR_251";
    public static String ERR_252 = "ERR_252";
    public static String ERR_253 = "ERR_253";
    public static String ERR_254 = "ERR_254";
    public static String ERR_255 = "ERR_255";
    public static String ERR_256 = "ERR_256";
    public static String ERR_257 = "ERR_257";
    public static String ERR_258 = "ERR_258";
    public static String ERR_259 = "ERR_259";
    public static String ERR_260 = "ERR_260";
    public static String ERR_261 = "ERR_261";
    public static String ERR_262 = "ERR_262";
    public static String ERR_263 = "ERR_263";
    public static String ERR_264 = "ERR_264";
    public static String ERR_265 = "ERR_265";
    public static String ERR_266 = "ERR_266";
    public static String ERR_267 = "ERR_267";
    public static String ERR_268 = "ERR_268";
    public static String ERR_269 = "ERR_269";
    public static String ERR_270 = "ERR_270";
    public static String ERR_271 = "ERR_271";
    public static String ERR_272 = "ERR_272";
    public static String ERR_273 = "ERR_273";
    public static String ERR_274 = "ERR_274";
    public static String ERR_275 = "ERR_275";
    public static String ERR_276 = "ERR_276";
    public static String ERR_277 = "ERR_277";
    public static String ERR_278 = "ERR_278";
    public static String ERR_279 = "ERR_279";
    public static String ERR_280 = "ERR_280";
    public static String ERR_281 = "ERR_281";
    public static String ERR_282 = "ERR_282";
    public static String ERR_283 = "ERR_283";
    public static String ERR_284 = "ERR_284";
    public static String ERR_285 = "ERR_285";
    public static String ERR_286 = "ERR_286";
    public static String ERR_287 = "ERR_287";
    public static String ERR_288 = "ERR_288";
    public static String ERR_289 = "ERR_289";
    public static String ERR_290 = "ERR_290";
    public static String ERR_291 = "ERR_291";
    public static String ERR_292 = "ERR_292";
    public static String ERR_293 = "ERR_293";
    public static String ERR_294 = "ERR_294";
    public static String ERR_295 = "ERR_295";
    public static String ERR_296 = "ERR_296";
    public static String ERR_297 = "ERR_297";
    // public static String ERR_298 = "ERR_298";
    // public static String ERR_299 = "ERR_299";
    // public static String ERR_300 = "ERR_300";
    public static String ERR_301 = "ERR_301";
    public static String ERR_302 = "ERR_302";
    public static String ERR_303 = "ERR_303";
    public static String ERR_304 = "ERR_304";
    public static String ERR_305 = "ERR_305";
    public static String ERR_306 = "ERR_306";
    public static String ERR_307 = "ERR_307";
    public static String ERR_308 = "ERR_308";
    public static String ERR_309 = "ERR_309";
    public static String ERR_310 = "ERR_310";
    public static String ERR_311 = "ERR_311";
    public static String ERR_312 = "ERR_312";
    public static String ERR_313 = "ERR_313";
    public static String ERR_314 = "ERR_314";
    public static String ERR_315 = "ERR_315";
    public static String ERR_316 = "ERR_316";
    public static String ERR_317 = "ERR_317";
    // public static String ERR_318 = "ERR_318";
    public static String ERR_319 = "ERR_319";
    public static String ERR_320 = "ERR_320";
    public static String ERR_321 = "ERR_321";
    public static String ERR_322 = "ERR_322";
    public static String ERR_323 = "ERR_323";
    public static String ERR_324 = "ERR_324";
    public static String ERR_325 = "ERR_325";
    public static String ERR_326 = "ERR_326";
    public static String ERR_327 = "ERR_327";
    public static String ERR_328 = "ERR_328";
    public static String ERR_329 = "ERR_329";
    public static String ERR_330 = "ERR_330";
    public static String ERR_331 = "ERR_331";
    public static String ERR_332 = "ERR_332";
    public static String ERR_333 = "ERR_333";
    // public static String ERR_334 = "ERR_334";
    public static String ERR_335 = "ERR_335";
    public static String ERR_336 = "ERR_336";
    public static String ERR_337 = "ERR_337";
    public static String ERR_338 = "ERR_338";
    public static String ERR_339 = "ERR_339";
    // public static String ERR_340 = "ERR_340";
    public static String ERR_341 = "ERR_341";
    public static String ERR_342 = "ERR_342";
    public static String ERR_343 = "ERR_343";
    // public static String ERR_344 = "ERR_344";
    public static String ERR_345 = "ERR_345";
    public static String ERR_346 = "ERR_346";
    public static String ERR_347 = "ERR_347";
    public static String ERR_348 = "ERR_348";
    public static String ERR_349 = "ERR_349";
    public static String ERR_350 = "ERR_350";
    public static String ERR_351 = "ERR_351";
    public static String ERR_352 = "ERR_352";
    public static String ERR_353 = "ERR_353";
    public static String ERR_354 = "ERR_354";
    public static String ERR_355 = "ERR_355";
    // public static String ERR_356 = "ERR_356";
    public static String ERR_357 = "ERR_357";
    public static String ERR_358 = "ERR_358";
    public static String ERR_359 = "ERR_359";
    public static String ERR_360 = "ERR_360";
    public static String ERR_361 = "ERR_361";
    public static String ERR_362 = "ERR_362";
    public static String ERR_363 = "ERR_363";
    public static String ERR_364 = "ERR_364";
    public static String ERR_365 = "ERR_365";
    public static String ERR_366 = "ERR_366";
    public static String ERR_367 = "ERR_367";
    public static String ERR_368 = "ERR_368";
    public static String ERR_369 = "ERR_369";
    public static String ERR_370 = "ERR_370";
    public static String ERR_371 = "ERR_371";
    public static String ERR_372 = "ERR_372";
    public static String ERR_373 = "ERR_373";
    public static String ERR_374 = "ERR_374";
    public static String ERR_375 = "ERR_375";
    public static String ERR_376 = "ERR_376";
    public static String ERR_377 = "ERR_377";
    public static String ERR_378 = "ERR_378";
    public static String ERR_379 = "ERR_379";
    public static String ERR_380 = "ERR_380";
    public static String ERR_381 = "ERR_381";
    public static String ERR_382 = "ERR_382";
    public static String ERR_383 = "ERR_383";
    public static String ERR_384 = "ERR_384";
    public static String ERR_385 = "ERR_385";
    public static String ERR_386 = "ERR_386";
    public static String ERR_387 = "ERR_387";
    // public static String ERR_388 = "ERR_388";
    public static String ERR_389 = "ERR_389";
    public static String ERR_390 = "ERR_390";
    public static String ERR_391 = "ERR_391";
    // public static String ERR_392 = "ERR_392";
    public static String ERR_393 = "ERR_393";
    // public static String ERR_394 = "ERR_394";
    // public static String ERR_395 = "ERR_395";
    public static String ERR_396 = "ERR_396";
    public static String ERR_397 = "ERR_397";
    // public static String ERR_398 = "ERR_398";
    public static String ERR_399 = "ERR_399";
    public static String ERR_400 = "ERR_400";
    public static String ERR_401 = "ERR_401";
    public static String ERR_402 = "ERR_402";
    public static String ERR_403 = "ERR_403";
    // public static String ERR_404 = "ERR_404";
    public static String ERR_405 = "ERR_405";
    public static String ERR_406 = "ERR_406";
    public static String ERR_407 = "ERR_407";
    public static String ERR_408 = "ERR_408";
    public static String ERR_409 = "ERR_409";
    public static String ERR_410 = "ERR_410";
    public static String ERR_411 = "ERR_411";
    public static String ERR_412 = "ERR_412";
    public static String ERR_413 = "ERR_413";
    public static String ERR_414 = "ERR_414";
    public static String ERR_415 = "ERR_415";
    public static String ERR_416 = "ERR_416";
    public static String ERR_417 = "ERR_417";
    public static String ERR_418 = "ERR_418";
    public static String ERR_419 = "ERR_419";
    public static String ERR_420 = "ERR_420";
    public static String ERR_421 = "ERR_421";
    public static String ERR_422 = "ERR_422";
    public static String ERR_423 = "ERR_423";
    public static String ERR_424 = "ERR_424";
    public static String ERR_425 = "ERR_425";
    public static String ERR_426 = "ERR_426";
    public static String ERR_427 = "ERR_427";
    public static String ERR_428 = "ERR_428";
    public static String ERR_429 = "ERR_429";
    public static String ERR_430 = "ERR_430";
    public static String ERR_431 = "ERR_431";
    public static String ERR_432 = "ERR_432";
    public static String ERR_433 = "ERR_433";
    public static String ERR_434 = "ERR_434";
    public static String ERR_435 = "ERR_435";
    public static String ERR_436 = "ERR_436";
    public static String ERR_437 = "ERR_437";
    public static String ERR_438 = "ERR_438";
    public static String ERR_439 = "ERR_439";
    public static String ERR_440 = "ERR_440";
    public static String ERR_441 = "ERR_441";
    // public static String ERR_442 = "ERR_442";
    public static String ERR_443 = "ERR_443";
    public static String ERR_444 = "ERR_444";
    public static String ERR_445 = "ERR_445";
    public static String ERR_446 = "ERR_446";
    public static String ERR_447 = "ERR_447";
    public static String ERR_448 = "ERR_448";
    public static String ERR_449 = "ERR_449";
    public static String ERR_450 = "ERR_450";
    public static String ERR_451 = "ERR_451";
    public static String ERR_452 = "ERR_452";
    public static String ERR_453 = "ERR_453";
    public static String ERR_454 = "ERR_454";
    public static String ERR_455 = "ERR_455";
    public static String ERR_456 = "ERR_456";
    // public static String ERR_457 = "ERR_457";
    // public static String ERR_458 = "ERR_458";
    // public static String ERR_459 = "ERR_459";
    // public static String ERR_460 = "ERR_460";
    // public static String ERR_461 = "ERR_461";
    // public static String ERR_462 = "ERR_462";
    // public static String ERR_463 = "ERR_463";
    public static String ERR_464 = "ERR_464";
    public static String ERR_465 = "ERR_465";
    public static String ERR_466 = "ERR_466";
    public static String ERR_467 = "ERR_467";
    public static String ERR_468 = "ERR_468";
    // public static String ERR_469 = "ERR_469";
    // public static String ERR_470 = "ERR_470";
    // public static String ERR_471 = "ERR_471";
    public static String ERR_472 = "ERR_472";
    public static String ERR_473 = "ERR_473";
    public static String ERR_474 = "ERR_474";
    public static String ERR_475 = "ERR_475";
    public static String ERR_476 = "ERR_476";
    public static String ERR_477 = "ERR_477";
    public static String ERR_478 = "ERR_478";
    public static String ERR_479 = "ERR_479";
    public static String ERR_480 = "ERR_480";
    public static String ERR_481 = "ERR_481";
    public static String ERR_482 = "ERR_482";
    public static String ERR_483 = "ERR_483";
    public static String ERR_484 = "ERR_484";
    public static String ERR_485 = "ERR_485";
    // public static String ERR_486 = "ERR_486";
    public static String ERR_487 = "ERR_487";
    // public static String ERR_488 = "ERR_488";
    public static String ERR_489 = "ERR_489";
    public static String ERR_490 = "ERR_490";
    public static String ERR_491 = "ERR_491";
    public static String ERR_492 = "ERR_492";
    public static String ERR_493 = "ERR_493";
    public static String ERR_494 = "ERR_494";
    public static String ERR_495 = "ERR_495";
    // public static String ERR_496 = "ERR_496";
    public static String ERR_497 = "ERR_497";
    public static String ERR_498 = "ERR_498";
    public static String ERR_499 = "ERR_499";
    public static String ERR_500 = "ERR_500";
    public static String ERR_501 = "ERR_501";
    // public static String ERR_502 = "ERR_502";
    public static String ERR_503 = "ERR_503";
    public static String ERR_504 = "ERR_504";
    public static String ERR_505 = "ERR_505";
    public static String ERR_506 = "ERR_506";
    public static String ERR_507 = "ERR_507";
    public static String ERR_508 = "ERR_508";
    public static String ERR_509 = "ERR_509";
    public static String ERR_510 = "ERR_510";
    public static String ERR_511 = "ERR_511";
    public static String ERR_512 = "ERR_512";
    public static String ERR_513 = "ERR_513";
    public static String ERR_514 = "ERR_514";
    public static String ERR_515 = "ERR_515";
    public static String ERR_516 = "ERR_516";
    public static String ERR_517 = "ERR_517";
    public static String ERR_518 = "ERR_518";
    public static String ERR_519 = "ERR_519";
    public static String ERR_520 = "ERR_520";
    public static String ERR_521 = "ERR_521";
    public static String ERR_522 = "ERR_522";
    public static String ERR_523 = "ERR_523";
    public static String ERR_524 = "ERR_524";
    public static String ERR_525 = "ERR_525";
    public static String ERR_526 = "ERR_526";
    public static String ERR_527 = "ERR_527";
    public static String ERR_528 = "ERR_528";
    public static String ERR_529 = "ERR_529";
    // public static String ERR_530 = "ERR_530";
    public static String ERR_531 = "ERR_531";
    public static String ERR_532 = "ERR_532";
    public static String ERR_533 = "ERR_533";
    public static String ERR_534 = "ERR_534";
    public static String ERR_535 = "ERR_535";
    public static String ERR_536 = "ERR_536";
    public static String ERR_537 = "ERR_537";
    public static String ERR_538 = "ERR_538";
    public static String ERR_539 = "ERR_539";
    public static String ERR_540 = "ERR_540";
    public static String ERR_541 = "ERR_541";
    public static String ERR_542 = "ERR_542";
    public static String ERR_543 = "ERR_543";
    public static String ERR_544 = "ERR_544";
    public static String ERR_545 = "ERR_545";
    public static String ERR_546 = "ERR_546";
    public static String ERR_547 = "ERR_547";
    public static String ERR_548 = "ERR_548";
    public static String ERR_549 = "ERR_549";
    public static String ERR_550 = "ERR_550";
    public static String ERR_551 = "ERR_551";
    public static String ERR_552 = "ERR_552";
    public static String ERR_553 = "ERR_553";
    public static String ERR_554 = "ERR_554";
    public static String ERR_555 = "ERR_555";
    public static String ERR_556 = "ERR_556";
    public static String ERR_557 = "ERR_557";
    public static String ERR_558 = "ERR_558";
    public static String ERR_559 = "ERR_559";
    public static String ERR_560 = "ERR_560";
    public static String ERR_561 = "ERR_561";
    public static String ERR_562 = "ERR_562";
    public static String ERR_563 = "ERR_563";
    public static String ERR_564 = "ERR_564";
    public static String ERR_565 = "ERR_565";
    public static String ERR_566 = "ERR_566";
    public static String ERR_567 = "ERR_567";
    public static String ERR_568 = "ERR_568";
    public static String ERR_569 = "ERR_569";
    public static String ERR_570 = "ERR_570";
    public static String ERR_571 = "ERR_571";
    public static String ERR_572 = "ERR_572";
    public static String ERR_573 = "ERR_573";
    public static String ERR_574 = "ERR_574";
    public static String ERR_575 = "ERR_575";
    public static String ERR_576 = "ERR_576";
    public static String ERR_577 = "ERR_577";
    // public static String ERR_578 = "ERR_578";
    // public static String ERR_579 = "ERR_579";
    // public static String ERR_580 = "ERR_580";
    public static String ERR_581 = "ERR_581";
    // public static String ERR_582 = "ERR_582";
    // public static String ERR_583 = "ERR_583";
    // public static String ERR_584 = "ERR_584";
    // public static String ERR_585 = "ERR_585";
    // public static String ERR_586 = "ERR_586";
    // public static String ERR_587 = "ERR_587";
    // public static String ERR_588 = "ERR_588";
    // public static String ERR_589 = "ERR_589";
    // public static String ERR_590 = "ERR_590";
    public static String ERR_591 = "ERR_591";
    public static String ERR_592 = "ERR_592";
    public static String ERR_593 = "ERR_593";
    public static String ERR_594 = "ERR_594";
    // public static String ERR_595 = "ERR_595";
    public static String ERR_596 = "ERR_596";
    public static String ERR_597 = "ERR_597";
    // public static String ERR_598 = "ERR_598";
    public static String ERR_599 = "ERR_599";
    public static String ERR_600 = "ERR_600";
    public static String ERR_601 = "ERR_601";
    public static String ERR_602 = "ERR_602";
    public static String ERR_603 = "ERR_603";
    public static String ERR_604 = "ERR_604";
    public static String ERR_605 = "ERR_605";
    public static String ERR_606 = "ERR_606";
    public static String ERR_607 = "ERR_607";
    public static String ERR_608 = "ERR_608";
    public static String ERR_609 = "ERR_609";
    public static String ERR_610 = "ERR_610";
    public static String ERR_611 = "ERR_611";
    public static String ERR_612 = "ERR_612";
    public static String ERR_613 = "ERR_613";
    public static String ERR_614 = "ERR_614";
    public static String ERR_615 = "ERR_615";
    public static String ERR_616 = "ERR_616";
    public static String ERR_617 = "ERR_617";
    public static String ERR_618 = "ERR_618";
    public static String ERR_619 = "ERR_619";
    // public static String ERR_620 = "ERR_620";
    // public static String ERR_621 = "ERR_621";
    public static String ERR_622 = "ERR_622";
    public static String ERR_623 = "ERR_623";
    public static String ERR_624 = "ERR_624";
    public static String ERR_625 = "ERR_625";
    public static String ERR_626 = "ERR_626";
    public static String ERR_627 = "ERR_627";
    public static String ERR_628 = "ERR_628";
    public static String ERR_629 = "ERR_629";
    public static String ERR_630 = "ERR_630";
    public static String ERR_631 = "ERR_631";
    public static String ERR_632 = "ERR_632";
    public static String ERR_633 = "ERR_633";
    public static String ERR_634 = "ERR_634";
    public static String ERR_635 = "ERR_635";
    public static String ERR_636 = "ERR_636";
    public static String ERR_637 = "ERR_637";
    public static String ERR_638 = "ERR_638";
    public static String ERR_639 = "ERR_639";
    public static String ERR_640 = "ERR_640";
    public static String ERR_641 = "ERR_641";
    public static String ERR_642 = "ERR_642";
    // public static String ERR_643 = "ERR_643";
    // public static String ERR_644 = "ERR_644";
    // public static String ERR_645 = "ERR_645";
    public static String ERR_646 = "ERR_646";
    public static String ERR_647 = "ERR_647";
    public static String ERR_648 = "ERR_648";
    public static String ERR_649 = "ERR_649";
    public static String ERR_650 = "ERR_650";
    public static String ERR_651 = "ERR_651";
    public static String ERR_652 = "ERR_652";
    public static String ERR_653 = "ERR_653";
    public static String ERR_654 = "ERR_654";
    public static String ERR_655 = "ERR_655";
    public static String ERR_656 = "ERR_656";
    public static String ERR_657 = "ERR_657";
    public static String ERR_658 = "ERR_658";
    public static String ERR_659 = "ERR_659";
    public static String ERR_660 = "ERR_660";
    public static String ERR_661 = "ERR_661";
    public static String ERR_662 = "ERR_662";
    public static String ERR_663 = "ERR_663";
    public static String ERR_664 = "ERR_664";
    // public static String ERR_665 = "ERR_665";
    public static String ERR_666 = "ERR_666";
    public static String ERR_667 = "ERR_667";
    public static String ERR_668 = "ERR_668";
    public static String ERR_669 = "ERR_669";
    public static String ERR_670 = "ERR_670";
    public static String ERR_671 = "ERR_671";
    public static String ERR_672 = "ERR_672";
    // public static String ERR_673 = "ERR_673";
    public static String ERR_674 = "ERR_674";
    public static String ERR_675 = "ERR_675";
    public static String ERR_676 = "ERR_676";
    public static String ERR_677 = "ERR_677";
    public static String ERR_678 = "ERR_678";
    public static String ERR_679 = "ERR_679";
    public static String ERR_680 = "ERR_680";
    public static String ERR_681 = "ERR_681";
    public static String ERR_682 = "ERR_682";
    public static String ERR_683 = "ERR_683";
    public static String ERR_684 = "ERR_684";
    public static String ERR_685 = "ERR_685";
    public static String ERR_686 = "ERR_686";
    public static String ERR_687 = "ERR_687";
    // public static String ERR_688 = "ERR_688";
    public static String ERR_689 = "ERR_689";
    public static String ERR_690 = "ERR_690";
    public static String ERR_691 = "ERR_691";
    public static String ERR_692 = "ERR_692";
    // public static String ERR_693 = "ERR_693";
    public static String ERR_694 = "ERR_694";
    public static String ERR_695 = "ERR_695";
    // public static String ERR_696 = "ERR_696";
    public static String ERR_697 = "ERR_697";
    public static String ERR_698 = "ERR_698";
    public static String ERR_699 = "ERR_699";
    public static String ERR_700 = "ERR_700";
    public static String ERR_701 = "ERR_701";
    public static String ERR_702 = "ERR_702";
    public static String ERR_703 = "ERR_703";
    public static String ERR_704 = "ERR_704";
    public static String ERR_705 = "ERR_705";
    public static String ERR_706 = "ERR_706";
    public static String ERR_707 = "ERR_707";
    public static String ERR_708 = "ERR_708";
    public static String ERR_709 = "ERR_709";
    // public static String ERR_710 = "ERR_710";
    public static String ERR_711 = "ERR_711";
    public static String ERR_712 = "ERR_712";
    public static String ERR_713 = "ERR_713";
    public static String ERR_714 = "ERR_714";
    public static String ERR_715 = "ERR_715";
    public static String ERR_716 = "ERR_716";
    public static String ERR_717 = "ERR_717";
    public static String ERR_718 = "ERR_718";
    public static String ERR_719 = "ERR_719";
    public static String ERR_720 = "ERR_720";
    public static String ERR_721 = "ERR_721";
    public static String ERR_722 = "ERR_722";
    public static String ERR_723 = "ERR_723";
    public static String ERR_724 = "ERR_724";
    public static String ERR_725 = "ERR_725";
    // public static String ERR_726 = "ERR_726";
    public static String ERR_727 = "ERR_727";
    public static String ERR_728 = "ERR_728";
    public static String ERR_729 = "ERR_729";
    public static String ERR_730 = "ERR_730";
    public static String ERR_731 = "ERR_731";
    public static String ERR_732 = "ERR_732";
    public static String ERR_733 = "ERR_733";


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
