/* Copyright 2004 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.services.persondir.support;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jasig.services.persondir.IPerson;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.support.merger.MultivaluedAttributeMerger;

/**
 * A {@link IPersonAttributeDao} implementation which iterates over children 
 * IPersonAttributeDaos queries each with the same data and merges their
 * reported attributes in a configurable way. The default merger is
 * {@link MultivaluedAttributeMerger}.
 * 
 * @author andrew.petro@yale.edu
 * @author Eric Dalquist
 * @version $Revision$ $Date$
 * @since uPortal 2.5
 */
public class MergingPersonAttributeDaoImpl extends AbstractAggregatingDefaultQueryPersonAttributeDao {
    public MergingPersonAttributeDaoImpl() {
        this.attrMerger = new MultivaluedAttributeMerger();
    }
    
    
    /**
     * Calls the current IPersonAttributeDao from using the seed.
     * 
     * @see org.jasig.services.persondir.support.AbstractAggregatingDefaultQueryPersonAttributeDao#getAttributesFromDao(java.util.Map, boolean, org.jasig.services.persondir.IPersonAttributeDao, java.util.Set)
     */
    @Override
    protected Set<IPerson> getAttributesFromDao(Map<String, List<Object>> seed, boolean isFirstQuery, IPersonAttributeDao currentlyConsidering, Set<IPerson> resultPeople) {
        return currentlyConsidering.getPeopleWithMultivaluedAttributes(seed);
    }
}