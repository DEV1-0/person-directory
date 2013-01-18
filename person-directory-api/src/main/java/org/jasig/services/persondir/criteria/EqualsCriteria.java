package org.jasig.services.persondir.criteria;

import java.util.List;
import java.util.Map;

public class EqualsCriteria extends CompareCriteria<Object> {

    public EqualsCriteria(String attribute, Object value) {
        super(attribute, value);
    }

    @Override
    public boolean equals(Map<String, List<Object>> attributes) {
        final List<Object> values = attributes.get(this.getAttribute());
        
        for (final Object value : values) {
            if (value.equals(this.getValue())) {
                return true;
            }
        }
        
        return false;
    }
}
