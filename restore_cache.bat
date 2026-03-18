@echo off
echo Restoring cache from backup...
copy "client\Cache.backup\*" "client\Cache\"
echo Cache restored from backup!
pause
