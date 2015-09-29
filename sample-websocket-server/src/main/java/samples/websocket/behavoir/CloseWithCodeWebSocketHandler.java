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

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class CloseWithCodeWebSocketHandler extends AbstractWebSocketHandler {

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Integer code;
        try {
            code = Integer.parseInt(message.getPayload());
            session.close(new CloseStatus(code));
        } catch (NumberFormatException e) {
            session.sendMessage(new TextMessage("unable to parse close code"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] payload = message.getPayload().array();
        switch (payload.length) {
        case 4: {
            int ir = 0;
            for (int i = 0; i < 4; ++i)
                ir += (payload[i] & 0xff) << 8 * i;
            session.close(new CloseStatus(ir));
        }
            break;

        case 8: {
            long lr = 0;
            for (int i = 0; i < 8; ++i)
                lr += (long) (payload[i] & 0xff) << 8 * i;
            session.close(new CloseStatus((int) lr));
        }
            break;

        default:
            session.sendMessage(new TextMessage("unable to parse close code"));
            break;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
    }
}
