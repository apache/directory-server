#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

!define AppName "${app.description}"
!define AppVersion "${app.version}"
!define OutFile "${app.final.name}"
!define ShortName "${app}"
!define SourceDir "${app.base.dir}\src"
!define JRE_VERSION "1.5.0"
!define Vendor "Apache Software Foundation"
!define Project "Apache Directory"
!define Suite "Apache Directory Suite"

!define JAVA_URL "http://java.sun.com/javase/downloads/index_jdk5.jsp"

!macro CreateInternetShortcut FILENAME URL ;ICONFILE ICONINDEX
WriteINIStr "${FILENAME}.url" "InternetShortcut" "URL" "${URL}"
!macroend

!include "MUI.nsh"
!include "Sections.nsh"

Var InstallJRE
Var JREPath



;--------------------------------
;Configuration

  ;General
  Name "${AppName}"
  OutFile "..\${OutFile}.exe"

  ;Folder selection page
  InstallDir "$PROGRAMFILES\${AppName}"

  ;Get install folder from registry if available
  InstallDirRegKey HKLM "SOFTWARE\${Vendor}\${ShortName}" ""

; Installation types
InstType "Full"

BrandingText "${AppName} - ${AppVersion}"
XPStyle on

!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${SourceDir}\main\resources\header_server.bmp"
!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_WELCOMEFINISHPAGE_BITMAP "${SourceDir}\main\resources\welcome_server.bmp"
!define iconfile "${SourceDir}\main\resources\server-installer.ico"
!define MUI_ICON "${iconfile}"
!define MUI_UNICON "${iconfile}"

;--------------------------------
;Pages

  ; License page
  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE "server-win32\LICENSE"

  ; This page checks for JRE. It displays a dialog based on JRE.ini if it needs to install JRE
  ; Otherwise you won't see it.
  ;Page custom CheckInstalledJRE

  ; Define headers for the 'Java installation successfully' page
  #!define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "Java installation complete"
  #!define MUI_PAGE_HEADER_TEXT "Installing Java runtime"
  #!define MUI_PAGE_HEADER_SUBTEXT "Please wait while we install the Java runtime"
  #!define MUI_INSTFILESPAGE_FINISHHEADER_SUBTEXT "Java runtime installed successfully."
  #!insertmacro MUI_PAGE_INSTFILES
  !define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "Installation complete"
  !define MUI_PAGE_HEADER_TEXT "Installing"
  !define MUI_PAGE_HEADER_SUBTEXT "Please wait while ${AppName} is being installed."

  !insertmacro MUI_PAGE_COMPONENTS

  # The main installation directory

    Var SERVER_HOME_DIR
    ;!define MUI_PAGE_CUSTOMFUNCTION_PRE PreServerDir
    !define MUI_DIRECTORYPAGE_VARIABLE          $SERVER_HOME_DIR  ;selected by user
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION  "Server Home Directory"     ;descriptive text
    !define MUI_DIRECTORYPAGE_TEXT_TOP          "Select the directory where you would like to install ${AppName}"  ; GUI page title
    !insertmacro MUI_PAGE_DIRECTORY  ; this pops-up the GUI page

    Var INSTANCE_HOME_DIR
    !define MUI_PAGE_CUSTOMFUNCTION_PRE PreInstanceDir
    !define MUI_DIRECTORYPAGE_VARIABLE          $INSTANCE_HOME_DIR  ;selected by user
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION  "Server Instances Home Directory"     ;descriptive text
    !define MUI_DIRECTORYPAGE_TEXT_TOP          "Select the directory where you would like instance data to be stored.$\n$\nThis directory will be the home location for new instances."  ; GUI page title
    !insertmacro MUI_PAGE_DIRECTORY  ; this pops-up the GUI page

    Var JAVA_HOME_DIR
    !define MUI_DIRECTORYPAGE_VARIABLE          $JAVA_HOME_DIR  ;selected by user
    !define MUI_DIRECTORYPAGE_TEXT_DESTINATION  "Java JDK Home Directory"     ;descriptive text
    !define MUI_DIRECTORYPAGE_TEXT_TOP          "Select the Java home directory that you would like to use for running the installed applications."
    !insertmacro MUI_PAGE_DIRECTORY  ; this pops-up the GUI page


  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH

