@echo off
echo ========================================================
echo  Running Automated Test Suite
echo ========================================================

java -cp "bin;lib/*;resources" com.library.test.SystemValidationTest
