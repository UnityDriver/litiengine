package de.gurkenlabs.utiliti.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.gurkenlabs.litiengine.Game;
import de.gurkenlabs.litiengine.SpritesheetInfo;
import de.gurkenlabs.litiengine.environment.EmitterMapObjectLoader;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObject;
import de.gurkenlabs.litiengine.environment.tilemap.MapObjectProperty;
import de.gurkenlabs.litiengine.environment.tilemap.MapObjectType;
import de.gurkenlabs.litiengine.environment.tilemap.xml.Blueprint;
import de.gurkenlabs.litiengine.environment.tilemap.xml.MapObject;
import de.gurkenlabs.litiengine.environment.tilemap.xml.Tileset;
import de.gurkenlabs.litiengine.graphics.ImageFormat;
import de.gurkenlabs.litiengine.graphics.Spritesheet;
import de.gurkenlabs.litiengine.graphics.emitters.xml.EmitterData;
import de.gurkenlabs.litiengine.resources.Resources;
import de.gurkenlabs.litiengine.util.io.ImageSerializer;
import de.gurkenlabs.utiliti.EditorScreen;
import de.gurkenlabs.utiliti.Icons;
import de.gurkenlabs.utiliti.Program;
import de.gurkenlabs.utiliti.UndoManager;
import de.gurkenlabs.utiliti.swing.dialogs.SpritesheetImportPanel;
import de.gurkenlabs.utiliti.swing.panels.PropPanel;

@SuppressWarnings("serial")
public class AssetPanelItem extends JPanel {
  private static final Logger log = Logger.getLogger(AssetPanelItem.class.getName());
  private static final Border normalBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
  private static final Border focusBorder = BorderFactory.createDashedBorder(UIManager.getDefaults().getColor("Tree.selectionBorderColor"));

  private final JLabel iconLabel;
  private final JTextField textField;
  private final JPanel buttonPanel;
  private final JButton btnEdit;
  private final JButton btnDelete;
  private final JButton btnAdd;
  private final JButton btnExport;

  private final transient Object origin;

  public AssetPanelItem() {
    this(null);
  }

