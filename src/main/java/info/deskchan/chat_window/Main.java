package info.deskchan.chat_window;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;

import java.text.SimpleDateFormat;
import java.util.*;


public class Main implements Plugin {
    private static PluginProxyInterface pluginProxy;

    private class ChatPhrase{
        public final String text;
        public final int sender;
        public final Date date;
        public ChatPhrase(String text,int sender){
            this.text=text;
            this.sender=sender;
            date=new Date();
        }
        public HashMap<String,Object> toMap(){
            HashMap<String,Object> map=new HashMap<>();
            String color=null;
            String senderName=null;
            switch(sender){
                case 0: {
                    color="#F00";
                    senderName="Тян";
                } break;
                case 1: {
                    color="#00F";
                    senderName="Юзер";
                } break;
                case 2: color="#888"; break;
            }
            map.put("text","("+new SimpleDateFormat("HH:mm:ss").format(date)+") "+(senderName!=null ? "["+senderName+"]: " : "")+text+"\n");
            map.put("color",color);
            return map;
        }
    }

    private boolean chatIsOpened=false;

    private LinkedList<ChatPhrase> history;
    private ArrayList<HashMap<String,Object>> historyToChat(){
        ArrayList<HashMap<String,Object>> ret=new ArrayList<>();
        List<ChatPhrase> list=history.subList(Math.max(history.size() - 8, 0), history.size());
        HashMap<String,Object> current;
        if(list.size()==0){
            ret.add(new ChatPhrase("История сообщений пуста",2).toMap());
            return ret;
        }
        for(ChatPhrase phrase : list){
            ret.add(phrase.toMap());
        }
        return ret;

    }

    @Override
    public boolean initialize(PluginProxyInterface newPluginProxy) {
        pluginProxy=newPluginProxy;
        log("setup chat window started");
        history=new LinkedList<>();
        pluginProxy.addMessageListener("chat:setup", (sender, tag, data) -> {
            chatIsOpened=true;
            setupChat();
        });
        pluginProxy.addMessageListener("chat:setup", (sender, tag, data) -> {
            chatIsOpened=true;
            setupChat();
        });
        pluginProxy.addMessageListener("chat:closed", (sender, tag, data) -> {
            chatIsOpened=false;
        });
        pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
            put("name", pluginProxy.getString("chat.open"));
            put("msgTag", "chat:setup");
        }});
        pluginProxy.addMessageListener("DeskChan:say", (sender, tag, data) -> {
            String text;
            if(data instanceof Map){
                text=(String) ((HashMap<String,Object>) data).getOrDefault("text", "");
            } else {
                text=data.toString();
            }
            Map<String, Object> delayData = new HashMap<>();
            delayData.put("delay", 1);
            pluginProxy.sendMessage("core-utils:notify-after-delay", delayData, (s, d) -> {
                history.add(new ChatPhrase(text,0));
                setupChat();
            });
        });
        pluginProxy.addMessageListener("DeskChan:user-said", (sender, tag, data) -> {
            history.add(new ChatPhrase((String) ((HashMap<String,Object>) data).getOrDefault("value", ""),1));
            setupChat();
        });
        log("setup chat window completed");
        setupChat();
        return true;
    }

    void setupChat() {
        if(!chatIsOpened) return;
        pluginProxy.sendMessage("gui:show-custom-window", new HashMap<String, Object>() {{
            LinkedList<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
            list.add(new HashMap<String, Object>() {{
                put("id", "name");
                put("type", "TextField");
                put("enterTag","DeskChan:user-said");
            }});
            list.add(new HashMap<String, Object>() {{
                put("id", "textname");
                put("type", "CustomizableTextArea");
                put("width",400d);
                put("height",200d);
                put("value",historyToChat());
            }});
            put("controls", list);
            put("name",pluginProxy.getString("chat"));
            put("onClose","chat:closed");
        }});
    }
    static void log(String text) {
        pluginProxy.log(text);
    }

    static void log(Throwable e) {
        pluginProxy.log(e);
    }
}
