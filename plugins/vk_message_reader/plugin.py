from vk_adapter import VK, MessageSource
from busproxy import *
from localization.main import Localization


# Localization class.
l10n = Localization.get_instance()

# Callback which analyzes incoming messages and shows them to the user.
def show_message(source, text, data):
    if source == MessageSource.USER:
        say(l10n.get("msg_from_user") % (data['first_name'], data['last_name'], text))
    elif source == MessageSource.CHAT:
        say(l10n.get("msg_from_chat") % (data['first_name'], data['last_name'], data['chat_name'], text))
    elif source == MessageSource.GROUP:
        say(l10n.get("msg_from_group") % (data['group_name'], text))

# Initializes vk_adapter and tries to log in using the old data.
vk = VK(get_data_dir_path(), show_message)
vk.try_start_listening(lambda msg_error:
    say(l10n.get("auth_attempt_fail"))
)

# Adds a tab to the options menu. The user can enter his credentials there.
send_message("gui:add-options-tab", {'name': 'VK', 'msgTag': "vk:login", 'controls': [
    {'type': 'TextField', 'id': 'login', 'label': l10n.get("login")},
    {'type': 'TextField', 'id': 'password', 'label': l10n.get("password")},
    {'type': 'Label', 'value': l10n.get("token_hint")},
    {'type': 'Button', 'msgTag': 'vk:get-token', 'value': l10n.get("get_token")},
    {'type': 'TextField', 'id': 'token', 'label': l10n.get("token")}
]})

# Callback which is called when user clicks "Save" in the options.
add_message_listener("vk:login", lambda tag, sender, data: vk.try_start_listening_again(data, lambda msg_error:
    say(l10n.get("login_fail"))
))
# This one is called when user clicks "Get token" in the options.
add_message_listener("vk:get-token", lambda tag, sender, data: vk.get_token())

# Stops the listening before unloading the plugin.
add_cleanup_handler(lambda: vk.stop_listening())
