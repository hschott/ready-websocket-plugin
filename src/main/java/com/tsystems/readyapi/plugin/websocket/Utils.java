package com.tsystems.readyapi.plugin.websocket;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.eviware.soapui.model.ModelItem;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.SpinnerAdapterFactory;
import com.jgoodies.binding.value.ValueModel;

class Utils {
    private final static Logger LOGGER = Logger.getLogger(PluginConfig.LOGGER_NAME);

    public static final String TREE_VIEW_IS_UNAVAILABLE = "The Tree View is available in Ready!API only.";

    public static boolean areStringsEqual(String s1, String s2) {
        return areStringsEqual(s1, s2, false);
    }

    public static boolean areStringsEqual(String s1, String s2, boolean caseInsensitive) {
        if (s1 == null)
            return s2 == null;
        if (caseInsensitive)
            return s1.equalsIgnoreCase(s2);
        else
            return s1.equals(s2);
    }

    public static boolean areStringsEqual(String s1, String s2, boolean caseInsensitive,
            boolean dontDistinctNullAndEmpty) {
        if (dontDistinctNullAndEmpty)
            if (s1 == null || s1.length() == 0)
                return s2 == null || s2.length() == 0;
        return areStringsEqual(s1, s2, caseInsensitive);
    }

    public static String bytesToHexString(byte[] buf) {
        final String decimals = "0123456789ABCDEF";
        if (buf == null)
            return null;
        char[] r = new char[buf.length * 2];
        for (int i = 0; i < buf.length; ++i) {
            r[i * 2] = decimals.charAt((buf[i] & 0xf0) >> 4);
            r[i * 2 + 1] = decimals.charAt(buf[i] & 0x0f);
        }
        return new String(r);
    }

    public static <B> JSpinner createBoundSpinEdit(PresentationModel<B> pm, String propertyName, int minPropValue,
            int maxPropValue, int step) {
        ValueModel valueModel = pm.getModel(propertyName);
        Number defValue = (Number) valueModel.getValue();
        SpinnerModel spinnerModel = new SpinnerNumberModel(defValue, minPropValue, maxPropValue, step);
        SpinnerAdapterFactory.connect(spinnerModel, valueModel, defValue);
        return new JSpinner(spinnerModel);

    }

    public static JComponent createJsonTreeEditor(boolean editable, ModelItem modelItem) {
        Class clazz;
        try {
            clazz = Class.forName("com.tsystems.readyapi.plugin.websocket.JsonTreeEditor");
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            return (JComponent) clazz.getConstructor(boolean.class, ModelItem.class).newInstance(editable, modelItem);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error(e);
            return null;
        }
    }

    public static RTextScrollPane createRTextScrollPane(RTextArea textArea) {
        Constructor[] ctors = RTextScrollPane.class.getConstructors();
        Constructor ctor = null;
        for (Constructor tmpCtor : ctors) {
            Class[] paramClasses = tmpCtor.getParameterTypes();
            if (paramClasses != null && paramClasses.length == 1 && paramClasses[0].isAssignableFrom(RTextArea.class)) {
                ctor = tmpCtor;
                break;
            }
        }
        try {
            return (RTextScrollPane) ctor.newInstance(textArea);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error(e);
            return null;
        }
    }

    public static JComponent createXmlTreeEditor(boolean editable, ModelItem modelItem) {
        Class clazz;
        try {
            clazz = Class.forName("com.tsystems.readyapi.plugin.websocket.XmlTreeEditor");
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            return (JComponent) clazz.getConstructor(boolean.class, ModelItem.class).newInstance(editable, modelItem);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error(e);
            return null;
        }
    }

    public static String getExceptionMessage(Throwable e) {
        Throwable cause = ExceptionUtils.getRootCause(e);
        return cause != null ? cause.toString() : e.toString();
    }

    public static byte[] hexStringToBytes(String str) {
        if (str == null)
            return null;
        if (str.length() % 2 != 0)
            throw new IllegalArgumentException();
        byte[] result = new byte[str.length() / 2];
        try {
            for (int i = 0; i < result.length; ++i)
                result[i] = (byte) Short.parseShort(str.substring(i * 2, i * 2 + 2), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
        return result;
    }

    public static boolean isJsonTreeEditorAvailable() {
        try {
            Class.forName("com.eviware.soapui.support.editor.views.xml.outline.support.JsonObjectTree");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isXmlTreeEditorAvailable() {
        try {
            Class.forName("com.eviware.soapui.support.editor.views.xml.outline.support.XmlObjectTree");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void releaseTreeEditor(JComponent treeEditor) {
        try {
            treeEditor.getClass().getMethod("release").invoke(treeEditor);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error(e);
        }
    }

    public static void showMemo(JTextArea memo, boolean visible) {
        memo.setVisible(visible);
        if (memo.getParent() instanceof JScrollPane)
            memo.getParent().setVisible(visible);
        else if (memo.getParent().getParent() instanceof JScrollPane)
            memo.getParent().getParent().setVisible(visible);

    }

}
