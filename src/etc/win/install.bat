@echo off
rem ***************************************************************************
rem * MIT License                                                             *
rem * Copyright (c) 2018 Isao Akiyama                                         *
rem *                                                                         *
rem * Permission is hereby granted, free of charge, to any person obtaining   *
rem * a copy of this software and associated documentation files (the         *
rem * "Software"), to deal in the Software without restriction, including     *
rem * without limitation the rights to use, copy, modify, merge, publish,     *
rem * distribute, sublicense, and/or sell copies of the Software, and to      *
rem * permit persons to whom the Software is furnished to do so, subject to   *
rem * the following conditions:                                               *
rem *                                                                         *
rem * The above copyright notice and this permission notice shall be          *
rem * included in all copies or substantial portions of the Software.         *
rem *                                                                         *
rem * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         *
rem * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      *
rem * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  *
rem * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    *
rem * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    *
rem * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       *
rem * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  *
rem ***************************************************************************
setlocal enabledelayedexpansion

set CWD=%~dp0
set INSTALL_DIR=_INSTALL_DIR_
set CONFIG_TOOL=java -cp "%CWD%tool;%CWD%lib" ConfigTool
set REPLACE_LIB=%CONFIG_TOOL% replace LLIIBB %INSTALL_DIR%lib
set MODE=initial

:parse_args
if "%1"=="" goto parse_args_end
if "%1"=="-mode" (
  set MODE=%2
  shift
  shift
) else (
  goto err01a
)
goto parse_args
:parse_args_end

if "%MODE%"=="initial" (
  goto initial_install
) else if "%MODE%"=="upgrade" (
  goto upgrade_install
) else (
  goto err01b
)

:initial_install
if exist %INSTALL_DIR% goto err02a

echo mkdir %INSTALL_DIR%
mkdir %INSTALL_DIR%
if errorlevel 1 goto err04

goto install

:upgrade_install
if not exist %INSTALL_DIR% goto err02b

echo del %INSTALL_DIR%*.txt
del %INSTALL_DIR%*.txt

echo rmdir /s /q %INSTALL_DIR%src
rmdir /s /q %INSTALL_DIR%src

echo rmdir /s /q %INSTALL_DIR%lib\sango
rmdir /s /q %INSTALL_DIR%lib\sango

echo rmdir /s /q %INSTALL_DIR%lib\org
rmdir /s /q %INSTALL_DIR%lib\org

echo rmdir /s /q %INSTALL_DIR%lib\sni_sango
rmdir /s /q %INSTALL_DIR%lib\sni_sango

echo rmdir /s /q %INSTALL_DIR%lib\etc
rmdir /s /q %INSTALL_DIR%lib\etc

echo rmdir /s /q %INSTALL_DIR%bin
rmdir /s /q %INSTALL_DIR%bin

echo rmdir /s /q %INSTALL_DIR%doc
rmdir /s /q %INSTALL_DIR%doc

echo rmdir /s /q %INSTALL_DIR%sample
rmdir /s /q %INSTALL_DIR%sample

echo rmdir /s /q %INSTALL_DIR%etc
rmdir /s /q %INSTALL_DIR%etc

goto install

:install
echo copy %CWD%*.txt %INSTALL_DIR%
copy %CWD%*.txt %INSTALL_DIR%
if errorlevel 1 goto err04

echo xcopy /q /s /e %CWD%src\*.* %INSTALL_DIR%src\
xcopy /q /s /e %CWD%src\*.* %INSTALL_DIR%src\
if errorlevel 1 goto err04

echo xcopy /q /s /e %CWD%lib\*.* %INSTALL_DIR%lib\
xcopy /q /s /e %CWD%lib\*.* %INSTALL_DIR%lib\
if errorlevel 1 goto err04

echo mkdir %INSTALL_DIR%lib\etc
mkdir %INSTALL_DIR%lib\etc
if errorlevel 1 goto err04
echo copy %CWD%system.props %INSTALL_DIR%lib\etc\
copy %CWD%system.props %INSTALL_DIR%lib\etc\
if errorlevel 1 goto err04

echo mkdir %INSTALL_DIR%bin
mkdir %INSTALL_DIR%bin
if errorlevel 1 goto err04
echo type %CWD%bin\win\sangoc.bat ^| %REPLACE_LIB% ^> %INSTALL_DIR%\bin\sangoc.bat
type %CWD%bin\win\sangoc.bat | %REPLACE_LIB% > %INSTALL_DIR%\bin\sangoc.bat
if errorlevel 1 goto err04
echo type %CWD%bin\win\sango.bat ^| %REPLACE_LIB% ^> %INSTALL_DIR%\bin\sango.bat
type %CWD%bin\win\sango.bat | %REPLACE_LIB% > %INSTALL_DIR%\bin\sango.bat
if errorlevel 1 goto err04

echo xcopy /q /s /e %CWD%doc\*.* %INSTALL_DIR%doc\
xcopy /q /s /e %CWD%doc\*.* %INSTALL_DIR%doc\
if errorlevel 1 goto err04

echo xcopy /q /s /e %CWD%sample\*.* %INSTALL_DIR%sample\
xcopy /q /s /e %CWD%sample\*.* %INSTALL_DIR%sample\
if errorlevel 1 goto err04

echo xcopy /q /s /e %CWD%etc\*.* %INSTALL_DIR%etc\
xcopy /q /s /e %CWD%etc\*.* %INSTALL_DIR%etc\
if errorlevel 1 goto err04

set RET=0
goto ret

:err01a
echo **ERROR: Invalide argument. %1
set RET=1
goto ret

:err01b
echo **ERROR: Invalide mode. %MODE%
set RET=1
goto ret

:err02a
echo **ERROR: Install directory %INSTALL_DIR% already exists.  Delete it in advance.
set RET=1
goto ret

:err02b
echo **ERROR: Install directory %INSTALL_DIR% does not exist.
set RET=1
goto ret

:err04
echo ** ERROR: Failed to copy files.
set RET=1
goto ret

:ret
exit /b RET
