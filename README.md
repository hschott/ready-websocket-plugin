[![Build Status](https://travis-ci.org/hschott/ready-websocket-plugin.svg)](https://travis-ci.org/hschott/ready-websocket-plugin)

## Ready! API Websocket Plugin

This plugin adds three TestSteps to the functional testing in Ready! API
* one for publishing messages to an websocket server
* one for receiving (and asserting) messages
* and one for dropping connections

It integrates fully with all other Ready! API features like property expansion, property transfers, data-driven testing, etc.

Install the plugin via the Plugin Manager inside Ready! API.

This plugin also runs with SoapUI 5.2. Install it by copying it to <users home>/.soapuios/plugins/ .

This plugin was inspired by [SmartBear/ready-mqtt-plugin](https://github.com/SmartBear/ready-mqtt-plugin) and lots of code has been taken from it.
Thanks to the original authors.



