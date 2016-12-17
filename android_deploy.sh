#!/bin/bash

# Please be sure, that 'adb' command from Android SDK is in your PATH variable

# exit if errors
set -e

# Some colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Default: Manager instance static IP (104.154.187.219)
server_ip_default="104.154.187.219"

echo -e "The server IP will default to the Google Cloud instance for Group 7 if no address is given. \
Please give an IP address that can be used in the demo configuration."
echo
read -p "Server address for the Android build [${server_ip_default}]: " server_ip
server_ip="${server_ip:-$server_ip_default}"

install_to_device=false
echo
read -p "Do you want the script to install the APK to the Android device? If yes, the device has to be attached now. (y/n): " choice
case "$choice" in
  y|Y ) install_to_device=true;;
  n|N ) install_to_device=false;;
  * ) echo "Please answer 'y' or 'n'. ";;
esac

# build Android package
echo -e "${GREEN}Preparing Android package...${NC}"
cd android/MobileOffloading
sed -i -e "s|10.0.2.2|${server_ip}|g" app/src/main/res/values/strings.xml
chmod +x gradlew
./gradlew assembleDebug
echo -e "${NC}The APK file can be found at ./app/build/outputs/apk/app-debug.apk${NC}"

if ${install_to_device}
then
  echo -e "      ${NC}Installing to device...${NC}"
  adb -d install app/build/outputs/apk/app-debug.apk
  echo -e "      ${NC}Success! Open MobileOffloading application on your Android device.${NC}"
fi