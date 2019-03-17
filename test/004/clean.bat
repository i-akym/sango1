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
for /r . %%f in (*.sgm) do (
  del /q %%f
)
for /d /r . %%d in (result) do (
  del /q %%d\*.txt
)
for /r . %%f in (_clean.bat) do (
  if exist %%f call %%f
)