; Uninstall  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

!cd server-win32


;--------------------------------
;Modern UI Configuration

  !define MUI_ABORTWARNING

;--------------------------------
;Languages

!insertmacro MUI_LANGUAGE "English"


;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecServerFiles ${LANG_ENGLISH} "Apache Directory Server Application Files"
  LangString DESC_SecInstanceFiles ${LANG_ENGLISH} "Example server instance"

  ;Header
  LangString TEXT_JRE_TITLE ${LANG_ENGLISH} "Java Runtime Environment"
  LangString TEXT_JRE_SUBTITLE ${LANG_ENGLISH} "Installation"
  LangString TEXT_PRODVER_TITLE ${LANG_ENGLISH} "Installed version of ${AppName}"
  LangString TEXT_PRODVER_SUBTITLE ${LANG_ENGLISH} "Installation cancelled"

;--------------------------------
;Installer Sections

SectionGroup "Apache Directory Server"
Section "Application Files" SecServerFiles
  SectionIn 1 RO
  SetOutPath "$SERVER_HOME_DIR\bin"
  File /r "bin\*.*"

  SetOutPath "$SERVER_HOME_DIR\lib"
  File /r "lib\*.*"

  SetOutPath "$SERVER_HOME_DIR\conf"
  File /r "conf\*.*"

  SetOutPath "$SERVER_HOME_DIR"
  File "*"
  
    Push "$SERVER_HOME_DIR"
    Push "*.txt"
    Call ConvertFiles

    Push "$SERVER_HOME_DIR\conf"
    Push "*.*"
    Call ConvertFiles

    GetFunctionAddress $R0 ReplaceConfig ; handle to callback fn
    Push $R0
    Push "$SERVER_HOME_DIR\conf\apacheds.conf" ; file to replace in
    Call ReplaceInFile
    
  CreateDirectory "$INSTANCE_HOME_DIR"

  ;Store install folder
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Project} Server" "DisplayName" "${Project} Server - (remove only)"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Project} Server" "DisplayIcon" "$SERVER_HOME_DIR\uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Project} Server" "UninstallString" '"$SERVER_HOME_DIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Project} Server" "NoModify" "1"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Project} Server" "NoRepair" "1"

  # Probably need to filter the file here (put in instance home)


  ;Create uninstaller
  WriteUninstaller "$SERVER_HOME_DIR\Uninstall.exe"

    CreateDirectory "$SMPROGRAMS\Apache Directory Suite\Server"

    !insertmacro CreateInternetShortcut "$SMPROGRAMS\Apache Directory Suite\Server\Basic Users Guide" \
      "http://directory.apache.org/apacheds/1.5/apacheds-v15-basic-users-guide.html"
    !insertmacro CreateInternetShortcut "$SMPROGRAMS\Apache Directory Suite\Server\Advanced Users Guide" \
      "http://directory.apache.org/apacheds/1.5/apacheds-v15-advanced-users-guide.html"
    !insertmacro CreateInternetShortcut "$SMPROGRAMS\Apache Directory Suite\Server\Developers Guide" \
      "http://directory.apache.org/apacheds/1.5/apacheds-v15-developers-guide.html"

    CreateShortCut "$SMPROGRAMS\Apache Directory Suite\Server\Uninstall.lnk" "$SERVER_HOME_DIR\uninstall.exe" "" "$SERVER_HOME_DIR\uninstall.exe" 0

SectionEnd

