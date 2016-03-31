# Android Silent SMS Ping

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

## Usage

  - Install from published APK
    - link: https://github.com/itds-consulting/android-ping-sms/releases/download/v1.0/ping-sms.apk
  - Compile it yourself and install on Android connected device
    - ./gradle clean installDebug
