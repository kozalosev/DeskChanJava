package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.ActionManager;
import com.eternal_search.deskchan.core.BrowserAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

abstract class Actions {

   static void loadMenuActions(MainWindow window) {

      ActionManager.add("quit", new AbstractAction("�����") {
         @Override
         public void actionPerformed(ActionEvent actionEvent) {
            window.setVisible(false);
            window.dispose();
         }
      });

      ActionManager.add("options", new AbstractAction("���������...") {
         @Override
         public void actionPerformed(ActionEvent actionEvent) {
            Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            window.optionsDialog.setLocation(
                    (screenBounds.width - window.optionsDialog.getWidth()) / 2 + screenBounds.x,
                    (screenBounds.height - window.optionsDialog.getHeight()) / 2 + screenBounds.y
            );
            window.optionsDialog.setVisible(true);
         }
      });

      ActionManager.add("about", new AbstractAction("�������� �������") {
         public void actionPerformed(ActionEvent actionEvent) {
            try {
               URL link = new URL("https://2ch.hk/s/res/1936557.html");
               BrowserAdapter.openWebpage(link);
            } catch (MalformedURLException e) {
               e.printStackTrace();
            }
         }
      });

      ActionManager.add("degrade", new AbstractAction("�������������") {
         public void actionPerformed(ActionEvent actionEvent) {
            try {
               URL link = new URL("https://2ch.hk/b/");
               BrowserAdapter.openWebpage(link);
            } catch (MalformedURLException e) {
               e.printStackTrace();
            }
         }
      });

      ActionManager.add("feed", new RandomMessageAction(window, "���������", new String[] {
           //"��! ��� ��� ��� �������!",
           //"����� ��������?",
           //"�� ����� �� �� ��� � ��������-�� �����?",
           "�-�-�! ��� ������!",
           "������, �� ������!",
           "� ����� ������ ��� ������?"
      }));

      ActionManager.add("sex", new RandomMessageAction(window, "�������?", new String[] {
              //"����, �����, � ���� ������ ������� �����. ����� ������?",
              //"��� ���� �������! �� ��� ���� ��� ���� �����!",
              "���-���-���...",
              "��� �� ���, ��� ��������!",
              "������ ����� ������! ������ ������������, ��� � ������ �����!"
      }));
   }


   static ActionListener getSayAction(MainWindow window) {
      return new RandomMessageAction(window, null, new String[] {
           "������, � �� ���� �� ��� ��������?",
           "����� ������, ������!",
           "����� ��� ���� ������������?",
           "� ������ ����� � ��������. <��-�-�>",
           "�� ������� �� ����� ���������, ������?"
      });
   }
}


class RandomMessageAction extends AbstractAction {
   private String[] phrases;
   private MainWindow window;

   RandomMessageAction(MainWindow window, String label, String[] phrases) {
      super(label);
      this.window = window;
      this.phrases = phrases;
   }

   public void actionPerformed(ActionEvent actionEvent) {
      Random rand = new Random();
      int i = rand.nextInt(this.phrases.length);
      window.showBalloon(this.phrases[i]);
   }
}