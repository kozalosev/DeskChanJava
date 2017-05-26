# -*- coding: utf-8 -*-

"""This module encapsulates all code related to the process of parsing received messages and generating
result phrases, which will be ready to be shown to the user.
Author: Leonid Kozarin <kozalo@nekochan.ru>
"""

from vk_adapter import MessageSource
from busprovider import BusProvider


class MessageProcessor:
    """Recommended usage:
        MessageProcessor.get_lambda(say_func)
    """

    _l10n = BusProvider.get_localization()

    @classmethod
    def get_phrase(cls, source, text, forwarded_messages, attachments, data):
        """"This method is useful when we want to process the response of vk_adapter.MessageListener.
        
        :param source: USER, CHAT, or GROUP.
        :type source: MessageSource
        
        :param text: Text of the message.
        :type text: str

        :param forwarded_messages: List of forwarded messages.
        :type forwarded_messages: list
        
        :param attachments: List of attachments.
        :type attachments: list
        
        :param data: Other optional data: first and last name of the sender, a chat name or group name.
        :type data: dict
        
        :returns: The correctly formed phrase that is ready to be shown to the user.
        :rtype: str
        """

        l10n = cls._l10n

        if source == MessageSource.USER:
            phrase = l10n['msg_from_user'] % (data['first_name'], data['last_name'])
        elif source == MessageSource.CHAT:
            phrase = l10n['msg_from_chat'] % (data['first_name'], data['last_name'], data['chat_name'])
        elif source == MessageSource.GROUP:
            phrase = l10n['msg_from_group'] % (data['group_name'])
        else:
            phrase = l10n['msg_from_unsupported_source']

        if text:
            phrase += "\n\n%s" % text

        if forwarded_messages or attachments:
            phrase += '\n\n'

            if forwarded_messages:
                phrase += l10n['forwarded_messages'] + '\n'

            if attachments:
                attachment_list = []
                for attachment in attachments:
                    attachment_type = attachment['type']
                    if attachment_type == "link":
                        link = attachment['link']
                        title = link['title']
                        url = link['url']
                        attachment_list.append(u"%s — %s" % (title, url))
                    else:
                        try:
                            attachment_list.append(l10n[attachment_type])
                        except ValueError:
                            attachment_list.append(l10n['unsupported_attachment'])

                phrase += "[%s]" % "]\n[".join(attachment_list)

        return phrase

    @classmethod
    def get_lambda(cls, say_func):
        """A convenient shortcut to get a function that can be used in the constructor of VK class.
        :param say_func: A callback function, which will be called with generated string and is expected to show the message to the user.
        :type say_func: callable
        """

        return lambda source, text, fwd_messages, attachments, data: say_func(cls.get_phrase(source, text, fwd_messages, attachments, data))
