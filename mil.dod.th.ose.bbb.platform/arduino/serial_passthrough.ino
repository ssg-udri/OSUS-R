//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
#define SEND_PIN 23
#define WAKEUP_PIN 24
#define LED_PIN 13
#define CONTROLLER Serial2
#define DEVICE Serial1
#define BAUD_RATE 38400
#define SIZE 200

String data = "";

void setup() 
{
  CONTROLLER.begin(BAUD_RATE);
  DEVICE.begin(BAUD_RATE);
  pinMode(SEND_PIN, INPUT);
  pinMode(WAKEUP_PIN, OUTPUT);
  pinMode(LED_PIN, OUTPUT);
  data.reserve(SIZE);
  digitalWrite(LED_PIN, LOW);
}

void sendDeviceData()
{
  if (digitalRead(SEND_PIN))
  {
      CONTROLLER.print(data);
      data = "";
  }
}

void loop() 
{
  if (digitalRead(SEND_PIN))
  {
    digitalWrite(LED_PIN, HIGH);
  }
  else
  {
    digitalWrite(LED_PIN, LOW);
  }
  
  int inDevice = DEVICE.available();
  if (inDevice > 0)
  {
    //Send wakeup signal
    digitalWrite(WAKEUP_PIN, HIGH);
    delay(50);
    digitalWrite(WAKEUP_PIN, LOW);
    
    for (int i = 0; i < inDevice; i++)
    {
      char inChar = (char)DEVICE.read();
      data += inChar;
      sendDeviceData();
    }
  }
  else if (!data.equals(""))
  {
    sendDeviceData();
  }
  
  int inController = CONTROLLER.available();
  if (inController > 0)
  {
     for (int i = 0; i < inController; i++)
     {
       DEVICE.write(CONTROLLER.read());
     }
  }
}
