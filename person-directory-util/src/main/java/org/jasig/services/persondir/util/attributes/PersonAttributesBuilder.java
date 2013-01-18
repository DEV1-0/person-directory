package org.jasig.services.persondir.util.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jasig.services.persondir.PersonAttributes;
import org.springframework.util.LinkedCaseInsensitiveMap;

public class PersonAttributesBuilder {
    final LinkedCaseInsensitiveMap<List<Object>> attributes = new LinkedCaseInsensitiveMap<List<Object>>();
    
    public PersonAttributes build() {
        return new ImmutablePersonAttributesImpl(attributes);
    }
    
    public void add(String name, Object value) {
        List<Object> values = this.attributes.get(name);
        if (values == null) {
            values = new ArrayList<Object>(1);
            this.attributes.put(name, values);
        }
        
        if (value instanceof Collection<?>) {
            values.addAll((Collection<?>)value);
        }
        else {
            values.add(value);
        }
    }
}