; This section needs a custom screen to ask for a name for the instance (replace default)
Section "Example Instance" SecInstanceFiles
    SectionIn 1
    StrCpy $R9 "Passed the Example Instance section"
    
    Push "default"
    Call CreateInstanceDirs

    ;I am hand picking the files for now, but we could simplify this by creating a template for new instances
    SetOutPath "$INSTANCE_HOME_DIR\default\conf"
    File "conf\log4j.properties"
    File /oname=apacheds.conf "conf\apacheds-default.conf"
    File "conf\server.xml"
    ; Removing the redundant server.xml file (see DIRSERVER-1112)
    Delete "$SERVER_HOME_DIR\conf\server.xml"

    Push "$INSTANCE_HOME_DIR\default\conf"
    Push "*.*"
    Call ConvertFiles

    SetOutPath "$INSTANCE_HOME_DIR\default\ldif"
    File "conf\example.ldif"

    Push "$INSTANCE_HOME_DIR\default\ldif"
    Push "*.*"
    Call ConvertFiles

    Push "default"
    Push "$INSTANCE_HOME_DIR"
    Call RegisterInstance
SectionEnd
SectionGroupEnd


;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecServerFiles} $(DESC_SecServerFiles)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecInstanceFiles} $(DESC_SecInstanceFiles)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

;---------------------------------
; Functions

; Converts Unix to CR/LF for Windows
Function ConvertFiles
    Pop $R0
    Pop $R1

    FindFirst $R2 $R3 "$R1\$R0"
    loop:
      StrCmp $R3 "" done
      Push "$R1\$R3"
      Call ConvertUnixNewLines
      FindNext $R2 $R3
      Goto loop
    done:
      FindClose $R2

    Pop $R3
    Pop $R2
    Pop $R1
    Pop $R0
FunctionEnd

Function CreateInstanceDirs
    Pop $0
    CreateDirectory "$INSTANCE_HOME_DIR\$0\log"
    CreateDirectory "$INSTANCE_HOME_DIR\$0\conf"
    CreateDirectory "$INSTANCE_HOME_DIR\$0\partitions"
    ; the run dir seems to be irrelevant on Windows, but the InstallationLayout.verifyInstallation() method requires it
    CreateDirectory "$INSTANCE_HOME_DIR\$0\run"
    Pop $0
FunctionEnd

Function RegisterInstance
    Pop $0
    Pop $1
    nsExec::ExecToLog '"$SERVER_HOME_DIR\bin\${ShortName}" -i "$SERVER_HOME_DIR\conf\${ShortName}.conf" set.INSTANCE_HOME="$0" "set.INSTANCE=$1" set.APACHEDS_HOME="$SERVER_HOME_DIR" '
    Pop $1
    Pop $0
FunctionEnd

Function un.RegisterInstance
    Pop $0
    nsExec::ExecToLog '"$SERVER_HOME_DIR\bin\${ShortName}" -r "$SERVER_HOME_DIR\conf\${ShortName}.conf" "set.INSTANCE=$0"'
    Pop $0
FunctionEnd

Function .onInstSuccess
  Push "$INSTANCE_HOME_DIR\conf\apacheds-default.conf"
  Call ConvertUnixNewLines

  StrCmp $R9 "" End

  ; Start the server
  MessageBox MB_YESNO|MB_ICONQUESTION "Do you want to start the default directory instance?" IDYES startService IDNO End
startService:  
  nsExec::ExecToLog '"$SERVER_HOME_DIR\bin\${ShortName}" --start "$SERVER_HOME_DIR\conf\${ShortName}.conf" "set.INSTANCE_HOME=$INSTANCE_HOME_DIR" "set.INSTANCE=default" "set.APACHEDS_HOME=$SERVER_HOME_DIR"'
  
End:
FunctionEnd

;--------------------------------
;Installer Functions

Function .onInit
    SetCurInstType 0
    SetAutoClose false
    StrCpy $SERVER_HOME_DIR "$PROGRAMFILES\Apache Directory Server"
    Call CheckInstalledJRE
    StrCpy $JAVA_HOME_DIR "$JREPath"
FunctionEnd

Function PreServerDir
    ;StrCpy $SERVER_HOME_DIR $INSTDIR
    ;SetAutoClose false
FunctionEnd

Function PreInstanceDir
    StrCpy $INSTANCE_HOME_DIR $SERVER_HOME_DIR\instances
FunctionEnd

Function CheckInstalledJRE
  Push "${JRE_VERSION}"
  Call DetectJRE
  Exch $0	; Get return value from stack

  StrCmp $0 "0" NoFound
  StrCmp $0 "-1" FoundOld
  Goto JREAlreadyInstalled

