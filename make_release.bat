@echo off
rem ###########################################################################
rem # MIT License                                                             #
rem # Copyright (c) 2018 Isao Akiyama                                         #
rem #                                                                         #
rem # Permission is hereby granted, free of charge, to any person obtaining   #
rem # a copy of this software and associated documentation files (the         #
rem # "Software"), to deal in the Software without restriction, including     #
rem # without limitation the rights to use, copy, modify, merge, publish,     #
rem # distribute, sublicense, and/or sell copies of the Software, and to      #
rem # permit persons to whom the Software is furnished to do so, subject to   #
rem # the following conditions:                                               #
rem #                                                                         #
rem # The above copyright notice and this permission notice shall be          #
rem # included in all copies or substantial portions of the Software.         #
rem #                                                                         #
rem # THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         #
rem # EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      #
rem # MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  #
rem # IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    #
rem # CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    #
rem # TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       #
rem # SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  #
rem ###########################################################################
setlocal

if "%1"=="" goto print_usage

set TARGET_DIR=%1
if "%TARGET_DIR:~-1%"=="\" (
  set TARGET_DIR=%TARGET_DIR:~0,-1%
)
set RELEASE_DIR=%TARGET_DIR%\release

if not exist "%TARGET_DIR%" goto err01
if exist "%RELEASE_DIR%" goto err02

mkdir %RELEASE_DIR%
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\src
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\src\org
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\src\sango
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\src\sni_sango
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\src\bin
if errorlevel 1 goto err03
xcopy /q /s /e src\org\*.java %RELEASE_DIR%\src\org\
if errorlevel 1 goto err03
xcopy /q /s /e src\sango\*.sg %RELEASE_DIR%\src\sango\
if errorlevel 1 goto err03
xcopy /q /s /e src\sni_sango\*.java %RELEASE_DIR%\src\sni_sango\
if errorlevel 1 goto err03
xcopy /q /s /e src\bin\win\*.bat %RELEASE_DIR%\src\bin\win\
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\src\bin\unix
if errorlevel 1 goto err03
copy src\bin\unix\sango %RELEASE_DIR%\src\bin\unix\
if errorlevel 1 goto err03
copy src\bin\unix\sangoc %RELEASE_DIR%\src\bin\unix\
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\lib
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\lib\org
if errorlevel 1 goto err03
xcopy /q /s /e src\org\*.class %RELEASE_DIR%\lib\org\
if errorlevel 1 goto err03
xcopy /q /s /e src\sango\*.sgm %RELEASE_DIR%\lib\sango\
if errorlevel 1 goto err03
xcopy /q /s /e src\sni_sango\*.class %RELEASE_DIR%\lib\sni_sango\
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\bin
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\bin\win
if errorlevel 1 goto err03
xcopy /q src\bin\win\*.bat %RELEASE_DIR%\bin\win\
if errorlevel 1 goto err03
mkdir %RELEASE_DIR%\bin\unix
if errorlevel 1 goto err03
copy src\bin\unix\sango %RELEASE_DIR%\bin\unix\
if errorlevel 1 goto err03
copy src\bin\unix\sangoc %RELEASE_DIR%\bin\unix\
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\doc
if errorlevel 1 goto err03
xcopy /q /s /e src\doc\*.html %RELEASE_DIR%\doc\
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\sample
if errorlevel 1 goto err03
xcopy /q /s /e src\sample\*.sg %RELEASE_DIR%\sample\
if errorlevel 1 goto err03
xcopy /q /s /e src\sample\*.sgm %RELEASE_DIR%\sample\
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\etc
if errorlevel 1 goto err03
xcopy /q /s /e src\etc\*.bat %RELEASE_DIR%\etc\
if errorlevel 1 goto err03
xcopy /q /s /e src\etc\*.sh %RELEASE_DIR%\etc\
if errorlevel 1 goto err03

mkdir %RELEASE_DIR%\tool
if errorlevel 1 goto err03
xcopy /q /s /e tool\*sh %RELEASE_DIR%\tool\
if errorlevel 1 goto err03
xcopy /q /s /e tool\*class %RELEASE_DIR%\tool\

copy LICENSE.txt %RELEASE_DIR%\
if errorlevel 1 goto err03

copy src\etc\README*.txt %RELEASE_DIR%\
if errorlevel 1 goto err03

copy src\etc\win\win_configure.bat %RELEASE_DIR%\
if errorlevel 1 goto err03
copy src\etc\unix\unix-configure.sh %RELEASE_DIR%\
if errorlevel 1 goto err03

rem Remove extra files...
for /r %RELEASE_DIR%\ %%f in (*~) do (
  del /q %%f
  if errorlevel 1 goto err03
)

exit /b 0

:print_usage
echo Usage: make_release.bat target_dir
exit /b 1

:err01
echo ** Error. %TARGET_DIR% does not exist.
exit /b 1

:err02
echo ** Error. %RELEASE_DIR% already exists.
exit /b 1

:err03
echo ** Error occurred.
exit /b 1
