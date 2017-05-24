"""Wrapper over the vk_api module.
Author: Leonid Kozarin <kozalo@nekochan.ru>
"""

from vk_api import VkApi, AuthError
from enum import Enum
from busprovider import BusProvider
import os


class MessageSource(Enum):
    """Source of a message: another user, chat, or group."""

    USER = 0
    CHAT = 1
    GROUP = 2


class MessageListener:
    """Worker. Creates a new thread and listening for incoming messages."""

    DEFAULT_REFRESH_DELAY = 5   # in seconds

    def __init__(self, session):
        """Constructor
        :param session: An instance of VkApi.
        :type session: VkApi
        """

        self._session = session
        self._running = False
        self._stop_flag = False

    def start(self, listener_callback, refresh_delay=DEFAULT_REFRESH_DELAY):
        """Creates a new thread and listening for new messages.

        :param listener_callback: A callback function, which will be called when a message is received.
        :type listener_callback: callable

        :param refresh_delay: A time in seconds. The more a value, the more network traffic is saved.
        :type refresh_delay: int
        """

        from threading import Thread

        t = Thread(target=self._longpoll_loop, args=(listener_callback, refresh_delay))
        t.start()

    def stop(self):
        """Stops the listening loop."""

        if self._running:
            self._stop_flag = True

    def disconnect(self):
        """Closes the connection and terminates the listening loop."""

        self.stop()
        self._session.http.close()

    @property
    def listening(self):
        """Property.
        :returns: True if the listening loop is running. False otherwise.
        :rtype: bool
        """
        return self._running

    def _longpoll_loop(self, callback, refresh_delay):
        """Main method. It's listening to new incoming messages in a separate thread.
        
        :param callback: A callback function, which will be called when a message is received.
        :type callback: callable

        :param refresh_delay: A time in seconds. The more a value, the more network traffic is saved.
        :type refresh_delay: int
        """

        import time

        def get_messages(last_message_id=0):
            response = api.messages.get(out=0, count=1, last_message_id=last_message_id)
            if len(response['items']) > 0:
                last_message_id = response['items'][0]['id']
            return response['items'], last_message_id


        self._running = True
        api = self._session.get_api()
        _, last_message_id = get_messages()

        while True:
            if self._stop_flag:
                break

            messages, last_message_id = get_messages(last_message_id)

            for message in messages:
                text = message['body']
                attachments = message['attachments'] if "attachments" in message else None
                forwarded_messages = message['fwd_messages'] if "fwd_messages" in message else None
                group_name, first_name, last_name, chat_name = (None,) * 4

                if "group_id" in message:
                    source = MessageSource.GROUP
                    group = api.groups.getById(group_id=message['group_id'])
                    group_name = group['name']
                else:
                    user = api.users.get(user_ids=message['user_id'])[0]
                    first_name, last_name = user['first_name'], user['last_name']

                    if "chat_id" in message:
                        source = MessageSource.CHAT
                        chat = api.messages.getChat(chat_id=message['chat_id'])
                        chat_name = chat['title']
                    else:
                        source = MessageSource.USER

                data = {}
                for var in ('group_name', 'first_name', 'last_name', 'chat_name'):
                    if var in locals():
                        data[var] = locals()[var]

                callback(source, text, forwarded_messages, attachments, data)

            time.sleep(refresh_delay)

        self._running = False
        self._stop_flag = False


class Auth:
    """Abstract class, which has means to authorize the user."""

    APP_ID = 5970490
    last_error = None

    @classmethod
    def get_token(cls):
        """Opens a special web page in the browser to let the user log in to VK and get a new token."""

        import webbrowser

        webbrowser.open_new_tab("https://oauth.vk.com/authorize?client_id=%i&display=page&\
redirect_uri=https://oauth.vk.com/blank.html&scope=messages,offline&response_type=token&v=5.62" % cls.APP_ID)

    @classmethod
    def login(cls, login=None, password=None, token=None, config_filename="vk_config.json"):
        """Tries to authorize the user on VK. Can be used without any arguments if credentials are already in the configuration file.
        
        :param login: If passed and a token is not passed, it will be used together with a password.
        :type login: str
        :type password: str
        
        :param token: Can be not only a token, but a whole web address. If a token is provided it will be used instead of login and password.
        :type token: str
        
        :param config_filename: vk_api saves information about the session here.
        :type config_filename: str
        
        :returns: An instance of VkApi.
        :rtype: VkApi
        """

        import re
        from vk_api.exceptions import BadPassword, AccountBlocked, ApiError

        if token:
            pattern = "access_token=([a-z0-9]{85})&"
            matches = re.search(pattern, token)
            if matches:
                token = matches.group(1)

        l10n = BusProvider.get_localization()

        try:
            session = VkApi(login, password, token=token, app_id=cls.APP_ID, scope="messages,offline",
                            config_filename=config_filename)
            session.auth()
            # More precise test. Trying to execute API query...
            session.get_api().messages.get(count=1)
        except BadPassword:
            cls.last_error = l10n['bad_password']
            return None
        except AccountBlocked:
            cls.last_error = l10n['account_blocked']
            return None
        except (AuthError, ApiError) as err:
            cls.last_error = err
            return None

        return session