FoundOld:
  ;!insertmacro MUI_INSTALLOPTIONS_WRITE "/home/ccustine/development/projects/organicelement/libraries/apacheds_trunk/apacheds/server-installers/src/main/installers/jre.ini" "Field 1" "Text" "${AppName} requires a more recent version of the Java Runtime Environment than the one found on your computer.  The installation of JRE ${JRE_VERSION} will start."
  ;!insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  ;!insertmacro MUI_INSTALLOPTIONS_DISPLAY_RETURN "/home/ccustine/development/projects/organicelement/libraries/apacheds_trunk/apacheds/server-installers/src/main/installers/jre.ini"
  Goto MustInstallJRE

NoFound:
  ;MessageBox MB_OK "JRE not found"
  ;!insertmacro MUI_INSTALLOPTIONS_WRITE "/home/ccustine/development/projects/organicelement/libraries/apacheds_trunk/apacheds/server-installers/src/main/installers/jre.ini" "Field 1" "Text" "No Java Runtime Environment could be found on your computer. The installation of JRE v${JRE_VERSION} will start."
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JRE_TITLE)" "$(TEXT_JRE_SUBTITLE)"
  ;!insertmacro MUI_INSTALLOPTIONS_DISPLAY_RETURN "/home/ccustine/development/projects/organicelement/libraries/apacheds_trunk/apacheds/server-installers/src/main/installers/jre.ini"
  Goto MustInstallJRE

MustInstallJRE:
  Exch $0	; $0 now has the installoptions page return value
  ; Do something with return value here
  Pop $0	; Restore $0
  StrCpy $InstallJRE "yes"
  Return

JREAlreadyInstalled:
;  MessageBox MB_OK "No download: ${TEMP2}"
;  MessageBox MB_OK "JRE already installed"
  StrCpy $InstallJRE "no"
  StrCpy $JREPath "$0"

  ;!insertmacro MUI_INSTALLOPTIONS_WRITE "/home/ccustine/development/projects/organicelement/libraries/apacheds_trunk/apacheds/server-installers/src/main/installers/jre.ini" "UserDefinedSection" "JREPath" $JREPATH
  Pop $0		; Restore $0
  Return

FunctionEnd

; Returns: 0 - JRE not found. -1 - JRE found but too old. Otherwise - Path to JAVA EXE

; DetectJRE. Version requested is on the stack.
; Returns (on stack)	"0" on failure (java too old or not installed), otherwise path to java interpreter
; Stack value will be overwritten!

Function DetectJRE
  Exch $0	; Get version requested
		; Now the previous value of $0 is on the stack, and the asked for version of JDK is in $0
  Push $1	; $1 = Java version string (ie 1.5.0)
  Push $2	; $2 = Javahome
  Push $3	; $3 and $4 are used for checking the major/minor version of java
  Push $4
  ;MessageBox MB_OK "Detecting JRE"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ;MessageBox MB_OK "Read : $1"
  StrCmp $1 "" DetectTry2
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "JavaHome"
  ;MessageBox MB_OK "Read 3: $2"
  StrCmp $2 "" DetectTry2
  Goto GetJRE

DetectTry2:
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
  ;MessageBox MB_OK "Detect Read : $1"
  StrCmp $1 "" NoFound
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$1" "JavaHome"
  ;MessageBox MB_OK "Detect Read 3: $2"
  StrCmp $2 "" NoFound

GetJRE:
; $0 = version requested. $1 = version found. $2 = javaHome
  ;MessageBox MB_OK "Getting JRE"
  IfFileExists "$2\bin\java.exe" 0 NoFound
  StrCpy $3 $0 1			; Get major version. Example: $1 = 1.5.0, now $3 = 1
  StrCpy $4 $1 1			; $3 = major version requested, $4 = major version found
  ;MessageBox MB_OK "Want $3 , found $4"
  IntCmp $4 $3 0 FoundOld FoundNew
  StrCpy $3 $0 1 2
  StrCpy $4 $1 1 2			; Same as above. $3 is minor version requested, $4 is minor version installed
  ;MessageBox MB_OK "Want $3 , found $4"
  IntCmp $4 $3 FoundNew FoundOld FoundNew

