@echo off
setlocal
mvn -q -DskipTests package
if errorlevel 1 exit /b %errorlevel%
java -jar target\mindmap-course-design-1.0-SNAPSHOT.jar
