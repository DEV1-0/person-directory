/* Copyright 2004 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.services.persondir.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.jasig.services.persondir.IPersonAttributes;
import org.springframework.dao.support.DataAccessUtils;


/**
 * Abstract class implementing the IPersonAttributeDao method  {@link org.jasig.services.persondir.IPersonAttributeDao#getPerson(String)}
 * by delegation to {@link org.jasig.services.persondir.IPersonAttributeDao#getPeopleWithMultivaluedAttributes(Map)} using a configurable
 * default attribute name. If {@link org.jasig.services.persondir.IPersonAttributeDao#getPeopleWithMultivaluedAttributes(Map)} returnes
 * more than one {@link IPersonAttributes} is returned {@link org.springframework.dao.IncorrectResultSizeDataAccessException} is thrown.
 * 
 * <br>
 * <br>
 * Configuration:
 * <table border="1">
 *     <tr>
 *         <th align="left">Property</th>
 *         <th align="left">Description</th>
 *         <th align="left">Required</th>
 *         <th align="left">Default</th>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">defaultAttribute</td>
 *         <td>
 *             The attribute to use for the key in the {@link Map} passed to {@link org.jasig.services.persondir.IPersonAttributeDao#getPeopleWithMultivaluedAttributes(Map)}
 *             when {@link #getPerson(String)} is called. The value is the uid passed to the method.
 *         </td>
 *         <td valign="top">No</td>
 *         <td valign="top">"username"</td>
 *     </tr>
 * </table>
 * 
 * @author Eric Dalquist
 * @version $Revision$ $Date$
 * @since uPortal 2.5
 */
public abstract class AbstractDefaultAttributePersonAttributeDao extends AbstractFlatteningPersonAttributeDao {
    /**
     * Defaults attribute to use for a simple query
     */
    private String defaultAttribute = "username";

    /**
     * @see org.jasig.services.persondir.IPersonAttributeDao#getPerson(java.lang.String)
     * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one matching {@link IPersonAttributes} is found.
     */
    public final IPersonAttributes getPerson(String uid) {
        Validate.notNull(uid, "uid may not be null.");
        
        //Generate the seed map for the uid
        final Map<String, List<Object>> seed = this.toSeedMap(uid);
        
        //Run the query using the seed
        final Set<IPersonAttributes> people = this.getPeopleWithMultivaluedAttributes(seed);
        
        //Ensure a single result is returned
        IPersonAttributes person = (IPersonAttributes)DataAccessUtils.singleResult(people);
        if (person == null) {
            return null;
        }
        
        //Force set the name of the returned IPersonAttributes if it isn't provided in the return object
        if (person.getName() == null) {
            person = new NamedPersonImpl(uid, person.getAttributes());
        }
        
        return person;
    }


    /**
     * Converts the uid to a multi-valued seed Map using the value from {@link #getDefaultAttributeName()}
     * as the key.
     */
    protected Map<String, List<Object>> toSeedMap(String uid) {
        final List<Object> values = Collections.singletonList((Object)uid);
        final Map<String, List<Object>> seed = Collections.singletonMap(this.defaultAttribute, values);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Created seed map='" + seed + "' for uid='" + uid + "'");
        }
        return seed;
    }


    /**
     * Returns the attribute set by {@link #setDefaultAttributeName(String)} or
     * if it has not been called the default value "uid" is returned.
     * 
     * @return The default single string query attribute, will never be null.
     */
    public final String getDefaultAttributeName() {
        return this.defaultAttribute;
    }
    
    /**
     * Sets the attribute to use for {@link #getUserAttributes(String)} queries.
     * It cannot be <code>null</code>.
     * 
     * @param name The attribute name to set as default.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    public final void setDefaultAttributeName(final String name) {
        Validate.notNull(name, "The default attribute name may not be null");
        this.defaultAttribute = name;
    }
}