  public AssetPanelItem(Object origin) {
    setPreferredSize(new Dimension(64, 100));
    this.origin = origin;
    this.setBackground(AssetPanel.BACKGROUND);
    this.setBorder(normalBorder);

    this.getInputMap(JPanel.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteAsset");
    this.getActionMap().put("deleteAsset", new AbstractAction() {
      public void actionPerformed(ActionEvent ae) {
        deleteAsset();
      }
    });

    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        UIDefaults defaults = UIManager.getDefaults();
        setBackground(defaults.getColor("Tree.selectionBackground"));
        setForeground(defaults.getColor("Tree.selectionForeground"));
        textField.setForeground(defaults.getColor("Tree.selectionForeground"));
        setBorder(focusBorder);

        // TODO: We might need to provide multiple JPanels that contain the
        // buttons for
        // a certain usage and swap them out
        if (getOrigin() instanceof SpritesheetInfo || getOrigin() instanceof EmitterData) {
          btnEdit.setVisible(true);
          btnAdd.setVisible(true);
          btnDelete.setVisible(true);
          btnExport.setVisible(true);
        } else if (getOrigin() instanceof Tileset) {
          btnEdit.setVisible(false);
          btnAdd.setVisible(false);
          btnDelete.setVisible(false);
          btnExport.setVisible(true);
        } else if (getOrigin() instanceof MapObject) {
          btnEdit.setVisible(false);
          btnAdd.setVisible(true);
          btnDelete.setVisible(true);
          btnExport.setVisible(true);
        }
      }

      @Override
      public void focusLost(FocusEvent e) {
        UIDefaults defaults = UIManager.getDefaults();
        setBackground(AssetPanel.BACKGROUND);
        setForeground(defaults.getColor("Tree.foreground"));
        textField.setForeground(Color.WHITE);
        setBorder(normalBorder);

        btnEdit.setVisible(false);
        btnAdd.setVisible(false);
        btnDelete.setVisible(false);
        btnExport.setVisible(false);
      }
    });

    setLayout(new BorderLayout(0, 0));
    this.setFocusable(true);
    this.setRequestFocusEnabled(true);
    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        requestFocus();
      }
    });

    this.iconLabel = new JLabel("");
    iconLabel.setPreferredSize(new Dimension(64, 64));
    this.iconLabel.setSize(64, 64);
    this.iconLabel.setMinimumSize(new Dimension(64, 64));
    this.iconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        requestFocus();
      }
    });
    add(this.iconLabel, BorderLayout.NORTH);

    this.iconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && addEntity()) {
          e.consume();
        }
      }
    });

    this.textField = new JTextField();
    add(this.textField, BorderLayout.SOUTH);
    this.textField.setColumns(10);
    this.textField.setHorizontalAlignment(JTextField.CENTER);
    this.textField.setForeground(Color.WHITE);
    this.textField.setBackground(null);
    this.textField.setBorder(null);
    this.textField.setEditable(false);

    this.setMinimumSize(new Dimension(this.iconLabel.getWidth(), this.iconLabel.getHeight() + this.textField.getHeight()));

    GridLayout buttonGridLayout = new GridLayout(0, 4, 0, 0);
    buttonPanel = new JPanel(buttonGridLayout);
    buttonPanel.setPreferredSize(new Dimension(64, 20));
    buttonPanel.setMinimumSize(new Dimension(64, 20));
    buttonPanel.setOpaque(false);
    add(buttonPanel, BorderLayout.EAST);

    btnAdd = new JButton("");
    btnAdd.setToolTipText("Add Entity");
    btnAdd.addActionListener(e -> this.addEntity());
    btnAdd.setMaximumSize(new Dimension(16, 16));
    btnAdd.setMinimumSize(new Dimension(16, 16));
    btnAdd.setPreferredSize(new Dimension(16, 16));
    btnAdd.setOpaque(false);
    btnAdd.setIcon(Icons.ADD);
    btnAdd.setVisible(false);
    btnAdd.setEnabled(canAdd());

    btnEdit = new JButton("");
    btnEdit.setToolTipText("Edit Asset");
    btnEdit.addActionListener(e -> {
      if (!(this.getOrigin() instanceof SpritesheetInfo)) {
        return;
      }
      SpritesheetImportPanel spritePanel = new SpritesheetImportPanel((SpritesheetInfo) this.getOrigin());
      int option = JOptionPane.showConfirmDialog(Game.window().getRenderComponent(), spritePanel, Resources.strings().get("menu_assets_editSprite"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (option != JOptionPane.OK_OPTION) {
        return;
      }

      final Collection<SpritesheetInfo> sprites = spritePanel.getSpriteSheets();
      for (SpritesheetInfo spriteFile : sprites) {
        int index = -1;
        Optional<SpritesheetInfo> old = EditorScreen.instance().getGameFile().getSpriteSheets().stream().filter((x -> x.getName().equals(spriteFile.getName()))).findFirst();
        if (old.isPresent()) {
          index = EditorScreen.instance().getGameFile().getSpriteSheets().indexOf(old.get());
          EditorScreen.instance().getGameFile().getSpriteSheets().remove(index);
        }

        EditorScreen.instance().getGameFile().getSpriteSheets().removeIf(x -> x.getName().equals(spriteFile.getName()));
        if (index != -1) {
          EditorScreen.instance().getGameFile().getSpriteSheets().add(index, spriteFile);
        } else {
          EditorScreen.instance().getGameFile().getSpriteSheets().add(spriteFile);
        }
      }

      // TODO: in case the asset has been renamed: update all props that uses
      // the
      // asset to use the new name (assets are treated as reference by name)
      EditorScreen.instance().loadSpriteSheets(EditorScreen.instance().getGameFile().getSpriteSheets(), true);
    });
    btnEdit.setMaximumSize(new Dimension(16, 16));
    btnEdit.setMinimumSize(new Dimension(16, 16));
    btnEdit.setPreferredSize(new Dimension(16, 16));
    btnEdit.setOpaque(false);
    btnEdit.setIcon(Icons.PENCIL);
    btnEdit.setVisible(false);

    btnDelete = new JButton("");
    btnDelete.setToolTipText("Delete Asset");
    btnDelete.addActionListener(e -> this.deleteAsset());
    btnDelete.setMaximumSize(new Dimension(16, 16));
    btnDelete.setMinimumSize(new Dimension(16, 16));
    btnDelete.setPreferredSize(new Dimension(16, 16));
    btnDelete.setOpaque(false);
    btnDelete.setIcon(Icons.DELETE);
    btnDelete.setVisible(false);

    btnExport = new JButton("");
    btnExport.setToolTipText("Export Asset");
    btnExport.addActionListener(e -> this.export());
    btnExport.setMaximumSize(new Dimension(16, 16));
    btnExport.setMinimumSize(new Dimension(16, 16));
    btnExport.setPreferredSize(new Dimension(16, 16));
    btnExport.setOpaque(false);
    btnExport.setIcon(Icons.EXPORT);
    btnExport.setVisible(false);

    buttonPanel.add(btnEdit);
    buttonPanel.add(btnAdd);
    buttonPanel.add(btnDelete);
    buttonPanel.add(btnExport);
  }

  public AssetPanelItem(Icon icon, String text, Object origin) {
    this(origin);
    this.iconLabel.setHorizontalAlignment(JLabel.CENTER);
    this.iconLabel.setIcon(icon);
    this.textField.setText(text);
  }

  public Object getOrigin() {
    return this.origin;
  }

  private void deleteAsset() {
    if (getOrigin() instanceof SpritesheetInfo) {
      SpritesheetInfo info = (SpritesheetInfo) getOrigin();
      int n = JOptionPane.showConfirmDialog(Game.window().getRenderComponent(), "Do you really want to delete the spritesheet [" + info.getName() + "]?\n Entities that use the sprite won't be rendered anymore!", "Delete Spritesheet?", JOptionPane.YES_NO_OPTION);

      if (n == JOptionPane.OK_OPTION) {
        EditorScreen.instance().getGameFile().getSpriteSheets().remove(getOrigin());
        Resources.images().clear();
        Resources.spritesheets().remove(info.getName());
        EditorScreen.instance().getMapComponent().reloadEnvironment();

        Program.getAssetTree().forceUpdate();
      }
    } else if (getOrigin() instanceof EmitterData) {
      EmitterData emitter = (EmitterData) getOrigin();
      int n = JOptionPane.showConfirmDialog(Game.window().getRenderComponent(), "Do you really want to delete the emitter [" + emitter.getName() + "]", "Delete Emitter?", JOptionPane.YES_NO_OPTION);

      if (n == JOptionPane.OK_OPTION) {
        EditorScreen.instance().getGameFile().getEmitters().remove(getOrigin());
        EditorScreen.instance().getMapComponent().reloadEnvironment();

        Program.getAssetTree().forceUpdate();
      }
    } else if (getOrigin() instanceof Blueprint) {
      Blueprint blueprint = (Blueprint) getOrigin();
      int n = JOptionPane.showConfirmDialog(Game.window().getRenderComponent(), "Do you really want to delete the blueprint [" + blueprint.getName() + "]?", "Delete Blueprint?", JOptionPane.YES_NO_OPTION);
      if (n == JOptionPane.OK_OPTION) {
        EditorScreen.instance().getGameFile().getBluePrints().remove(getOrigin());
        Program.getAssetTree().forceUpdate();
      }
    }
  }

  private boolean addEntity() {
    if(Game.world().environment() == null || Game.world().camera() == null) {
      return false;
    }
    
    // TODO: experimental code... this needs to be refactored with issue #66
    if (this.getOrigin() instanceof SpritesheetInfo) {
      SpritesheetInfo info = (SpritesheetInfo) this.getOrigin();
      String propName = PropPanel.getNameBySpriteName(info.getName());
      if (propName == null) {
        return false;
      }

      MapObject mo = new MapObject();
      mo.setType(MapObjectType.PROP.name());
      mo.setX((int) Game.world().camera().getFocus().getX() - info.getWidth() / 2);
      mo.setY((int) Game.world().camera().getFocus().getY() - info.getHeight() / 2);
      mo.setWidth((int) info.getWidth());
      mo.setHeight((int) info.getHeight());
      mo.setId(Game.world().environment().getNextMapId());
      mo.setName("");
      mo.setValue(MapObjectProperty.COLLISIONBOX_WIDTH, info.getWidth() * 0.4);
      mo.setValue(MapObjectProperty.COLLISIONBOX_HEIGHT, info.getHeight() * 0.4);
      mo.setValue(MapObjectProperty.COLLISION, true);
      mo.setValue(MapObjectProperty.COMBAT_INDESTRUCTIBLE, false);
      mo.setValue(MapObjectProperty.PROP_ADDSHADOW, true);
      mo.setValue(MapObjectProperty.SPRITESHEETNAME, propName);

      EditorScreen.instance().getMapComponent().add(mo);
      return true;
    } else if (this.getOrigin() instanceof EmitterData) {
      MapObject newEmitter = (MapObject) EmitterMapObjectLoader.createMapObject((EmitterData) this.getOrigin());
      newEmitter.setX((int) (Game.world().camera().getFocus().getX() - newEmitter.getWidth()));
      newEmitter.setY((int) (Game.world().camera().getFocus().getY() - newEmitter.getHeight()));
      newEmitter.setId(Game.world().environment().getNextMapId());
      EditorScreen.instance().getMapComponent().add(newEmitter);
    } else if (this.getOrigin() instanceof Blueprint) {
      Blueprint blueprint = (Blueprint) this.getOrigin();

      UndoManager.instance().beginOperation();
      try {
        List<IMapObject> newObjects = blueprint.build((int) Game.world().camera().getFocus().getX() - blueprint.getWidth() / 2, (int) Game.world().camera().getFocus().getY() - blueprint.getHeight() / 2);
        for (IMapObject newMapObject : newObjects) {
          EditorScreen.instance().getMapComponent().add(newMapObject);
        }

        // separately select the added objects because this cannot be done in
        // the
        // previous loop because it gets overwritten every time a map object
        // gets added
        for (IMapObject newMapObject : newObjects) {
          EditorScreen.instance().getMapComponent().setSelection(newMapObject, false);
        }
      } finally {
        UndoManager.instance().endOperation();
      }
    }

    return false;
  }

  private void export() {
    if (this.getOrigin() instanceof Tileset) {
      this.exportTileset();
      return;
    } else if (this.getOrigin() instanceof SpritesheetInfo) {
      this.exportSpritesheet();
      return;
    } else if (this.getOrigin() instanceof EmitterData) {
      this.exportEmitter();
      return;
    } else if (this.getOrigin() instanceof MapObject) {
      this.exportBlueprint();
      return;
    }
  }

  private void exportSpritesheet() {
    if (this.getOrigin() instanceof SpritesheetInfo) {
      SpritesheetInfo spriteSheetInfo = (SpritesheetInfo) this.getOrigin();

      Optional<Spritesheet> opt = Resources.spritesheets().tryGet(spriteSheetInfo.getName());
      if (!opt.isPresent()) {
        return;
      }

      Spritesheet sprite = opt.get();

      ImageFormat format = sprite.getImageFormat() != ImageFormat.UNDEFINED ? sprite.getImageFormat() : ImageFormat.PNG;

      Object[] options = { ".xml", format.toExtension() };
      int answer = JOptionPane.showOptionDialog(Game.window().getRenderComponent(), "Select an export format:", "Export Spritesheet", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

      try {
        JFileChooser chooser;
        String source = EditorScreen.instance().getProjectPath();
        chooser = new JFileChooser(source != null ? source : new File(".").getCanonicalPath());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Export Spritesheet");
        if (answer == 0) {
          XmlExportDialog.export(spriteSheetInfo, "Spritesheet", spriteSheetInfo.getName());
        } else if (answer == 1) {
          FileFilter filter = new FileNameExtensionFilter(format.toString() + " - Image", format.toString());
          chooser.setFileFilter(filter);
          chooser.addChoosableFileFilter(filter);
          chooser.setSelectedFile(new File(spriteSheetInfo.getName() + format.toExtension()));

          int result = chooser.showSaveDialog(Game.window().getRenderComponent());
          if (result == JFileChooser.APPROVE_OPTION) {
            ImageSerializer.saveImage(chooser.getSelectedFile().toString(), sprite.getImage(), format);
            log.log(Level.INFO, "exported spritesheet {0} to {1}", new Object[] { spriteSheetInfo.getName(), chooser.getSelectedFile() });
          }
        }
      } catch (IOException e) {
        log.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  private void exportTileset() {
    if (!(this.getOrigin() instanceof Tileset)) {
      return;
    }

    Tileset tileset = (Tileset) this.getOrigin();
    XmlExportDialog.export(tileset, "Tileset", tileset.getName(), Tileset.FILE_EXTENSION);
  }

  private void exportEmitter() {
    if (!(this.getOrigin() instanceof EmitterData)) {
      return;
    }

    EmitterData emitter = (EmitterData) this.getOrigin();
    XmlExportDialog.export(emitter, "Emitter", emitter.getName());
  }

  private void exportBlueprint() {
    if (!(this.getOrigin() instanceof Blueprint)) {
      return;
    }

    Blueprint mapObject = (Blueprint) this.getOrigin();
    XmlExportDialog.export(mapObject, "Blueprint", mapObject.getName());
  }

  private boolean canAdd() {
    if (this.getOrigin() != null && this.getOrigin() instanceof SpritesheetInfo) {
      SpritesheetInfo info = (SpritesheetInfo) this.getOrigin();
      String propName = PropPanel.getNameBySpriteName(info.getName());
      return propName != null && !propName.isEmpty();
    }

    return this.getOrigin() instanceof MapObject || this.getOrigin() instanceof EmitterData;
  }
}
