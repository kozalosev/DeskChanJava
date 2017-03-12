import random
from javax.swing import Timer
from libs.constants import *
from libs.functions import build_tag

# Shows the welcome message.
bus.sendMessage("DeskChan:say", {'text': 'Hello!'})
# Prints information messages to the console.
print("[Test Python] Plugin directory: %s." % bus.getPluginDir())
print("[Test Python] Data directory: %s." % bus.getDataDir())
# Adds the "Test" item into the popup menu.
bus.sendMessage("DeskChan:register-simple-action", {'name': 'Test', 'msgTag': build_tag(TAG_MENUACTION)})

# Let's print identical messages if the user clicks on the character or selects the option in the popup menu.
func_show_test_message = lambda sender, tag, data: bus.sendMessage("DeskChan:say", {'text': 'It works!'})
bus.addMessageListener(build_tag(TAG_MENUACTION), func_show_test_message)
bus.addMessageListener("gui-events:character-left-click", func_show_test_message)

# Adds the options tab.
bus.sendMessage("gui:add-options-tab", {'name': 'Test Python', 'msgTag': build_tag(TAG_SAVE_OPTIONS), 'controls': [
    {
        'type': 'TextField', 'id': TAG_TEXTFIELD, 'label': 'Test',
        'value': 'Type something here and press the button!'
    }
]})

# Prints a message when user clicks on the "Save" button.
bus.addMessageListener(build_tag(TAG_SAVE_OPTIONS), lambda sender, tag, data:
    bus.sendMessage("DeskChan:say", {'text': "You asked me to print: \"%s\"." % data[TAG_TEXTFIELD]})
)

# This piece of code demonstrates how we can use Python and Java modules.
# Shows random float point numbers every minute.
timer = Timer(TIMER_DELAY, lambda action_event:
    bus.sendMessage("DeskChan:say", {'text': random.random()})
)
timer.start()

# Here you should stop any actions and release any resources which the plugin is used.
bus.addCleanupHandler(lambda: timer.stop())
