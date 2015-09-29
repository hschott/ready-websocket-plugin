Sample Websocket Server
-----------------------

Start within this directory:

  mvn spring-boot:run

Provides four websocket endpoints:

  ./echo - Echos all received messages back to the client
  ./hello - Sends "Hello world!" right after connection is established
  ./code - Closes session with close code received in message payload
  ./stream - Sends continuous stream of random 4 byte binary messages

All services are SockJS enabled.
Establish native rfc 6455 websocket connection by suffixing above URIs with /websocket .

