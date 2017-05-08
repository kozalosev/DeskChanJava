// Imports file content into the scope.
load(bus.getPluginDirPath() + "/utils.js");
// Import Java types.
var Timer = Java.type("java.util.Timer");
var TimerTask = Java.type("java.util.TimerTask");
var ArrayList = Java.type("java.util.ArrayList");
// More information about Nashorn API you can find on the following web page:
// http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/


// Shows the welcome message.
bus.sendMessage("DeskChan:say", {text: "Hello!"});
// A shortcut function.
bus.say('And again but in Russian: "Привет!"');
// Prints information messages to the console.
bus.log("Plugin directory: " + bus.getPluginDirPath());
bus.log("Data directory: " + bus.getDataDirPath());
bus.log("Root directory: " + bus.getRootDirPath());
// Adds the "Test" item into the popup menu.
bus.sendMessage("DeskChan:register-simple-action", {name: "Test", msgTag: buildTag(TAG_MENUACTION)});

// Let's print identical messages if the user clicks on the character or selects the option in the popup menu.
var showTestMessageFunc = function(sender, tag, data) { bus.say("It works!") };
bus.addMessageListener(buildTag(TAG_MENUACTION), showTestMessageFunc);
bus.addMessageListener("gui-events:character-left-click", showTestMessageFunc);

// Adds the options tab.
// Note that we have to create an ArrayList explicitly because of a bug in Nashorn.
// It can't cast a JavaScript array to a List by itself.
var controls = new ArrayList();
controls.add({ type: "Label", value: "Use this text field to test some piece of code or play around with API." });
controls.add({ type: "TextField", id: TAG_CODE, label: "Code" });
bus.sendMessage("gui:setup-options-tab", {name: "Test JavaScript", msgTag: buildTag(TAG_SAVE_OPTIONS), controls: controls});

// Prints a message when user clicks on the "Save" button.
bus.addMessageListener(buildTag(TAG_SAVE_OPTIONS), function(sender, tag, data) {
    eval(data[TAG_CODE]);
});


// This piece of code demonstrates how we can use and extend Java classes.
// Shows random float point numbers every minute.
var TimerAction = Java.extend(TimerTask, {
    run: function() {
        bus.say(Math.random());
    }
});

var timer = new Timer();
timer.schedule(new TimerAction(), TIMER_DELAY, TIMER_DELAY);

// Here you should stop any actions and release any resources which the plugin is used.
bus.addCleanupHandler(function() {
    timer.cancel();
    timer.purge();
});
