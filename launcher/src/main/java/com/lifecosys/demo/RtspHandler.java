package com.lifecosys.demo;

import java.io.IOException;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class RtspHandler extends TextWebSocketHandler {

    private static final String RTSP_URL = "rtsp://192.168.41.133:554/";

    private final Logger log = LoggerFactory.getLogger(RtspHandler.class);
    private static final Gson gson = new GsonBuilder().create();

    @Autowired
    private KurentoClient kurento;

    WebRtcEndpoint webRtcEndpoint;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
        switch (jsonMessage.get("id").getAsString()) {
        case "play":
            play(session, jsonMessage);
            break;
        case "onIceCandidate": {
            JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();
            IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(), jsonCandidate.get("sdpMid").getAsString(),
                    jsonCandidate.get("sdpMLineIndex").getAsInt());
            webRtcEndpoint.addIceCandidate(candidate);
            break;
        }
        default:
            sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
            break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    private void play(final WebSocketSession session, JsonObject jsonMessage) {
        try {

            // 1. Media logic
            final MediaPipeline pipeline = kurento.createMediaPipeline();
            webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, RTSP_URL).build();
            player.connect(webRtcEndpoint);

            // Player listeners
            player.addErrorListener(new EventListener<ErrorEvent>() {
                @Override
                public void onEvent(ErrorEvent event) {
                    log.info("ErrorEvent for session '{}': {}", session.getId(), event.getDescription());
                    sendPlayEnd(session, pipeline);
                }
            });
            player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
                @Override
                public void onEvent(EndOfStreamEvent event) {
                    log.info("EndOfStreamEvent for session '{}'", session.getId());
                    sendPlayEnd(session, pipeline);
                }
            });

            // 3. SDP negotiation
            String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "playResponse");
            response.addProperty("sdpAnswer", sdpAnswer);

            // 4. Gather ICE candidates
            webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            });

            // 5. Play recorded stream
            player.play();

            synchronized (session) {
                session.sendMessage(new TextMessage(response.toString()));
            }

            webRtcEndpoint.gatherCandidates();
        } catch (Throwable t) {
            log.error("Play error", t);
            sendError(session, t.getMessage());
        }
    }

    public void sendPlayEnd(WebSocketSession session, MediaPipeline pipeline) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("id", "playEnd");
            session.sendMessage(new TextMessage(response.toString()));
        } catch (IOException e) {
            log.error("Error sending playEndOfStream message", e);
        }
        // Release pipeline
        pipeline.release();
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("id", "error");
            response.addProperty("message", message);
            session.sendMessage(new TextMessage(response.toString()));
        } catch (IOException e) {
            log.error("Exception sending message", e);
        }
    }
}
