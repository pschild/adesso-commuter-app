# adesso-commuter-app

# Setup
1. Copy `secret.properties.template` to `secret.properties` in root dir and set your endpoint URL.

# ADB usage

## Save dumps of alarms to file

`./adb.exe shell dumpsys alarm > dump.txt`

## Save logs since given time to file

`./adb.exe logcat -d -t '07-11 05:25:00.000' > log.txt`

## Idle mode

### Force the system into idle mode

`./adb.exe shell dumpsys deviceidle force-idle`

### exit idle mode + reactivate the device

`./adb.exe shell dumpsys deviceidle unforce && ./adb.exe shell dumpsys battery reset`

##  Standby mode

### Force the app into App Standby mode

`./adb.exe shell dumpsys battery unplug && ./adb.exe shell am set-inactive de.pschild.adessocommutingnotifier true`

### waking your app

`./adb.exe shell am set-inactive de.pschild.adessocommutingnotifier false && ./adb.exe shell am get-inactive de.pschild.adessocommutingnotifier`

## Investigate Power/Wakelocks

`./adb.exe shell dumpsys power`

`./adb.exe shell dumpsys power | grep WAKE_LOCK`
