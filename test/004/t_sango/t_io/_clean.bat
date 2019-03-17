@echo off
setlocal
set CWD=%~dp0
for /d %%d in (%CWD%data\dd*) do (
  rmdir /s /q %%d
)
del /q %CWD%data\d*
