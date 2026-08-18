@echo off
title ChainaBridge 0.4.0-alpha.5
cd /d "%~dp0"
java -jar ChainaBridge-0.4.0-alpha.5.jar
if errorlevel 1 (
  echo.
  echo ChainaBridge se cerro con un error. Comprueba que Java 21 este instalado.
  pause
)
