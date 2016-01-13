/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package samples.websocket.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import samples.websocket.behavoir.CloseWithCodeWebSocketHandler;
import samples.websocket.behavoir.RandomByteStreamWebSocketHandler;
import samples.websocket.echo.DefaultEchoService;
import samples.websocket.echo.EchoService;
import samples.websocket.echo.EchoWebSocketHandler;
import samples.websocket.greeting.DefaultGreetingService;
import samples.websocket.greeting.GreetingService;
import samples.websocket.greeting.GreetingWebSocketHandler;

@Configuration
@EnableAutoConfiguration
@EnableWebSocket
public class SampleWebSocketsApplication extends SpringBootServletInitializer implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(SampleWebSocketsApplication.class, args);
    }

    @Bean
    public WebSocketHandler closeWithCodeWebSocketHandler() {
        return new CloseWithCodeWebSocketHandler();
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SampleWebSocketsApplication.class);
    }

    @Bean
    public EchoService echoService() {
        return new DefaultEchoService(null);
    }

    @Bean
    public WebSocketHandler echoWebSocketHandler() {
        return new EchoWebSocketHandler(echoService());
    }

    @Bean
    public GreetingService greetingService() {
        return new DefaultGreetingService();
    }

    @Bean
    public WebSocketHandler greetingWebSocketHandler() {
        return new GreetingWebSocketHandler(greetingService());
    }

    @Bean
    public WebSocketHandler randomByteStreamWebSocketHandler() {
        return new RandomByteStreamWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(echoWebSocketHandler(), "/echo").setAllowedOrigins("*").withSockJS();
        registry.addHandler(greetingWebSocketHandler(), "/hello").setAllowedOrigins("*").withSockJS();
        registry.addHandler(closeWithCodeWebSocketHandler(), "/code").setAllowedOrigins("*").withSockJS();
        registry.addHandler(randomByteStreamWebSocketHandler(), "/stream").setAllowedOrigins("*").withSockJS();
    }

}
