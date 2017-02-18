package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.PluginProxy;
import com.eternal_search.deskchan.core.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;

public class MainWindow extends JFrame {
	
	private PluginProxy pluginProxy = null;
	private final CharacterWidget characterWidget = new CharacterWidget(this);
	private BalloonWidget balloonWidget = null;
	private BalloonWindow balloonWindow = null;
	OptionsDialog optionsDialog = null;

	Integer balloonDelay = 5000;
	private Timer balloonTimer = null;
	Integer sayDelay = 600000;
	private Timer sayTimer = null;
	
	void initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		setTitle("deskchan");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
		setAlwaysOnTop(true);
		setFocusableWindowState(false);
		setLayout(null);
		setBackground(new Color(0, 0, 0, 0));
		pack();
		characterWidget.loadImage(Utils.getResourcePath("characters/chocola01.png"));
		setDefaultLocation();
		setContentPane(characterWidget);
		optionsDialog = new OptionsDialog(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent windowEvent) {
				pluginProxy.sendMessage("core:quit", null);
			}
		});
		pluginProxy.addMessageListener("gui:say", ((sender, tag, data) -> {
			showBalloon(data.toString());
		}));

		Actions.loadMenuActions(this);

		this.balloonTimer = new Timer(balloonDelay, e -> {
            if(balloonWidget != null) {
                if(balloonWindow != null) {
                    balloonWindow.dispose();
                } else {
                    remove(MainWindow.this.balloonWidget);
                }

                balloonWindow = null;
                balloonWidget = null;
            }
        });
		balloonTimer.setRepeats(false);

		updateSayTimer();

		Calendar currentTime = Calendar.getInstance();
		int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);
		if (currentHour < 12 && currentHour > 6)
			showBalloon("Доброе утро, Хозяин!");
		else if (currentHour < 17 && currentHour > 6)
			showBalloon("Добрый день, Хозяин!");
		else if (currentHour < 23 && currentHour > 6)
			showBalloon("Добрый вечер, Хозяин!");
		else
			showBalloon("Доброй ночи, Хозяин!");
	}
	
	void setDefaultLocation() {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation(
				(int)screenBounds.getMaxX() - getWidth(),
				(int)screenBounds.getMaxY() - getHeight()
		);
	}
	
	void updateSizes() {
		Dimension characterSize = characterWidget.getPreferredSize();
		Rectangle characterBounds = new Rectangle(new Point(0, 0), characterSize);
		Dimension frameSize = new Dimension(characterSize);
		if (balloonWidget != null) {
			Dimension balloonSize = balloonWidget.getPreferredSize();
			Rectangle balloonBounds = new Rectangle(
					new Point(getX() - balloonSize.width, getY()),
					balloonSize
			);
			if (balloonBounds.getX() < 0) {
				balloonBounds.x = getX() + frameSize.width;
			}
			balloonBounds.height = 100;

			balloonWindow.setBounds(balloonBounds);
		}
		Rectangle frameBounds = new Rectangle(getLocation(), frameSize);
		if (!characterBounds.equals(characterWidget.getBounds())) {
			characterWidget.setBounds(characterBounds);
		}
		if (!frameBounds.equals(getBounds())) {
			setBounds(frameBounds);
		}
	}
	
	private void showBalloon(JComponent component) {
		if (balloonWidget != null) {
			if (balloonWindow != null) {
				balloonWindow.dispose();
			} else {
				remove(balloonWidget);
			}
			balloonWindow = null;
			balloonWidget = null;
		}
		if (component != null) {
			balloonWidget = new BalloonWidget(component, this);
			balloonWindow = new BalloonWindow(balloonWidget);
		}
		updateSizes();
		if (balloonWindow != null) {
			balloonWindow.setVisible(true);
		}

		if (balloonTimer.isRunning()) {
			balloonTimer.stop();
			balloonTimer.restart();
		}

		balloonTimer.setInitialDelay(balloonDelay);
		balloonTimer.start();
	}
	
	void showBalloon(String text) {
		if (text != null) {
			JLabel label = new JLabel(text);
			label.setHorizontalAlignment(JLabel.CENTER);
			showBalloon(label);
		} else {
			showBalloon((JComponent) null);
		}
	}
	
	@Override
	public void dispose() {
		if (balloonTimer.isRunning())
			balloonTimer.stop();

		if (optionsDialog != null) {
			optionsDialog.dispose();
		}
		if (balloonWindow != null) {
			balloonWindow.dispose();
		}
		super.dispose();
	}
	
	void setPosition(Point pos) {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		Rectangle frameBounds = new Rectangle(pos, getSize());
		if (frameBounds.getMaxX() > screenBounds.getMaxX()) {
			frameBounds.x = (int) screenBounds.getMaxX() - frameBounds.width;
		}
		if (frameBounds.getMaxY() > screenBounds.getMaxY()) {
			frameBounds.y = (int)screenBounds.getMaxY() - frameBounds.height;
		}
		frameBounds.x = Math.max(screenBounds.x, frameBounds.x);
		frameBounds.y = Math.max(screenBounds.y, frameBounds.y);
		setLocation(frameBounds.x, frameBounds.y);
		if (balloonWindow != null) {
			updateSizes();
		}
	}
	
	CharacterWidget getCharacterWidget() {
		return characterWidget;
	}
	
	PluginProxy getPluginProxy() {
		return pluginProxy;
	}

	void updateSayTimer() {
		if (sayTimer != null && sayTimer.isRunning())
			sayTimer.stop();

		sayTimer = new Timer(sayDelay, Actions.getSayAction(this));
	}
}
