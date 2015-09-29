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

package samples.websocket.behavoir;

import java.util.Random;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class RandomByteStreamWebSocketHandler extends AbstractWebSocketHandler {

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        final byte[] payload = new byte[4];

        Runnable sender = new Runnable() {

            @Override
            public void run() {

                while (true)
                    try {
                        new Random().nextBytes(payload);
                        session.sendMessage(new BinaryMessage(payload));
                        Thread.sleep(10);
                    } catch (Exception e) {
                        return;
                    }
            }
        };

        new Thread(sender).run();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
    }

}
