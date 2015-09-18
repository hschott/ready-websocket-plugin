package com.tsystems.readyapi.plugin.websocket;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueModel;

public class ReadOnlyValueModel<SrcType> extends AbstractValueModel {

    public interface Converter<SrcType> {
        Object convert(SrcType srcValue);
    }

    private ValueModel source;
    private Converter<SrcType> converter;

    public ReadOnlyValueModel(ValueModel source, Converter<SrcType> converter) {
        this.source = source;
        this.converter = converter;
        source.addValueChangeListener(new SubjectValueChangeHandler());
    }

    @Override
    public Object getValue() {
        return converter.convert((SrcType) source.getValue());
    }

    @Override
    public void setValue(Object newValue) {
    }

    private final class SubjectValueChangeHandler implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            fireValueChange(converter.convert((SrcType) evt.getOldValue()),
                    converter.convert((SrcType) evt.getNewValue()), true);
        }
    }
}