class VK:
    """Public interface to the end users of the module."""

    last_error = None

    def __init__(self, config_dir, response_listener):
        """Constructor.
        
        :param config_dir: A path to the directory where settings and credentials will be saved.
        :type config_dir: str
        
        :param response_listener: A callback function, which will be called for each incoming message.
        :type response_listener: callable
        """

        credentials_file = os.path.join(config_dir, "credentials.json")
        self._settings = BusProvider.get_settings(credentials_file)
        self._config_dir = config_dir
        self._response_listener = response_listener
        self._listener = None

    def _try_start_listening(self, credentials, refresh_delay):
        """Tries to authorize the user and initialize the process of listening.

        :param credentials: Login and password, or a token. Or nothing.
        :type credentials: dict

        :param refresh_delay: A time in seconds. The more a value, the more network traffic is saved.
        :type refresh_delay: int
        """

        from vk_api.exceptions import ApiError

        # We should stop the previously started listener if it exists.
        self.stop_listening()

        config_file = os.path.join(self._config_dir, "vk_config.json")
        if credentials:
            session = Auth.login(config_filename=config_file, **credentials)
        else:
            session = Auth.login(config_filename=config_file)

        l10n = BusProvider.get_localization()
        message_template = "%s\n\n%s"

        if session:
            settings = BusProvider.get_settings()
            settings.set("refresh_delay", refresh_delay)
            if hasattr(session, "token") and "access_token" in session.token and settings['token'] != session.token['access_token']:
                settings.set("token", session.token['access_token'])

            self._listener = MessageListener(session)
            try:
                self._listener.start(self._response_listener, refresh_delay)
            except ApiError as error_msg:
                self.last_error = message_template % (l10n['api_error'], error_msg)
                return None
            return self._listener
        else:
            self.last_error = message_template % (l10n['login_fail'], Auth.last_error)
            return None

    def try_start_listening(self, fail_callback):
        """To start listening, try this method first. It uses already saved credentials if they exist.
        :param fail_callback: A callback, which will be called with AuthError exception as an argument in case of a failure.
        :type fail_callback: callable
        """

        settings = self._settings
        l10n = BusProvider.get_localization()

        if settings['token']:
            credentials = { 'token': settings['token'] }
            refresh_delay = settings['refresh_delay'] if "refresh_delay" in settings else MessageListener.DEFAULT_REFRESH_DELAY
            if not self._try_start_listening(credentials, refresh_delay) and callable(fail_callback):
                fail_callback(self.last_error)
        elif callable(fail_callback):
            fail_callback(l10n['no_login_data'])

    def try_start_listening_again(self, credentials, refresh_delay, success_callback, fail_callback):
        """Use this method to authorize using either login and password, or a token.
        
        :param credentials: Login and password, or a token.
        :type credentials: dict

        :param refresh_delay: A time in seconds. The more a value, the more network traffic is saved.
        :type refresh_delay: int
        
        :param success_callback: A callback, which will be called if the credentials are right and we logged in successfully.
        :type success_callback: callable
        
        :param fail_callback: A callback, which will be called with AuthError exception as an argument in case of a failure.
        :type fail_callback: callable
        """

        if self._try_start_listening(credentials, refresh_delay):
            if callable(success_callback):
                success_callback()
        else:
            if callable(fail_callback):
                fail_callback(self.last_error)

    def stop_listening(self):
        """Stops the listening loop."""

        if isinstance(self._listener, MessageListener) and self._listener.listening:
            self._listener.stop()

    def disconnect(self):
        """Closes the connection and terminates the listening loop."""

        if isinstance(self._listener, MessageListener):
            self._listener.disconnect()

    @staticmethod
    def get_token():
        """Opens a special web page in the browser to let the user log in to VK and get a new token."""

        Auth.get_token()
