"""Wrapper over the vk_api module.
Author: Leonid Kozarin <kozalo@nekochan.ru>
"""

from vk_api import VkApi, AuthError
from vk_api.longpoll import VkLongPoll, VkEventType
from enum import Enum
from settings import Settings
import os


class MessageSource(Enum):
    """Source of a message: another user, chat, or group."""

    USER = 0
    CHAT = 1
    GROUP = 2


class MessageListener:
    """Worker. Creates a new thread and listening for incoming messages."""

    def __init__(self, session):
        """Constructor
        :param session: An instance of VkApi.
        :type session: VkApi
        """

        self._session = session
        self._running = False
        self._stop_flag = False

    def start(self, listener_callback):
        """Creates a new thread and listening for new messages.
        :param listener_callback: A callback function which will be called when a message is received.
        :type listener_callback: callable
        """

        from threading import Thread

        longpoll = VkLongPoll(self._session)

        t = Thread(target=self._longpoll_loop, args=(longpoll, listener_callback))
        t.start()

    def stop(self):
        """Closes connection and stops the listening loop."""

        if self._running:
            self._stop_flag = True
            self._session.http.close()

    @property
    def listening(self):
        """Property.
        :returns: True if the listening loop is running. False otherwise.
        :rtype: bool
        """
        return self._running

    def _longpoll_loop(self, longpoll, callback):
        """Main method. It's listening to new incoming messages in a separate thread.
        
        :param longpoll: An instance of VkLongPoll.
        :type longpoll: VkLongPoll
        
        :param callback: A callback function which will be called when a message is received.
        :type callback: callable
        """

        from requests.exceptions import ConnectionError

        self._running = True

        while True:
            if self._stop_flag:
                break

            events = longpoll.check()

            if events:
                try:
                    for event in events:
                        if event.type == VkEventType.MESSAGE_NEW and event.to_me:
                            api = self._session.get_api()

                            text = event.text
                            group_name, first_name, last_name, chat_name = (None,) * 4

                            if event.from_group:
                                source = MessageSource.GROUP
                                group = api.groups.getById(group_id=event.group_id)
                                group_name = group.name
                            else:
                                user = api.users.get(user_ids=event.user_id)[0]
                                first_name, last_name = user['first_name'], user['last_name']

                                if event.from_user:
                                    source = MessageSource.USER
                                elif event.from_chat:
                                    source = MessageSource.CHAT
                                    chat = api.messages.getChat(chat_id=event.chat_id)
                                    chat_name = chat.title
                                else:
                                    raise RuntimeError("Unexpected event source!")

                            data = {}
                            for var in ('group_name', 'first_name', 'last_name', 'chat_name'):
                                if var in locals():
                                    data[var] = locals()[var]

                            callback(source, text, data)
                # When we close the connection the code will be here, in the loop, in most cases.
                # Connection loss will be a cause of an exception. So, in this case we just need
                # to ignore the exception and get out of the loop.
                except ConnectionError as err:
                    if self._stop_flag:
                        break

        self._running = False
        self._stop_flag = False


class Auth:
    """Abstract class which has means to authorize the user."""

    APP_ID = 2895443
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

        if token:
            pattern = "access_token=([a-z0-9]{85})&"
            matches = re.match(pattern, token)
            if matches:
                token = matches.group(1)

        try:
            session = VkApi(login, password, token=token, app_id=cls.APP_ID, scope="messages,offline",
                            config_filename=config_filename)
            session.auth()
        except AuthError as err:
            cls.last_error = err
            return None

        return session


class VK:
    """Public interface to the end users of the module."""

    def __init__(self, config_dir, response_listener):
        """Constructor.
        
        :param config_dir: A path to the directory where settings and credentials will be saved.
        :type config_dir: str
        
        :param response_listener: A callback function which will be called for each incoming message.
        :type response_listener: callable
        """

        credentials_file = os.path.join(config_dir, "credentials.json")
        self._settings = Settings.get_instance(credentials_file)
        self._config_dir = config_dir
        self._response_listener = response_listener
        self._listener = None

    def _try_start_listening(self, credentials):
        """Tries to authorize the user and initialize the process of listening.
        :param credentials: Login and password, or a token. Or nothing.
        :type credentials: dict
        """

        config_file = os.path.join(self._config_dir, "vk_config.json")
        if credentials:
            session = Auth.login(config_filename=config_file, **credentials)
        else:
            session = Auth.login(config_filename=config_file)

        listener = MessageListener(session)
        if listener:
            settings = Settings.get_instance()
            if settings['token'] != session.token['access_token']:
                settings.set("token", session.token['access_token'])

            self._listener = listener
            listener.start(self._response_listener)
            return listener
        else:
            return None

    def try_start_listening(self, fail_callback):
        """To start listening, try this method first. It uses already saved credentials if they exist.
        :param fail_callback: A callback which will be called with AuthError exception as an argument in case of a failure.
        :type fail_callback: callable
        """

        settings = self._settings
        if settings['token'] or settings['login'] and settings['password']:
            credentials = {
                'token': settings['token'],
                'login': settings['login'],
                'password': settings['password']
            }
            if not self._try_start_listening(credentials) and callable(fail_callback):
                fail_callback(Auth.last_error)
        elif callable(fail_callback):
            fail_callback(Auth.last_error)

    def try_start_listening_again(self, credentials, fail_callback):
        """Use this method to authorize using either login and password, or a token.
        
        :param credentials: Login and password, or a token.
        :type credentials: dict
        
        :param fail_callback: A callback which will be called with AuthError exception as an argument in case of a failure.
        :type fail_callback: callable
        """

        if self._try_start_listening(credentials):
            settings = Settings.get_instance()
            for key, value in credentials.items():
                settings[key] = value
            settings.save()
        elif callable(fail_callback):
            fail_callback(Auth.last_error)

    def stop_listening(self):
        if isinstance(self._listener, MessageListener) and self._listener.listening:
            self._listener.stop()

    @staticmethod
    def get_token():
        """Opens a special web page in the browser to let the user log in to VK and get a new token."""

        Auth.get_token()
