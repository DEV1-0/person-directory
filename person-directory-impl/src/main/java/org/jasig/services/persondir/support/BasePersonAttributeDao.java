/**
 * Copyright 2007 The JA-SIG Collaborative.  All rights reserved.
 * See license distributed with this file and
 * available online at http://www.uportal.org/license.html
 */
package org.jasig.services.persondir.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.services.persondir.IPerson;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.springframework.dao.support.DataAccessUtils;

/**
 * Base {@link IPersonAttributeDao} that provides implementations of the deprecated methods. This class will be removed
 * in 1.6
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public abstract class BasePersonAttributeDao implements IPersonAttributeDao {
    protected final Log logger = LogFactory.getLog(getClass());
    
    /* (non-Javadoc)
     * @see org.jasig.services.persondir.IPersonAttributeDao#getMultivaluedUserAttributes(java.util.Map)
     */
    @SuppressWarnings("deprecation")
    public final Map<String, List<Object>> getMultivaluedUserAttributes(Map<String, List<Object>> seed) {
        final Set<IPerson> people = this.getPeopleWithMultivaluedAttributes(seed);

        //Get the first IPerson to return data for
        final IPerson person = (IPerson)DataAccessUtils.singleResult(people);
        
        //If null or no results return null
        if (person == null) {
            return null;
        }
        
        //Make a mutable copy of the peron's attributes
        return new LinkedHashMap<String, List<Object>>(person.getAttributes());
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.IPersonAttributeDao#getMultivaluedUserAttributes(java.lang.String)
     */
    @SuppressWarnings("deprecation")
    public final Map<String, List<Object>> getMultivaluedUserAttributes(String uid) {
        final IPerson person = this.getPerson(uid);
        
        if (person == null) {
            return null;
        }

        //Make a mutable copy of the peron's attributes
        return new LinkedHashMap<String, List<Object>>(person.getAttributes());
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.IPersonAttributeDao#getUserAttributes(java.util.Map)
     */
    @SuppressWarnings("deprecation")
    public final Map<String, Object> getUserAttributes(Map<String, Object> seed) {
        final Set<IPerson> people = this.getPeople(seed);

        //Get the first IPerson to return data for
        final IPerson person = (IPerson)DataAccessUtils.singleResult(people);
        
        //If null or no results return null
        if (person == null) {
            return null;
        }

        final Map<String, List<Object>> multivaluedUserAttributes = new LinkedHashMap<String, List<Object>>(person.getAttributes());
        return this.flattenResults(multivaluedUserAttributes);
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.IPersonAttributeDao#getUserAttributes(java.lang.String)
     */
    @SuppressWarnings("deprecation")
    public final Map<String, Object> getUserAttributes(String uid) {
        Validate.notNull(uid, "uid may not be null.");
        
        //Get the attributes from the subclass
        final Map<String, List<Object>> multivaluedUserAttributes = this.getMultivaluedUserAttributes(uid);
        
        return this.flattenResults(multivaluedUserAttributes);
    }

    /**
     * Takes a &lt;String, List&lt;Object>> Map and coverts it to a &lt;String, Object> Map. This implementation takes
     * the first value of each List to use as the value for the new Map.
     * 
     * @param multivaluedUserAttributes The attribute map to flatten.
     * @return A flattened version of the Map, null if the argument was null.
     */
    protected Map<String, Object> flattenResults(Map<String, List<Object>> multivaluedUserAttributes) {
        if (multivaluedUserAttributes == null) {
            return null;
        }
        
        //Convert the <String, List<Object> results map to a <String, Object> map using the first value of each List
        final Map<String, Object> userAttributes = new LinkedHashMap<String, Object>(multivaluedUserAttributes.size());
        
        for (final Map.Entry<String, List<Object>> attrEntry : multivaluedUserAttributes.entrySet()) {
            final String attrName = attrEntry.getKey();
            final List<Object> attrValues = attrEntry.getValue();
            
            final Object value;
            if (attrValues == null || attrValues.size() == 0) {
                value = null;
            }
            else {
                value = attrValues.get(0);
            }
            
            userAttributes.put(attrName, value);
        }
        
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Flattened Map='" + multivaluedUserAttributes + "' into Map='" + userAttributes + "'");
        }
        
        return userAttributes;
    }
}
