package com.tsystems.readyapi.plugin.websocket;

import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;

@AForm(name = "Select Test Step",
        description = "Select a test step which should be used as a source of Websocket connection parameters.")
public interface SelectSourceTestStepForm {

    @AField(name = "Test case", description = "Test case which the source test step belongs to.",
            type = AField.AFieldType.COMBOBOX)
    public final static String TEST_CASE = "Test case";

    @AField(name = "Test step", description = "Test step which will be used as a source of connection parameters.",
            type = AField.AFieldType.COMBOBOX)
    public final static String TEST_STEP = "Test step";

    @AField(name = "Test step's connection",
            description = "Websocket server connection parameters of the selected test step (for information only)",
            type = AField.AFieldType.INFORMATION)
    public final static String STEP_INFO = "Test step's connection";

    @AField(name = "Use property expansions", description = "Insert property expansion instead of property values",
            type = AField.AFieldType.BOOLEAN)
    public final static String USE_PROPERTY_EXPANSIONS = "Use property expansions";
}
