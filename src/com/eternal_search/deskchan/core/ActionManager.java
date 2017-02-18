package com.eternal_search.deskchan.core;

import java.util.Map;
import java.util.HashMap;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.lang.reflect.Constructor;

public abstract class ActionManager {
   private static Map _actions = new HashMap<String, Action>();

   public static void add(String name, AbstractAction action) {
      if (_actions.containsKey(name))
         throw new RuntimeException(String.format("Key '%s' already exists!", name));

      _actions.put(name, action);
   }

   public static Action get(String name) {
      if (!_actions.containsKey(name))
         throw new RuntimeException(String.format("No key '%s' found!", name));

      return (Action) _actions.get(name);
   }
}