package org.jasig.services.persondir.core.criteria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jasig.services.persondir.criteria.AndCriteria;
import org.jasig.services.persondir.criteria.Criteria;
import org.jasig.services.persondir.criteria.CriteriaBuilder;
import org.jasig.services.persondir.criteria.EqualsCriteria;
import org.jasig.services.persondir.criteria.NotCriteria;
import org.jasig.services.persondir.criteria.OrCriteria;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CriteriaToMapFilteringProcessorTest {
    @Test
    public void testSingleCriteriaIncluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return true;
            }
        });
        
        final Criteria c = new EqualsCriteria("attr", "value");
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of("attr", "value");
        assertEquals(excpected, filtered);
    }

    @Test
    public void testSingleCriteriaExcluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return false;
            }
        });
        
        final Criteria c = new EqualsCriteria("attr", "value");
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        assertTrue(filtered.isEmpty());
    }
    
    @Test
    public void testSingleNotCriteriaIncluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return true;
            }
        });
        
        final Criteria c = new NotCriteria(new EqualsCriteria("attr", "value"));
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of("attr", "value");
        assertEquals(excpected, filtered);
    }

    @Test
    public void testSingleNotCriteriaExcluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return false;
            }
        });
        
        final Criteria c = new NotCriteria(new EqualsCriteria("attr", "value"));
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        assertTrue(filtered.isEmpty());
    }
    
    @Test
    public void testSingleAndCriteriaIncluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return true;
            }
        });
        
        final Criteria ec = new EqualsCriteria("attr", "value");
        final Criteria c = new AndCriteria(ec);
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of("attr", "value");
        assertEquals(excpected, filtered);
    }

    @Test
    public void testSingleAndCriteriaExcluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return false;
            }
        });
        
        final Criteria c = new AndCriteria(new EqualsCriteria("attr", "value"));
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        assertTrue(filtered.isEmpty());
    }
    
    @Test
    public void testSingleOrCriteriaIncluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return true;
            }
        });
        
        final Criteria ec = new EqualsCriteria("attr", "value");
        final Criteria c = new OrCriteria(ec);
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of("attr", "value");
        assertEquals(excpected, filtered);
    }

    @Test
    public void testSingleOrCriteriaExcluded() {
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            public Boolean apply(String input) {
                return false;
            }
        });
        
        final Criteria c = new OrCriteria(new EqualsCriteria("attr", "value"));
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        assertTrue(filtered.isEmpty());
    }
    
    @Test
    public void testMultipleAndCriteriaIncExc() {
        @SuppressWarnings("unchecked")
        final Function<String, Boolean> filter = mock(Function.class);
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(filter);
        
        when(filter.apply(anyString())).thenReturn(true, false, true);
        
        final Criteria c = new AndCriteria(new EqualsCriteria("attr1", "value1"), new EqualsCriteria("attr2", "value2"), new EqualsCriteria("attr3", "value3"));
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of("attr1", "value1", "attr3", "value3");
        assertEquals(excpected, filtered);
    }
    
    @Test
    public void testMultipleOrCriteriaIncExc() {
        @SuppressWarnings("unchecked")
        final Function<String, Boolean> filter = mock(Function.class);
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(filter);
        
        when(filter.apply(anyString())).thenReturn(true, false, true);
        
        final Criteria c = new OrCriteria(new EqualsCriteria("attr1", "value1"), new EqualsCriteria("attr2", "value2"), new EqualsCriteria("attr3", "value3"));
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of("attr1", "value1", "attr3", "value3");
        assertEquals(excpected, filtered);
    }
    
    @Test
    public void testComplexCriteria() {
        final Random r = new Random(2);
        final CriteriaToMapFilteringProcessor criteriaFilteringProcessor = new CriteriaToMapFilteringProcessor(new Function<String, Boolean>() {
            private final Map<String, Boolean> included = new HashMap<String, Boolean>();
            public Boolean apply(String input) {
                Boolean result = included.get(input);
                if (result != null) {
                    return result;
                }
                result = r.nextBoolean();
                included.put(input, result);
                return result;
            }
        });
        
        final Criteria c =
                CriteriaBuilder.or(
                    CriteriaBuilder.and(
                        CriteriaBuilder.eq("phone", null),
                        CriteriaBuilder.eq("firstName", "jane"),
                        CriteriaBuilder.gt("age", 13),
                        CriteriaBuilder.gte("weight", 14),
                        CriteriaBuilder.lt("height", 15),
                        CriteriaBuilder.lte("length", 16),
                        CriteriaBuilder.like("lastName", "Dalq*")
                    ),
                    CriteriaBuilder.not(
                        CriteriaBuilder.and(
                            CriteriaBuilder.eq("phone", null),
                            CriteriaBuilder.eq("firstName", "jane"),
                            CriteriaBuilder.gt("age", 13),
                            CriteriaBuilder.gte("weight", 13),
                            CriteriaBuilder.lt("height", 15),
                            CriteriaBuilder.lte("length", 16),
                            CriteriaBuilder.like("lastName", "Dalq*")
                        )
                    ),
                    CriteriaBuilder.not(
                        CriteriaBuilder.or(
                            CriteriaBuilder.eq("phone", null),
                            CriteriaBuilder.not(
                                CriteriaBuilder.eq("firstName", "jane")
                            )
                        )
                    )
                );
        c.process(criteriaFilteringProcessor);
        
        final Map<String, Object> filtered = criteriaFilteringProcessor.getAttributes();
        //<{firstName=jane, weight=[14, 13], lastName=Dalq*}>
        final Map<String, Object> excpected = ImmutableMap.<String, Object>of(
                "firstName", "jane",
                "weight", ImmutableSet.of(14, 13), 
                "lastName", "Dalq*");
        assertEquals(excpected, filtered);
    }
}
