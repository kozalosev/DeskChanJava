package info.deskchan.gui_javafx;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;

import java.util.Map;
import java.util.PriorityQueue;

class Character extends MovablePane {
	
	private static final String DEFAULT_SKIN_NAME = "illia";
	
	enum LayerMode {
		ALWAYS_NORMAL,
		TOP_IF_MESSAGE,
		ALWAYS_TOP
	}
	
	private static final int DEFAULT_MESSAGE_PRIORITY = 1000;
	
	private ImageView imageView = new ImageView();
	private Skin skin = null;
	private String imageName = "normal";
	private String idleImageName = "normal";
	private PriorityQueue<MessageInfo> messageQueue = new PriorityQueue<>();
	private Balloon balloon = null;
	private String layerName = "top";
	private LayerMode layerMode = LayerMode.ALWAYS_TOP;
	
	Character(String id, Skin skin) {
		getChildren().add(imageView);
		setSkin(skin);
		setPositionStorageID("character." + id);
		addEventFilter(MouseEvent.MOUSE_PRESSED, this::startDrag);
	}
	
	Skin getSkin() {
		return skin;
	}
	
	void setSkin(Skin skin) {
		if (skin == null) {
			skin = Skin.load(DEFAULT_SKIN_NAME);
		}
		this.skin = skin;
		setImageName(imageName);
	}
	
	String getImageName() {
		return imageName;
	}
	
	void setImageName(String name) {
		imageName = ((name != null) && (name.length() > 0)) ? name : "normal";
		updateImage();
	}
	
	private Image getImage() {
		return (skin != null) ? skin.getImage(imageName) : null;
	}
	
	@Override
	void setDefaultPosition() {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		setPosition(new Point2D(screenBounds.getMaxX() - getWidth(),
				screenBounds.getMaxY() - getHeight()));
	}
	
	private void updateImage() {
		Image image = getImage();
		imageView.setImage(image);
		if (image != null) {
			resize(image.getWidth(), image.getHeight());
		}
	}
	
	void setIdleImageName(String name) {
		idleImageName = name;
		setImageName(name);
	}
	
	void say(Map<String, Object> data) {
		MessageInfo messageInfo = null;
		if (data != null) {
			messageInfo = new MessageInfo(data);
			if ((messageInfo.priority <= 0) && (messageQueue.size() > 0)) {
				return;
			}
			messageQueue.add(messageInfo);
			if (messageQueue.peek() != messageInfo) {
				return;
			}
		} else {
			messageQueue.poll();
		}
		if (balloon != null) {
			balloon.close();
			balloon = null;
		}
		messageInfo = messageQueue.peek();
		if (messageInfo == null) {
			setImageName(idleImageName);
		} else {
			setImageName(messageInfo.characterImage);
			balloon = new Balloon(this, messageInfo.text);
			balloon.setTimeout(messageInfo.timeout);
		}
		setLayerMode(layerMode);
	}
	
	LayerMode getLayerMode() {
		return layerMode;
	}
	
	void setLayerMode(LayerMode mode) {
		layerMode = mode;
		String newLayerName;
		if (mode.equals(LayerMode.ALWAYS_TOP)) {
			newLayerName = "top";
		} else if (mode.equals(LayerMode.TOP_IF_MESSAGE)) {
			newLayerName = (balloon != null) ? "top" : "normal";
		} else {
			newLayerName = "normal";
		}
		if (!layerName.equals(newLayerName)) {
			OverlayStage.getInstance(layerName).getRoot().getChildren().remove(this);
			layerName = newLayerName;
		}
		if (getParent() == null) {
			OverlayStage.getInstance(layerName).getRoot().getChildren().add(this);
		}
		if (balloon != null) {
			balloon.show(layerName);
		}
	}
	
	private static class MessageInfo implements Comparable<MessageInfo> {
		
		private final String text;
		private final String characterImage;
		private final int priority;
		private final int timeout;
		
		MessageInfo(Map<String, Object> data) {
			text = (String) data.getOrDefault("text", "");
			String characterImage = (String) data.getOrDefault("characterImage", null);
			if (characterImage != null) {
				characterImage = characterImage.toLowerCase();
			} else {
				characterImage = "normal";
			}
			this.characterImage = characterImage;
			priority = (Integer) data.getOrDefault("priority", DEFAULT_MESSAGE_PRIORITY);
			//timeout = (Integer) data.getOrDefault("timeout",
			//		Integer.parseInt(Main.getProperty("balloon.default_timeout", "15000")));
			timeout = (Integer) data.getOrDefault("timeout", Math.max(6000,
					text.length() * Integer.parseInt(Main.getProperty("balloon.default_timeout", "300"))));
		}
		
		@Override
		public int compareTo(MessageInfo messageInfo) {
			return -(priority - messageInfo.priority);
		}
		
	}
	
}
