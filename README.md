# Android Silent SMS Ping

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/com.itds.sms.ping) [<img src="getapkfromgithub.png" alt="Download APK from GitHub" height="60">](https://github.com/itds-consulting/android-silent-ping-sms/releases)

  - Does not require rooted device
  - SMS will not be delivered as standard SMS, target user will receive no visual notification about SMS being received

## Description

  - Payload looks like this: `byte[]{0x0A, 0x06, 0x03, (byte) 0xB0, (byte) 0xAF, (byte) 0x82, 0x03, 0x06, 0x6A, 0x00, 0x05}`
  - Full SMS PDU looks like this: `03050020 01f61fe0c91246066833682000412 06050423f00000 0a0603b0af8203066a0005`
    - Where first segment is SMSC (SMS Center), second is user-defined data SMS, last segment is payload specified in userspace
    - Using this payload, remote mobile station (baseband) will not deliver or correctly process the SMS, will only provide ACK (delivery report)

## Links

  - 3GPP 23.040 (originally GSM 03.40) https://en.wikipedia.org/wiki/GSM_03.40
  - 3GPP 23.038 (originally GSM 03.38) https://en.wikipedia.org/wiki/GSM_03.38

## Build

  - Compile it yourself and install on Android connected device
    - ./gradle clean installDebug
    
## License

   The project is licensed under the [GNU General Public License version 3 (or newer)](https://github.com/itds-consulting/android-silent-ping-sms/blob/master/LICENSE)

