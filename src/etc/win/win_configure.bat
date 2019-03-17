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
setlocal
set CWD=%~dp0

if "%1"=="" goto err03

:analyze_args
if "%1"=="" goto analyze_args_end
if "%1"=="-install-to" (
  set INSTALL_DIR=%2
  shift
  shift
) else goto err03
goto analyze_args
:analyze_args_end

if "%INSTALL_DIR%"=="" goto err04

if "%INSTALL_DIR:~0,2%"=="\\" goto install_dir_check_end
if "%INSTALL_DIR:~1,2%"==":\" goto install_dir_check_end
goto err05
:install_dir_check_end
if not "%INSTALL_DIR:~-1%"=="\" set INSTALL_DIR=%INSTALL_DIR%\

set CONFIG_TOOL=java -cp "%CWD%tool;%CWD%lib" ConfigTool
set REPLACE_LIB=%CONFIG_TOOL% replace _INSTALL_DIR_ %INSTALL_DIR%

set S="os.type" = "winnt" ;
echo %S% > %CWD%system.props
set S="line.terminator" = "\r\n" ;
echo %S% >> %CWD%system.props
set S="file.separator_char" = '\\' ;
echo %S% >> %CWD%system.props
set S="path.separator_char" = ';' ;
echo %S% >> %CWD%system.props
set S="file.encoding" = "MS932" ;
echo %S% >> %CWD%system.props
%CONFIG_TOOL% echoq byte_order >> %CWD%system.props
if errorlevel 1 goto err02
%CONFIG_TOOL% echo " = " >> %CWD%system.props
if errorlevel 1 goto err02
%CONFIG_TOOL% get byte_order >> %CWD%system.props
if errorlevel 1 goto err02
%CONFIG_TOOL% echo " ;" >> %CWD%system.props
if errorlevel 1 goto err02
%CONFIG_TOOL% nl >> %CWD%system.props
if errorlevel 1 goto err02

type %CWD%etc\win\install.bat | %REPLACE_LIB% > %CWD%install.bat
if errorlevel 1 goto err01

set RET=0
goto ret

:err01
echo ** ERROR: Failed to copy files.
set RET=1
goto ret

:err02
echo ** ERROR: Failed to make system.props.
set RET=1
goto ret

:err03
echo USAGE: configure -install-to install_dir
set RET=1
goto ret

:err04
echo ** ERROR: Install directory not specified.
set RET=1
goto ret

:err05
echo ** ERROR: Install directory path is not absolute.
set RET=1
goto ret

:ret
exit /b RET
