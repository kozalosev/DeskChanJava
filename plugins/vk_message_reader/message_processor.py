# -*- coding: utf-8 -*-

"""This module encapsulates all code related to the process of parsing received messages and generating
result phrases, which will be ready to be shown to the user.
Author: Leonid Kozarin <kozalo@nekochan.ru>
"""

from vk_adapter import MessageSource
from localization.main import Localization


class MessageProcessor:
    """Recommended usage:
        MessageProcessor.get_lambda(say_func)
    """

    _l10n = Localization.get_instance()

    @classmethod
    def get_phrase(cls, source, text, attachments, data):
        """"This method is useful when we want to process the response of vk_adapter.MessageListener.
        
        :param source: USER, CHAT, or GROUP.
        :type source: MessageSource
        
        :param text: Text of the message.
        :type text: str
        
        :param attachments: Dictionary of attachments and forwarded messages.
        :type attachments: dict
        
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

        if attachments:
            attachment_list = cls._parse_attachments(attachments)
            phrase += "\n\n[%s]" % "]\n[".join(attachment_list)

        return phrase

    @classmethod
    def get_lambda(cls, say_func):
        """A convenient shortcut to get a function that can be used in the constructor of VK class.
        :param say_func: A callback function, which will be called with generated string and is expected to show the message to the user.
        :type say_func: callable
        """

        return lambda source, text, attachments, data: say_func(cls.get_phrase(source, text, attachments, data))

    @classmethod
    def _parse_attachments(cls, attachments):
        """Parses attachments that we've got from vk_api. Actually, it rather parses their types. The only type of attachments that is really parsed is a link.
        :param attachments: Dictionary of attachments and forwarded messages.
        :type attachments: dict
        
        :returns: List of localized strings representing the types of the attachments.
        :rtype: list
        """

        l10n = cls._l10n
        attachment_list = []

        if len(attachments) > 0:
            if "fwd" in attachments:
                attachment_list.append(l10n['forwarded_messages'])

            i = 1
            while "attach%i" % i in attachments:
                type = attachments['attach%i_type' % i]

                if type == "link":
                    title = attachments['attach%i_title' % i]
                    url = attachments['attach%i_url' % i]
                    attachment_list.append(u"%s — %s" % (title, url))
                else:
                    try:
                        attachment_list.append(l10n[type])
                    except ValueError:
                        attachment_list.append(l10n['unsupported_attachment'])

                i += 1

        return attachment_list
