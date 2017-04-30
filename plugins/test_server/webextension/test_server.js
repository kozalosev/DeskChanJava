"use strict";

const PLUGIN_TAG = "test_server";
const TEST_INTERVAL = 1000;

const DEFAULT_PORT = 6867;
const SPARE_PORT = 10099;


runLocationTesting();


function runLocationTesting() {
    let lastPathname;
    let testFunc = function() {
        if (location.pathname != lastPathname) {
            lastPathname = location.pathname;
            scanPage();
        }
    };

    setInterval(testFunc, TEST_INTERVAL);
}

function scanPage() {
    let nameElement = document.querySelector("#page_info_wrap > div.page_top > h2");
    if (!nameElement)
        return;

    let name = nameElement.innerHTML;
    let editButtonElement = document.querySelector("#profile_edit_act");
    let action = (editButtonElement) ? "greetings" : "curiosity";

    let action_func = function(socket) {
        socket.send(JSON.stringify({
            msgTag: `${PLUGIN_TAG}:${action}`,
            msgData: {"name": name}
        }));
        socket.close();
    };

    tryGetSocket(DEFAULT_PORT, function(success, socket) {
        if (!success) {
            tryGetSocket(SPARE_PORT, function(success, socket) {
                if (success)
                    action_func(socket);
            });
        }

        action_func(socket);
    });
}

function tryGetSocket(port, callback) {
    let socket = new WebSocket(`ws://localhost:${port}`);
    socket.addEventListener("error", function(e) {
        console.log(`Couldn't connect to the DeskChan on port ${port}!`);
        callback(false);
    });
    socket.addEventListener("open", function() {
        callback(true, socket);
    });
}
