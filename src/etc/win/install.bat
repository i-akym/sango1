@rem ***************************************************************************
@rem * MIT License                                                             *
@rem * Copyright (c) 2018 Isao Akiyama                                         *
@rem *                                                                         *
@rem * Permission is hereby granted, free of charge, to any person obtaining   *
@rem * a copy of this software and associated documentation files (the         *
@rem * "Software"), to deal in the Software without restriction, including     *
@rem * without limitation the rights to use, copy, modify, merge, publish,     *
@rem * distribute, sublicense, and/or sell copies of the Software, and to      *
@rem * permit persons to whom the Software is furnished to do so, subject to   *
@rem * the following conditions:                                               *
@rem *                                                                         *
@rem * The above copyright notice and this permission notice shall be          *
@rem * included in all copies or substantial portions of the Software.         *
@rem *                                                                         *
@rem * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         *
@rem * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      *
@rem * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  *
@rem * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    *
@rem * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    *
@rem * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       *
@rem * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  *
@rem ***************************************************************************
@setlocal enabledelayedexpansion

@set CWD=%~dp0
@set INSTALL_DIR=_INSTALL_DIR_
@set CONFIG_TOOL=java -cp "%CWD%tool;%CWD%lib" ConfigTool
@set REPLACE_LIB=%CONFIG_TOOL% replace LLIIBB %INSTALL_DIR%lib

mkdir %INSTALL_DIR%
@if errorlevel 1 goto err03

copy %CWD%*.txt %INSTALL_DIR%
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%src
@if errorlevel 1 goto err04
xcopy /q /s /e %CWD%src\*.* %INSTALL_DIR%src
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%lib
@if errorlevel 1 goto err04
xcopy /q /s /e %CWD%lib\*.* %INSTALL_DIR%lib
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%lib\etc
@if errorlevel 1 goto err04
copy %CWD%system.props %INSTALL_DIR%lib\etc
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%bin
@if errorlevel 1 goto err04
type %CWD%bin\win\sangoc.bat | %REPLACE_LIB% > %INSTALL_DIR%\bin\sangoc.bat
@if errorlevel 1 goto err04
type %CWD%bin\win\sango.bat | %REPLACE_LIB% > %INSTALL_DIR%\bin\sango.bat
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%doc
@if errorlevel 1 goto err04
xcopy /q /s /e %CWD%doc\*.* %INSTALL_DIR%doc
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%sample
@if errorlevel 1 goto err04
xcopy /q /s /e %CWD%sample\*.* %INSTALL_DIR%sample
@if errorlevel 1 goto err04

mkdir %INSTALL_DIR%etc
@if errorlevel 1 goto err04
xcopy /q /s /e %CWD%etc\*.* %INSTALL_DIR%etc
@if errorlevel 1 goto err04

@set RET=0
@goto ret

:err02
@echo Install directory %INSTALL_DIR% already exists.  Delete it in advance.
@set RET=1
@goto ret

:err03
@echo ** ERROR: Failed to create install directory.
@set RET=1
@goto ret

:err04
@echo ** ERROR: Failed to copy files.
@set RET=1
@goto ret

:ret
@exit /b RET
