/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var ws = new WebSocket('ws://' + location.host + '/recording');
var videoOutput;
var webRtcPeer;

$(document).ready(function () {
    videoOutput = document.getElementById('videoOutput');
    play();
});

window.onbeforeunload = function () {
    ws.close();
}

ws.onmessage = function (message) {
    var parsedMessage = JSON.parse(message.data);
    console.info('Received message: ' + message.data);

    switch (parsedMessage.id) {
        case 'playResponse':
            playResponse(parsedMessage);
            break;
        case 'playEnd':
            playEnd();
            break;
        case 'error':
            setState(NO_CALL);
            onError('Error message from server: ' + parsedMessage.message);
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
        default:
            onError('Unrecognized message', parsedMessage);
    }
}

function onOffer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'start',
        sdpOffer: offerSdp,
        mode: $('input[name="mode"]:checked').val()
    }
    sendMessage(message);
}

function onError(error) {
    console.error(error);
}

function onIceCandidate(candidate) {
    console.log('Local candidate' + JSON.stringify(candidate));

    var message = {
        id: 'onIceCandidate',
        candidate: candidate
    };
    sendMessage(message);
}

function play() {
    console.log("Starting to play recorded video...");

    showSpinner(videoOutput);

    console.log('Creating WebRtcPeer and generating local sdp offer ...');

    var options = {
        remoteVideo: videoOutput,
        mediaConstraints: getConstraints(),
        onicecandidate: onIceCandidate
    }

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
        function (error) {
            if (error)
                return console.error(error);
            webRtcPeer.generateOffer(onPlayOffer);
        });
}

function onPlayOffer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'play',
        sdpOffer: offerSdp
    }
    sendMessage(message);
}

function getConstraints() {
    var mode = $('input[name="mode"]:checked').val();
    var constraints = {
        audio: true,
        video: true
    }

    if (mode == 'video-only') {
        constraints.audio = false;
    } else if (mode == 'audio-only') {
        constraints.video = false;
    }

    return constraints;
}


function playResponse(message) {
    webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
        if (error)
            return console.error(error);
    });
}

function playEnd() {
    hideSpinner(videoOutput);
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    console.log('Senging message: ' + jsonMessage);
    ws.send(jsonMessage);
}

function showSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].poster = './img/transparent-1px.png';
        arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
    }
}

function hideSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].src = '';
        arguments[i].poster = './img/webrtc.png';
        arguments[i].style.background = '';
    }
}