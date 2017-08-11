package info.deskchan.gui_javafx;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;

class SkinManagerDialog extends Dialog<Void> {
	
	private ListView<Skin> skinsList = new ListView<>();
	
	SkinManagerDialog(Window parent) {
		setTitle(Main.getString("skin_manager"));
		initOwner(parent);
		initModality(Modality.WINDOW_MODAL);
		skinsList.setPrefSize(400, 300);
		skinsList.setCellFactory(param -> new ListCell<Skin>() {
			@Override
			protected void updateItem(Skin item, boolean empty) {
				super.updateItem(item, empty);
				if (!empty) {
					setText(item.getName());
					String description = item.getDescription();
					if (description != null && !description.isEmpty()) {
						setTooltip(new Tooltip(description));
					}
				} else {
					setText(null);
					setTooltip(null);
				}
			}
		});
		//TreeTableView<SkinOrPack> skinsTree = new TreeTableView<>();
		for (String skinName : Skin.getSkinList()) {
			skinsList.getItems().add(Skin.load(skinName));
		}
		getDialogPane().setContent(skinsList);
		getDialogPane().getButtonTypes().add(ButtonType.APPLY);
		getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, (event) -> {
			event.consume();
			Skin skin = skinsList.getSelectionModel().getSelectedItem();
			Character character = App.getInstance().getCharacter();
			character.setSkin(skin);
			character.setDefaultPosition();
		});
	}
	
	/* private static class SkinOrPack {
		
		final Skin skin;
		final String title;
		
		SkinOrPack(Skin skin) {
			this.skin = skin;
			String[] parts = skin.toString().split("/");
			title = parts[parts.length - 1];
		}
		
		SkinOrPack(String pack) {
			skin = null;
			String[] parts = pack.split("/");
			title = parts[parts.length - 1];
		}
		
		@Override
		public String toString() {
			return title;
		}
		
	} */
	
}
