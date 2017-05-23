import random
from java.util import Timer, TimerTask
from libs.constants import *
from libs.functions import build_tag

# The plugin can communicate to the others using the bus. It's a special global object injected in the interpretator.
# The bus has the same methods as Groovy plugins.
# `busproxy` is a special module that you may use to include all functions of the bus to the scope. After that you
# won't have to use `bus` explicitly. Moreover, it provides more Pythonic aliases (lower case names with underscores)
# for some of the functions.
import busproxy
busproxy.inject(bus, globals())

# Some useful classes I provided for you to make your life a bit easier ;)
from pluginutils import Settings, Localization

# Shows the welcome message.
bus.sendMessage("DeskChan:say", {'text': 'Hello!'})
# Use unicode strings for non-ASCII characters.
# The method say() is a shortcut and sending a message as well.
say(u'And again but in Russian: "Привет!"')
# Prints information messages to the console.
# Note, how we can use aliases from the busproxy.
bus.log("Plugin directory: %s." % bus.getPluginDirPath())
log("Data directory: %s." % get_data_dir_path())
log("Root directory: %s." % get_root_dir_path())
# Adds the "Test" item into the popup menu.
send_message("DeskChan:register-simple-action", {'name': 'Test', 'msgTag': build_tag(TAG_MENUACTION)})

# Let's print identical messages if the user clicks on the character or selects the option in the popup menu.
func_show_test_message = lambda sender, tag, data: say("It works!")
addMessageListener(build_tag(TAG_MENUACTION), func_show_test_message)
add_message_listener("gui-events:character-left-click", func_show_test_message)

# Built-in localization class.
l10n = Localization.get_instance(bus, "localization")

# Adds the options tab.
send_message("gui:setup-options-tab", {'name': 'Test Python', 'msgTag': build_tag(TAG_SAVE_OPTIONS), 'controls': [
    { 'type': 'Label', 'value': l10n['hint_label'] },
    { 'type': 'TextField', 'id': TAG_CODE, 'label': l10n['code_label'] }
]})

# Interpreters any input code and tries to execute it.
def eval_msg(sender, tag, data):
    try:
        eval(data[TAG_CODE])
    except Exception as err:
        log(err)
add_message_listener(build_tag(TAG_SAVE_OPTIONS), eval_msg)


# This piece of code demonstrates how we can use Python and Java modules.
# Shows random float point numbers every minute.
class TimerAction(TimerTask):
    def run(self):
        say(random.random())

timer = Timer()
timer.schedule(TimerAction(), TIMER_DELAY, TIMER_DELAY)


# SECTION OF PURIFICATION
def timer_cleanup():
    timer.cancel()
    timer.purge()

# Here you should stop any actions and release any resources which the plugin is used.
add_cleanup_handler(timer_cleanup)

# Sample of usage the Settings class.
opts = Settings.get_instance(bus)
if opts['run_counter']:
    opts['run_counter'] += 1
else:
    opts['run_counter'] = 1
opts.save()
log("This plugin has been run %i time(s)." % opts['run_counter'])
