from vk_adapter import VK
from busproxy import *
from pluginutils import Localization
from message_processor import MessageProcessor


# Initializes localization mechanism
l10n = Localization.get_instance("localization")

# Initializes vk_adapter and tries to log in using the old data.
# VK takes a callback function as the second argument of the constructor. We utilize a special function `get_lambda` of
# MessageProcessor to get the callback. In its turn, it requires a callback function with one string parameter. Here we
# use the `say` function to show the message to the user.
vk = VK(get_data_dir_path(), MessageProcessor.get_lambda(say))
vk.try_start_listening(say)

# Adds a tab to the options menu. The user can enter his credentials there.
send_message("gui:add-options-tab", {'name': 'VK', 'msgTag': "vk:login", 'controls': [
    {'type': 'TextField', 'id': 'login', 'label': l10n["login"]},
    {'type': 'TextField', 'id': 'password', 'label': l10n["password"]},
    {'type': 'Label', 'value': l10n["token_hint"]},
    {'type': 'Button', 'msgTag': 'vk:get-token', 'value': l10n["get_token"]},
    {'type': 'TextField', 'id': 'token', 'label': l10n["token"]}
]})

# Callback which is called when user clicks "Save" in the options.
add_message_listener("vk:login", lambda tag, sender, data: vk.try_start_listening_again(
    data,
    success_callback=lambda: say(l10n['logged']),
    fail_callback=say
))
# This one is called when user clicks "Get token" in the options.
add_message_listener("vk:get-token", lambda tag, sender, data: vk.get_token())

# Stops the listening before unloading the plugin.
add_cleanup_handler(lambda: vk.stop_listening())