NoFound:
  ;MessageBox MB_OK "JRE not found"
  Push "0"
  Goto DetectJREEnd

FoundOld:
  MessageBox MB_OK "JRE too old: $3 is older than $4"
;  Push ${TEMP2}
  Push "-1"
  Goto DetectJREEnd
FoundNew:
  ;MessageBox MB_OK "JRE is new: $3 is newer than $4"

  Push "$2"
;  Push "OK"
;  Return
   Goto DetectJREEnd
DetectJREEnd:
	; Top of stack is return value, then r4,r3,r2,r1
	Exch	; => r4,rv,r3,r2,r1,r0
	Pop $4	; => rv,r3,r2,r1r,r0
	Exch	; => r3,rv,r2,r1,r0
	Pop $3	; => rv,r2,r1,r0
	Exch 	; => r2,rv,r1,r0
	Pop $2	; => rv,r1,r0
	Exch	; => r1,rv,r0
	Pop $1	; => rv,r0
	Exch	; => r0,rv
	Pop $0	; => rv
FunctionEnd

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ; Need to parse a list of instances or directories somehow
  Push "default"
  Call un.RegisterInstance

  ; remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${Project} Server"
  DeleteRegKey HKLM  "SOFTWARE\${Vendor}\${Project}\Server"

  ; remove shortcuts, if any.
  RMDir /r "$SMPROGRAMS\Apache Directory Suite\Server"

  ; remove files in root, then all dirs created by the installer.... leave user added or instance dirs.
  Delete "$INSTDIR\*"
  RMDir /r "$INSTDIR\bin"
  RMDir /r "$INSTDIR\conf"
  RMDir /r "$INSTDIR\var"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\log"

SectionEnd

Function ConvertUnixNewLines
    ; Usage:
    ;Push "path\to\text_file.txt"
    ;Call ConvertUnixNewLines

    Exch $R0 ;file #1 path
    Push $R1 ;file #1 handle
    Push $R2 ;file #2 path
    Push $R3 ;file #2 handle
    Push $R4 ;data
    Push $R5

     FileOpen $R1 $R0 r
     GetTempFileName $R2
     FileOpen $R3 $R2 w

     loopRead:
      ClearErrors
      FileRead $R1 $R4
      IfErrors doneRead

       StrCpy $R5 $R4 1 -1
       StrCmp $R5 $\n 0 +4
       StrCpy $R5 $R4 1 -2
       StrCmp $R5 $\r +3
       StrCpy $R4 $R4 -1
       StrCpy $R4 "$R4$\r$\n"

      FileWrite $R3 $R4

     Goto loopRead
     doneRead:

     FileClose $R3
     FileClose $R1

     SetDetailsPrint none
     Delete $R0
     Rename $R2 $R0
     SetDetailsPrint both

    Pop $R5
    Pop $R4
    Pop $R3
    Pop $R2
    Pop $R1
    Pop $R0
FunctionEnd

Function ReplaceInFile
    ;
	Exch $R0 ;file name to search in
	Exch
	Exch $R4 ;callback function handle
	Push $R1 ;file handle
	Push $R2 ;temp file name
	Push $R3 ;temp file handle
	Push $R5 ;line read

	GetTempFileName $R2
  	FileOpen $R1 $R0 r ;file to search in
  	FileOpen $R3 $R2 w ;temp file

loop_read:
 	ClearErrors
 	FileRead $R1 $R5 ;read line
 	Push $R5 ; put line on stack
 	Call $R4
 	Pop $R5 ; read line from stack
 	IfErrors exit
 	FileWrite $R3 $R5 ;write modified line
	Goto loop_read
exit:
  	FileClose $R1
  	FileClose $R3

   	SetDetailsPrint none
  	Delete $R0
  	Rename $R2 $R0
  	Delete $R2
   	SetDetailsPrint both

	; pop in reverse order
	Pop $R5
	Pop $R3
	Pop $R2
	Pop $R1
	Pop $R4
	Pop $R0
