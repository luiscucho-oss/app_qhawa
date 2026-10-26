// QHAWA: simulacion educativa. No es una medicion ni un indice sanitario.
#include <WiFi.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <PubSubClient.h>
#include <DHTesp.h>
#include <math.h>

const int POT = 34;
const int DHT_PIN=19;
DHTesp dht;
float temperature=NAN, humidity=NAN;
bool dhtOK=false;
uint32_t dhtSampled=0;
const int EYES[2][3] = {{25,26,27},{16,17,18}}; // R,G,B; catodo comun
// Broker publico: solo datos ficticios. Sustituir al integrar la app.
const char* BROKER = "broker.hivemq.com";
const char* TOPIC = "qhawa/demo/477386567410462721/lecturas";
WiFiClient transport;
PubSubClient mqtt(transport);
Adafruit_SSD1306 oled(128,64,&Wire,-1);
bool screenOK = false, brokerEcho = false;
uint32_t sampled=0, sent=0, retried=0, wifiRetried=0, sequence=0;
float pm25 = 0;
int state = 0;
const char* STATES[] = {"VERDE","AMARILLO","ROJO"};

void eyes(int s) {
  for(int e=0;e<2;e++) {
    digitalWrite(EYES[e][0], s>=1 ? HIGH:LOW);
    digitalWrite(EYES[e][1], s<=1 ? HIGH:LOW);
    digitalWrite(EYES[e][2], LOW);
  }
}
void draw() {
  if(!screenOK) return;
  oled.clearDisplay(); oled.setTextColor(SSD1306_WHITE);
  oled.setTextSize(1); oled.setCursor(0,0); oled.print("QHAWA | SIMULACION");
  oled.setCursor(0,11); oled.print("PM2.5 DEMO: "); oled.print(pm25,1);
  oled.setCursor(0,21); oled.print("ug/m3 | "); oled.print(STATES[state]);
  oled.setCursor(0,32);
  if(dhtOK) { oled.print("Temp: "); oled.print(temperature,1); oled.print(" C"); }
  else oled.print("Temp: -- C");
  oled.setCursor(0,43);
  if(dhtOK) { oled.print("Humedad: "); oled.print(humidity,1); oled.print(" %"); }
  else oled.print("DHT22: sin lectura");
  oled.setCursor(0,55);
  oled.print(WiFi.status()!=WL_CONNECTED ? "WiFi conectando" : !mqtt.connected() ? "MQTT conectando" : brokerEcho ? "MQTT eco recibido" : "MQTT conectado");
  oled.display();
}
void received(char* topic, byte* payload, unsigned int length) {
  // Eco del broker; NO confirma recepcion por una app movil.
  brokerEcho=true;
  Serial.print("BROKER RX: "); Serial.write(payload,length); Serial.println();
  draw();
}
void network(uint32_t now) {
  if(WiFi.status()!=WL_CONNECTED) {
    if(now-wifiRetried>=15000) {wifiRetried=now; WiFi.reconnect();}
    return;
  }
  if(!mqtt.connected() && now-retried>=5000) {
    retried=now; brokerEcho=false;
    String id="qhawa-"+String((uint32_t)ESP.getEfuseMac(),HEX)+"-"+String(now);
    if(mqtt.connect(id.c_str())) {
      Serial.println("MQTT conectado");
      if(!mqtt.subscribe(TOPIC)) Serial.println("No se pudo suscribir al eco");
    } else {Serial.print("MQTT error: "); Serial.println(mqtt.state());}
    draw();
  }
  mqtt.loop();
}
void publishReading(uint32_t now) {
  char json[512], tempText[16], humText[16];
  if(dhtOK) { snprintf(tempText,sizeof(tempText),"%.1f",temperature); snprintf(humText,sizeof(humText),"%.1f",humidity); }
  else { strcpy(tempText,"null"); strcpy(humText,"null"); }
  snprintf(json,sizeof(json),
    "{\"device_id\":\"macegato-demo-01\",\"simulated\":true,\"pollutant\":\"PM2.5\",\"value\":%.1f,\"unit\":\"ug/m3\",\"state\":\"%s\",\"threshold_profile\":\"demo_not_health_standard\",\"temperature_c\":%s,\"humidity_pct\":%s,\"dht_ok\":%s,\"uptime_ms\":%lu,\"sequence\":%lu}",
    pm25,STATES[state],tempText,humText,dhtOK ? "true" : "false",(unsigned long)now,(unsigned long)++sequence);
  Serial.println(json);
  if(mqtt.connected()) {
    bool ok=mqtt.publish(TOPIC,json,false);
    Serial.println(ok ? "MQTT TX enviado (QoS 0)" : "MQTT TX fallo");
  } else Serial.println("Sin MQTT: lectura local; no se envio");
}
void setup() {
  Serial.begin(115200);
  for(int e=0;e<2;e++) for(int c=0;c<3;c++) pinMode(EYES[e][c],OUTPUT);
  pinMode(POT,INPUT); analogReadResolution(12);
  dht.setup(DHT_PIN,DHTesp::DHT22);
  Wire.begin(21,22);
  screenOK=oled.begin(SSD1306_SWITCHCAPVCC,0x3C);
  if(!screenOK) Serial.println("OLED no disponible");
  eyes(0); draw();
  WiFi.mode(WIFI_STA); WiFi.begin("Wokwi-GUEST","",6);
  mqtt.setServer(BROKER,1883); mqtt.setCallback(received);
  mqtt.setBufferSize(768); mqtt.setSocketTimeout(2);
  Serial.println("QHAWA DEMO: pot 0-100% = PM2.5 ficticio 0-150 ug/m3");
  Serial.println("Limites DEMO: verde <25, amarillo 25-<75, rojo >=75");
  Serial.print("Tema: "); Serial.println(TOPIC);
}
void loop() {
  uint32_t now=millis();
  if(now-sampled>=250) {
    sampled=now;
    pm25=analogRead(POT)*150.0f/4095.0f;
    state=pm25<25 ? 0 : pm25<75 ? 1 : 2;
    eyes(state); draw();
  }
  if(now-dhtSampled>=2200) {
    dhtSampled=now;
    TempAndHumidity reading=dht.getTempAndHumidity();
    dhtOK=dht.getStatus()==0 && !isnan(reading.temperature) && !isnan(reading.humidity);
    if(dhtOK) { temperature=reading.temperature; humidity=reading.humidity; }
    Serial.print("DHT22: ");
    if(dhtOK) { Serial.print(temperature,1); Serial.print(" C | "); Serial.print(humidity,1); Serial.println(" %"); }
    else Serial.println("error de lectura");
    draw();
  }
  network(now);
  if(now-sent>=5000) {sent=now; publishReading(now);}
  delay(10);
}
