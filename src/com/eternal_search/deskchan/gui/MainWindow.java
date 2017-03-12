package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.PluginProxy;
import com.eternal_search.deskchan.core.Utils;
import dorkbox.systemTray.*;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class MainWindow extends JFrame {

	static final int WINDOW_MODE_NORMAL = 0;
	static final int WINDOW_MODE_TOP_MOST = 1;
	
	private PluginProxy pluginProxy = null;
	private Path dataDirPath = null;
	private CharacterWidget characterWidget;
	private BalloonWidget balloonWidget = null;
	private BalloonWindow balloonWindow = null;
	OptionsDialog optionsDialog = null;
	private Timer balloonTimer = null;
	private static final ResourceBundle strings = ResourceBundle.getBundle("gui-strings");
	static final Properties properties = new Properties();
	Font balloonTextFont = null;
	int balloonDefaultTimeout;
	private String currentCharacterImage = "normal";
	private PriorityQueue<BalloonMessage> balloonQueue = new PriorityQueue<>();
	private int windowMode = WINDOW_MODE_NORMAL;
	private static Image applicationIcon = null;
	private final JPopupMenu popupMenu = new JPopupMenu();
	private SystemTray systemTray = null;
	
	final Action quitAction = new AbstractAction(getString("quit")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			setVisible(false);
			dispose();
		}
	};
	final Action optionsAction = new AbstractAction(getString("options")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			optionsDialog.updateOptions();
			Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			optionsDialog.setLocation(
					(screenBounds.width - optionsDialog.getWidth()) / 2 + screenBounds.x,
					(screenBounds.height - optionsDialog.getHeight()) / 2 + screenBounds.y
			);
			optionsDialog.setVisible(true);
		}
	};
	final List<PluginAction> extraActions = new ArrayList<>();
	
	void initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		setIconImage(applicationIcon);
		dataDirPath = pluginProxy.getDataDir();
		try {
			properties.load(Files.newInputStream(dataDirPath.resolve("config.properties")));
		} catch (IOException e) {
			// Configuration file not found: do nothing
		}
		try {
			String lookAndFeelClassName = properties.getProperty("lookAndFeel.className");
			if (lookAndFeelClassName != null) {
				UIManager.setLookAndFeel(lookAndFeelClassName);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		{
			String fontFamily = properties.getProperty("balloon.font.family", "Times New Roman");
			String fontSizeStr = properties.getProperty("balloon.font.size", "18");
			String fontStyleStr = properties.getProperty("balloon.font.style", "1");
			int fontSize = Integer.parseInt(fontSizeStr);
			int fontStyle = Integer.parseInt(fontStyleStr);
			balloonTextFont = new Font(fontFamily, fontStyle, fontSize);
			balloonDefaultTimeout = Integer.parseInt(properties.getProperty("balloon.defaultTimeout",
					"10000"));
			windowMode = Integer.parseInt(properties.getProperty("window_mode", "0"));
		}
		setTitle("DeskChan");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setUndecorated(true);
		setType(Type.UTILITY);
		setAlwaysOnTop(windowMode == WINDOW_MODE_TOP_MOST);
		setFocusableWindowState(false);
		setLayout(null);
		setBackground(new Color(0, 0, 0, 0));
		pack();
		characterWidget = new CharacterWidget(this);
		if (!properties.getProperty("skin.builtin", "true").equals("false")) {
			characterWidget.loadBuiltinSkin(MainWindow.properties.getProperty("skin.name", "variant1.png"));
		} else {
			characterWidget.loadSkin(Paths.get(MainWindow.properties.getProperty("skin.name")));
		}
		setDefaultLocation();
		setContentPane(characterWidget);
		optionsDialog = new OptionsDialog(this);
		addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent windowEvent) {
					pluginProxy.sendMessage("core:quit", null);
				}
			});
		pluginProxy.addMessageListener("gui:say", (sender, tag, data) -> {
			runOnEventThread(() -> {
				Map m = (Map) data;
				showBalloon(createBalloonTextComponent(m.get("text").toString()), m);
			});
		});
		pluginProxy.addMessageListener("gui:ask", (sender, tag, data) -> {
			runOnEventThread(() -> {
				Map m = (Map) data;
				InputWidget widget = new InputWidget(MainWindow.this);
				widget.setMessage((String) m.getOrDefault("text", null));
				showBalloon(widget, m);
			});
		});
		pluginProxy.addMessageListener("gui:register-extra-action", (sender, tag, data) -> {
			runOnEventThread(() -> {
				Map m = (Map) data;
				String msgTag = m.get("msgTag").toString();
				Object msgData = m.getOrDefault("msgData", null);
				PluginAction action = new PluginAction(m.get("name").toString(), sender) {
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
							pluginProxy.sendMessage(msgTag, msgData);
						}
				};
				extraActions.add(action);
				rebuildPopupMenu();
			});
		});
		pluginProxy.addMessageListener("gui:change-skin", (sender, tag, data) -> {
			runOnEventThread(() -> {
				if (data instanceof Path) {
					characterWidget.loadSkin((Path) data);
				} else {
					characterWidget.loadSkin(Paths.get(data.toString()));
				}
				setDefaultLocation();
			});
		});
		pluginProxy.addMessageListener("gui:set-image", (sender, tag, data) -> {
			runOnEventThread(() -> {
				currentCharacterImage = data.toString();
				characterWidget.setImage(currentCharacterImage);
			});
		});
		pluginProxy.addMessageListener("gui:add-options-tab", (sender, tag, data) -> {
			runOnEventThread(() -> {
				Map m = (Map) data;
				String name = (String) m.getOrDefault("name", tag);
				String msgTag = (String) m.getOrDefault("msgTag", null);
				List controls = (List) m.get("controls");
				optionsDialog.addTab(name, sender, msgTag, controls);
			});
		});
		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			runOnEventThread(() -> {
				extraActions.removeIf(action -> action.getPlugin().equals(data));
				optionsDialog.removeTabsByPlugin(data.toString());
			});
		});
		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
			put("srcTag", "DeskChan:say"); put("dstTag", "gui:say"); put("priority", 100);
		}});
		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
			put("srcTag", "DeskChan:ask"); put("dstTag", "gui:ask"); put("priority", 100);
		}});
		pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
			put("srcTag", "DeskChan:register-simple-action"); put("dstTag", "gui:register-extra-action");
			put("priority", 100);
		}});
		balloonTimer = new Timer(balloonDefaultTimeout, e -> {
			if (balloonWidget != null) {
				closeBalloon();
			}
		});
		balloonTimer.setRepeats(false);
		systemTray = SystemTray.get();
		if (systemTray != null) {
			systemTray.setTooltip(getTitle());
			systemTray.setImage(applicationIcon);
			systemTray.setStatus("Normal");
		} else {
			System.err.println("Failed to create system tray icon");
		}
		rebuildPopupMenu();
	}
	
	void setDefaultLocation() {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation(
				(int)screenBounds.getMaxX() - getWidth(),
				(int)screenBounds.getMaxY() - getHeight()
		);
		updateSizes();
	}
	
	void updateSizes() {
		Dimension characterSize = characterWidget.getPreferredSize();
		Rectangle characterBounds = new Rectangle(new Point(0, 0), characterSize);
		Dimension frameSize = new Dimension(characterSize);
		if (balloonWidget != null) {
			Dimension balloonSize = balloonWindow.getSize();
			Rectangle balloonBounds = new Rectangle(
					new Point(getX() - balloonSize.width, getY()),
					balloonSize
			);
			if (balloonBounds.getX() < 0) {
				balloonBounds.x = getX() + frameSize.width;
			}
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
	
	void showBalloon(JComponent component, Map<String, Object> params) {
		boolean prevBalloonIsFocused = (balloonWindow != null) && balloonWindow.isFocused();
		if (component != null) {
			int priority = (int) params.getOrDefault("priority", 1000);
			BalloonMessage message = new BalloonMessage(component, params, priority);
			balloonQueue.add(message);
			if (balloonQueue.peek() != message) {
				if (priority <= 0) {
					balloonQueue.remove(message);
				}
				return;
			}
			balloonQueue.removeIf((msg) -> ((msg != message) && (msg.priority <= 0)));
		}
		if (balloonWidget != null) {
			if (balloonWindow != null) {
				balloonWindow.dispose();
			} else {
				remove(balloonWidget);
			}
			balloonWindow = null;
			balloonWidget = null;
			if (component == null) {
				balloonQueue.poll();
			}
		}
		BalloonMessage message = balloonQueue.peek();
		if (message != null) {
			component = message.getComponent();
			params = message.getParams();
		} else {
			component = null;
		}
		int timeout = (int) params.getOrDefault("timeout", balloonDefaultTimeout);
		String characterImage = (String) params.getOrDefault("characterImage", null);
		if (characterImage != null) {
			characterWidget.setImage(characterImage);
		}
		if (component != null) {
			balloonWidget = new BalloonWidget(component, this);
			balloonWindow = new BalloonWindow(balloonWidget);
		}
		updateSizes();
		if (balloonWindow != null) {
			balloonWindow.setVisible(true);
			if (prevBalloonIsFocused) {
				balloonWindow.setFocusableWindowState(true);
				balloonWindow.requestFocus();
			} else {
				Timer timer = new Timer(100, (actionEvent) -> {
					balloonWindow.setFocusableWindowState(true);
				});
				timer.setRepeats(false);
				timer.start();
			}
		}
		if (balloonTimer.isRunning()) {
			balloonTimer.stop();
		}
		if (balloonWidget != null) {
			if (timeout > 0) {
				balloonTimer.setInitialDelay(timeout);
				balloonTimer.start();
			}
		} else {
			characterWidget.setImage(currentCharacterImage);
		}
		if (balloonWidget != null) {
			setAlwaysOnTop(true);
			requestFocus();
		} else {
			setAlwaysOnTop(windowMode == WINDOW_MODE_TOP_MOST);
		}
	}
	
	void closeBalloon() {
		showBalloon(null, new HashMap<>());
	}
	
	private JComponent createBalloonTextComponent(String text) {
		JLabel label = new JLabel("<html><center>" + text + "</center></html>");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(balloonTextFont);
		return label;
	}
	
	@Override
	public void dispose() {
		if (balloonTimer.isRunning()) {
			balloonTimer.stop();
		}
		if (optionsDialog != null) {
			optionsDialog.dispose();
		}
		if (balloonWindow != null) {
			balloonWindow.dispose();
		}
		super.dispose();
		try {
			properties.store(Files.newOutputStream(dataDirPath.resolve("config.properties")),
					"DeskChan GUI configuration");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void setPosition(Point pos) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle desktopBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		Rectangle frameBounds = new Rectangle(pos, getSize());
		if (Math.abs(frameBounds.getMaxX() - desktopBounds.getMaxX()) < 5) {
			frameBounds.x = (int) desktopBounds.getMaxX() - frameBounds.width;
		}
		if (Math.abs(frameBounds.getMaxY() - desktopBounds.getMaxY()) < 5) {
			frameBounds.y = (int) desktopBounds.getMaxY() - frameBounds.height;
		}
		if (Math.abs(desktopBounds.getMinX() - frameBounds.getMinX()) < 5) {
			frameBounds.x = Math.max(desktopBounds.x, frameBounds.x);
		}
		if (Math.abs(desktopBounds.getMinY() - frameBounds.getMinY()) < 5) {
			frameBounds.y = Math.max(desktopBounds.y, frameBounds.y);
		}
		if (frameBounds.getMaxX() > screenSize.width) {
			frameBounds.x = screenSize.width - frameBounds.width;
		}
		if (frameBounds.getMaxY() > screenSize.height) {
			frameBounds.y = screenSize.height - frameBounds.height;
		}
		frameBounds.y = Math.max(frameBounds.y, 0);
		frameBounds.y = Math.max(frameBounds.y, 0);
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
	
	Path getDataDirPath() {
		return dataDirPath;
	}
	
	int getWindowMode() {
		return windowMode;
	}
	
	void setWindowMode(int mode) {
		windowMode = mode;
		properties.setProperty("window_mode", String.valueOf(windowMode));
		setAlwaysOnTop((windowMode == WINDOW_MODE_TOP_MOST) || (balloonWidget != null));
	}
	
	private void rebuildPopupMenu() {
		popupMenu.removeAll();
		popupMenu.add(optionsAction);
		popupMenu.addSeparator();
		if (!extraActions.isEmpty()) {
			for (Action action : extraActions) {
				popupMenu.add(action);
			}
			popupMenu.addSeparator();
		}
		popupMenu.add(quitAction);
		if (systemTray != null) {
			systemTray.getMenu().clear();
			for (int i = 0; i < popupMenu.getComponentCount(); ++i) {
				Component component = popupMenu.getComponent(i);
				if (component instanceof JSeparator) {
					systemTray.getMenu().add(new Separator());
				} else if (component instanceof JMenuItem) {
					JMenuItem menuItem = (JMenuItem) component;
					if (menuItem.getAction() != null) {
						systemTray.getMenu().add(new MenuItem(menuItem.getText(), (event) -> {
							menuItem.getAction().actionPerformed(event);
						}));
					}
				}
			}
		}
	}
	
	JPopupMenu getPopupMenu() {
		return popupMenu;
	}
	
	static Image getApplicationIcon() {
		return applicationIcon;
	}
	
	void showThrowable(Throwable e) {
		showThrowable(this, e);
	}
	
	static void showThrowable(Component frame, Throwable e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		String stackTraceStr = stringWriter.toString();
		showLongMessage(frame, stackTraceStr, e.toString(), JOptionPane.ERROR_MESSAGE);
	}
	
	static void showLongMessage(Component frame, String message, String title, int type) {
		JOptionPane.showMessageDialog(frame, new LongMessagePanel(message), title, type);
	}
	
	static void runOnEventThread(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			SwingUtilities.invokeLater(runnable);
		}
	}
	
	static String getString(String key) {
		try {
			String s = strings.getString(key);
			return new String(s.getBytes("ISO-8859-1"), "UTF-8");
		} catch (Throwable e) {
			return key;
		}
	}
	
	private static abstract class PluginAction extends AbstractAction {
		
		private String plugin;
		
		PluginAction(String text, String plugin) {
			super(text);
			this.plugin = plugin;
		}
		
		String getPlugin() {
			return plugin;
		}
		
	}
	
	private static class LongMessagePanel extends JPanel {
		
		private final BorderLayout borderLayout = new BorderLayout();
		private final JScrollPane scrollPane = new JScrollPane();
		private final JTextArea textArea = new JTextArea();
		
		private LongMessagePanel(String message) {
			this.setLayout(borderLayout);
			textArea.setEnabled(true);
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setText(message);
			textArea.setSize(textArea.getPreferredSize());
			scrollPane.getViewport().add(textArea, null);
			scrollPane.setPreferredSize(new Dimension(400, 250));
			scrollPane.getViewport().setViewPosition(new Point(0, 0));
			add(scrollPane, BorderLayout.CENTER);
		}
		
	}
	
	private static class BalloonMessage implements Comparable<BalloonMessage> {
		
		private int priority;
		private JComponent component;
		private Map<String, Object> params;
		
		BalloonMessage(JComponent component, Map<String, Object> params, int priority) {
			this.component = component;
			this.priority = priority;
			this.params = params;
		}
		
		int getPriority() {
			return priority;
		}
		
		JComponent getComponent() {
			return component;
		}
		
		Map<String, Object> getParams() {
			return params;
		}
		
		@Override
		public int compareTo(BalloonMessage balloonMessage) {
			return balloonMessage.priority - priority;
		}
		
	}
	
	static {
		try {
			applicationIcon = Skin.loadImageByPath(Utils.getResourcePath("icon.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