FunctionEnd

Function ReplaceConfig
    ; Example usage of ReplaceInFile
    ; Called Like this:
    ;
    ;GetFunctionAddress $R0 ReplaceInSQL ; handle to callback fn
    ;Push $R0
    ;Push "$INSTDIR\template.sql" ; file to replace in
    ;Call ReplaceInFile
	;
	;
	; save R1
	Push $R1
	Exch
	;
	;
	; A sequence of replacements.
        ; the string to replace in is at the top of the stack
    Push "@app.java.home@" ; string to find
	Push "$JAVA_HOME_DIR\bin\java.exe"
	Call StrReplace

	; restore stack
	Exch
	Pop $R1
FunctionEnd

# Uses $0
Function openLinkNewWindow
  Push $3
  Push $2
  Push $1
  Push $0
  ReadRegStr $0 HKCR "http\shell\open\command" ""
# Get browser path
    DetailPrint $0
  StrCpy $2 '"'
  StrCpy $1 $0 1
  StrCmp $1 $2 +2 # if path is not enclosed in " look for space as final char
    StrCpy $2 ' '
  StrCpy $3 1
  loop:
    StrCpy $1 $0 1 $3
    DetailPrint $1
    StrCmp $1 $2 found
    StrCmp $1 "" found
    IntOp $3 $3 + 1
    Goto loop

  found:
    StrCpy $1 $0 $3
    StrCmp $2 " " +2
      StrCpy $1 '$1"'

  Pop $0
  Exec '$1 $0'
  Pop $1
  Pop $2
  Pop $3
FunctionEnd

Var STR_REPLACE_VAR_0
Var STR_REPLACE_VAR_1
Var STR_REPLACE_VAR_2
Var STR_REPLACE_VAR_3
Var STR_REPLACE_VAR_4
Var STR_REPLACE_VAR_5
Var STR_REPLACE_VAR_6
Var STR_REPLACE_VAR_7
Var STR_REPLACE_VAR_8

Function StrReplace
  Exch $STR_REPLACE_VAR_2
  Exch 1
  Exch $STR_REPLACE_VAR_1
  Exch 2
  Exch $STR_REPLACE_VAR_0
    StrCpy $STR_REPLACE_VAR_3 -1
    StrLen $STR_REPLACE_VAR_4 $STR_REPLACE_VAR_1
    StrLen $STR_REPLACE_VAR_6 $STR_REPLACE_VAR_0
    loop:
      IntOp $STR_REPLACE_VAR_3 $STR_REPLACE_VAR_3 + 1
      StrCpy $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_4 $STR_REPLACE_VAR_3
      StrCmp $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_1 found
      StrCmp $STR_REPLACE_VAR_3 $STR_REPLACE_VAR_6 done
      Goto loop
    found:
      StrCpy $STR_REPLACE_VAR_5 $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_3
      IntOp $STR_REPLACE_VAR_8 $STR_REPLACE_VAR_3 + $STR_REPLACE_VAR_4
      StrCpy $STR_REPLACE_VAR_7 $STR_REPLACE_VAR_0 "" $STR_REPLACE_VAR_8
      StrCpy $STR_REPLACE_VAR_0 $STR_REPLACE_VAR_5$STR_REPLACE_VAR_2$STR_REPLACE_VAR_7
      StrLen $STR_REPLACE_VAR_6 $STR_REPLACE_VAR_0
      Goto loop
    done:
  Pop $STR_REPLACE_VAR_1 ; Prevent "invalid opcode" errors and keep the
  Pop $STR_REPLACE_VAR_1 ; stack as it was before the function was called
  Exch $STR_REPLACE_VAR_0
FunctionEnd

!macro _strReplaceConstructor OUT NEEDLE NEEDLE2 HAYSTACK
  Push "${HAYSTACK}"
  Push "${NEEDLE}"
  Push "${NEEDLE2}"
  Call StrReplace
  Pop "${OUT}"
!macroend

!define StrReplace '!insertmacro "_strReplaceConstructor"'

