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
@echo off
setlocal
if defined SANGO_JAVA_BIN (
  set JAVA=%SANGO_JAVA_BIN%\java
) else (
  set JAVA=java
)
set ARGS=%*
:analyze_args
if "%~1" == "" goto analyze_args_end
if "%ACT%" == "skip" (
    set ACT=
) else if "%ACT%" == "m-opt" (
    set ULIB=%~1
    set ACT=
) else if "%~1" == "-modules" (
    set ACT=m-opt
) else if "%~1" == "-m" (
    set ACT=m-opt
) else if "%~1" == "-verbose" (
    set ACT=skip
) else if "%~1" == "-quiet" (
    set ACT=skip
)
shift
goto analyze_args
:analyze_args_end
if not "%ULIB%" == "" (
  set LIB=%ULIB%;LLIIBB
) else (
  set LIB=LLIIBB
)
call %JAVA% -cp "%LIB%" org.sango_lang.RuntimeEngine -L "LLIIBB" %ARGS%
exit /b %ERRORLEVEL%
