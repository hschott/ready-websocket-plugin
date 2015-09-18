package com.tsystems.readyapi.plugin.websocket;

import java.awt.Dimension;

import javax.swing.JScrollPane;

import com.eviware.soapui.support.components.SimpleBindingForm;
import com.jgoodies.binding.PresentationModel;

public class DropConnectionTestStepPanel extends ConnectedTestStepPanel<DropConnectionTestStep> {

    public DropConnectionTestStepPanel(DropConnectionTestStep modelItem) {
        super(modelItem);
        buildUI();
    }

    protected void buildUI() {
        PresentationModel<DropConnectionTestStep> pm = new PresentationModel<DropConnectionTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        buildConnectionSection(form, pm);
        form.appendSeparator();
        form.appendHeading("Settings");
        buildRadioButtonsFromEnum(form, pm, "Drop method", DropConnectionTestStep.DROP_METHOD_BEAN_PROP_NAME,
                DropConnectionTestStep.DropMethod.class);

        add(new JScrollPane(form.getPanel()));
        setPreferredSize(new Dimension(500, 300));
    }

}
