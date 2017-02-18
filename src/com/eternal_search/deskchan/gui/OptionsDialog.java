package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;

class OptionsDialog extends JFrame {
	
	private final MainWindow mainWindow;
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private JList skinList;
	private final Action selectSkinAction = new AbstractAction("Select") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			Object selectValue = skinList.getSelectedValue();
			if (selectValue != null) {
				SkinInfo skinInfo = (SkinInfo) selectValue;
				mainWindow.getCharacterWidget().loadImage(skinInfo.path);
				mainWindow.setDefaultLocation();
			}
		}
	};
	private Integer balloonDelay;
	private Integer sayDelay;
	
	OptionsDialog(MainWindow mainWindow) {
		super("deskchan Options");
		this.mainWindow = mainWindow;
		setMinimumSize(new Dimension(300, 200));
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setContentPane(tabbedPane);
		JPanel appearanceTab = new JPanel(new BorderLayout());
		skinList = new JList(loadSkinList().toArray());
		skinList.setLayoutOrientation(JList.VERTICAL);
		skinList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectSkinAction.actionPerformed(null);
				}
			}
		});
		JScrollPane skinListScrollPane = new JScrollPane(skinList);
		appearanceTab.add(skinListScrollPane);
		JButton button = new JButton(selectSkinAction);
		appearanceTab.add(button, BorderLayout.PAGE_END);
		tabbedPane.addTab("Appearance", appearanceTab);
		JPanel pluginsTab = new JPanel(new BorderLayout());
		DefaultListModel pluginsListModel = new DefaultListModel();
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			pluginsListModel.addElement(data.toString());
		});
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			for (int i = 0; i < pluginsListModel.size(); ++i) {
				if (pluginsListModel.elementAt(i).equals(data)) {
					pluginsListModel.remove(i);
					break;
				}
			}
		});
		JList pluginsList = new JList(pluginsListModel);
		JScrollPane pluginsListScrollPane = new JScrollPane(pluginsList);
		pluginsTab.add(pluginsListScrollPane);
		tabbedPane.addTab("Plugins", pluginsTab);
		JPanel debugTab = new JPanel(new BorderLayout());
		//
		tabbedPane.addTab("Debug", debugTab);

		this.balloonDelay = OptionsDialog.this.mainWindow.balloonDelay;
		JPanel otherTab = new JPanel(new BorderLayout());
		JSlider balloonDelaySlider = new JSlider(JSlider.HORIZONTAL, 1, 19, OptionsDialog.this.balloonDelay / 1000);
		balloonDelaySlider.setMajorTickSpacing(10);
		balloonDelaySlider.setMinorTickSpacing(1);
		balloonDelaySlider.setPaintTicks(true);
		balloonDelaySlider.setPaintLabels(true);
		balloonDelaySlider.setSnapToTicks(true);
		balloonDelaySlider.addChangeListener(e -> {
			JSlider source = (JSlider)e.getSource();
			OptionsDialog.this.balloonDelay = source.getValue() * 1000;
		});

		this.sayDelay = OptionsDialog.this.mainWindow.sayDelay;
		JSlider sayDelaySlider = new JSlider(JSlider.HORIZONTAL, 10, 720, OptionsDialog.this.sayDelay / 60000);
		sayDelaySlider.setMajorTickSpacing(180);
		sayDelaySlider.setMinorTickSpacing(10);
		sayDelaySlider.setPaintTicks(true);
		sayDelaySlider.setPaintLabels(true);
		sayDelaySlider.setSnapToTicks(true);
		sayDelaySlider.addChangeListener(e -> {
			JSlider source = (JSlider)e.getSource();
			OptionsDialog.this.sayDelay = source.getValue() * 60000;
		});

		JButton otherOkButton = new JButton(new AbstractAction("OK") {
			public void actionPerformed(ActionEvent actionEvent) {
				mainWindow.balloonDelay = balloonDelay;
				mainWindow.sayDelay = sayDelay;
				mainWindow.updateSayTimer();
			}
		});
		otherTab.add(balloonDelaySlider, BorderLayout.PAGE_START);
		otherTab.add(sayDelaySlider, BorderLayout.CENTER);
		otherTab.add(otherOkButton, BorderLayout.PAGE_END);
		this.tabbedPane.addTab("Timings", otherTab);

		pack();
	}
	
	private ArrayList<SkinInfo> loadSkinList() {
		ArrayList<SkinInfo> list = new ArrayList<>();
		Path directoryPath = Utils.getResourcePath("characters");
		if (directoryPath != null) {
			try {
				DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath);
				for (Path skinPath : directoryStream) {
					if (!Files.isDirectory(skinPath)) {
						list.add(new SkinInfo(skinPath));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Collections.sort(list);
		return list;
	}
	
	class SkinInfo implements Comparable<SkinInfo> {
		
		String name;
		Path path;
		
		SkinInfo(String name, Path path) {
			this.name = name;
			this.path = path;
		}
		
		SkinInfo(Path path) {
			this(path.getFileName().toString(), path);
		}
		
		@Override
		public int compareTo(SkinInfo o) {
			return name.compareTo(o.name);
		}
		
		@Override
		public String toString() {
			return name;
		}
		
	}
	
}
