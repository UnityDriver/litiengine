package de.gurkenlabs.utiliti.swing.panels;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.LayoutStyle.ComponentPlacement;

import de.gurkenlabs.litiengine.entities.StaticShadow;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObject;
import de.gurkenlabs.litiengine.environment.tilemap.MapObjectProperty;
import de.gurkenlabs.litiengine.graphics.StaticShadowType;
import de.gurkenlabs.litiengine.resources.Resources;

@SuppressWarnings("serial")
public class StaticShadowPanel extends PropertyPanel {
  private JComboBox<StaticShadowType> comboBoxShadowType;
  private JSpinner spinnerOffset;

  public StaticShadowPanel() {
    super("panel_staticShadow");

    JLabel lblShadowType = new JLabel(Resources.strings().get("panel_shadowType"));

    this.comboBoxShadowType = new JComboBox<>();
    this.comboBoxShadowType.setModel(new DefaultComboBoxModel<StaticShadowType>(StaticShadowType.values()));

    JLabel lblOffset = new JLabel("offset");

    this.spinnerOffset = new JSpinner();

    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(groupLayout.createSequentialGroup().addContainerGap().addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(lblShadowType).addComponent(lblOffset)).addGap(7)
            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout.createSequentialGroup().addComponent(comboBoxShadowType, 0, 357, Short.MAX_VALUE).addGap(4))
                .addGroup(groupLayout.createSequentialGroup().addComponent(spinnerOffset, GroupLayout.PREFERRED_SIZE, 95, GroupLayout.PREFERRED_SIZE).addContainerGap(266, Short.MAX_VALUE)))));
    groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(groupLayout.createSequentialGroup().addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(comboBoxShadowType, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE).addComponent(lblShadowType, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED).addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblOffset).addComponent(spinnerOffset, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addContainerGap(240, Short.MAX_VALUE)));
    setLayout(groupLayout);

    this.setupChangedListeners();
  }

  private void setupChangedListeners() {
    this.comboBoxShadowType.addActionListener(new MapObjectPropertyActionListener(m -> m.setValue(MapObjectProperty.SHADOW_TYPE, (StaticShadowType) this.comboBoxShadowType.getSelectedItem())));
    this.spinnerOffset.addChangeListener(new MapObjectPropertyChangeListener(m -> m.setValue(MapObjectProperty.SHADOW_OFFSET, (int) this.spinnerOffset.getValue())));
  }

  @Override
  protected void clearControls() {
    this.comboBoxShadowType.setSelectedItem(StaticShadowType.NOOFFSET);
    this.spinnerOffset.setValue(StaticShadow.DEFAULT_OFFSET);
  }

  @Override
  protected void setControlValues(IMapObject mapObject) {
    String shadowType = mapObject.getStringValue(MapObjectProperty.SHADOW_TYPE);
    if (shadowType != null && !shadowType.isEmpty()) {
      this.comboBoxShadowType.setSelectedItem(StaticShadowType.valueOf(shadowType));
    }

    this.spinnerOffset.setValue(mapObject.getIntValue(MapObjectProperty.SHADOW_OFFSET, StaticShadow.DEFAULT_OFFSET));
  }
}
