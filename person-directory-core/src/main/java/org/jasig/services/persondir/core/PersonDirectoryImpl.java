package org.jasig.services.persondir.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.jasig.services.persondir.AttributeQuery;
import org.jasig.services.persondir.Person;
import org.jasig.services.persondir.PersonAttributes;
import org.jasig.services.persondir.PersonDirectory;
import org.jasig.services.persondir.core.config.AttributeSourceConfig;
import org.jasig.services.persondir.core.config.CriteriaSearchableAttributeSourceConfig;
import org.jasig.services.persondir.core.config.PersonDirectoryConfig;
import org.jasig.services.persondir.core.config.SimpleAttributeSourceConfig;
import org.jasig.services.persondir.core.worker.AttributeQueryWorker;
import org.jasig.services.persondir.core.worker.CriteriaSearchableAttributeQueryWorker;
import org.jasig.services.persondir.core.worker.SimpleAttributeQueryWorker;
import org.jasig.services.persondir.criteria.Criteria;
import org.jasig.services.persondir.criteria.CriteriaBuilder;
import org.jasig.services.persondir.spi.BaseAttributeSource;
import org.jasig.services.persondir.spi.cache.CacheKeyGenerator;
import org.jasig.services.persondir.spi.gate.AttributeSourceGate;
import org.jasig.services.persondir.util.criteria.AttributeNamesCriteriaProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.util.concurrent.MoreExecutors;

public final class PersonDirectoryImpl implements PersonDirectory {
    protected final Logger logger = LoggerFactory.getLogger(getClass()); 
    
    protected final PersonDirectoryConfig config;
    private final ExecutorService attributeSourceExecutor;

    public PersonDirectoryImpl(PersonDirectoryConfig config) {
        this.config = config;
        
        //Setup executor service
        final ExecutorService executorService = config.getExecutorService();
        if (executorService == null) {
            logger.warn("No ExecutorService was configured, all attribute retrieval will be done serially.");
            this.attributeSourceExecutor = MoreExecutors.sameThreadExecutor();
        }
        else {
            this.attributeSourceExecutor = executorService;
        }
    }

    @Override
    public Person findPerson(String primaryId) {
        final Criteria criteria = CriteriaBuilder.eq(this.config.getPrimaryIdAttribute(), primaryId);
        final AttributeQuery<Criteria> attributeQuery = this.createDefaultQuery(criteria);
        
        final List<Person> results = this.searchForPeople(attributeQuery);
        
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(results.size() + " results were returned for findPerson(" + primaryId + "), 0 or 1 results was expected", 1, results.size());
        }
        
        return results.get(0);
    }
    
    @Override
    public List<Person> searchForPeople(Criteria query) {
        return searchForPeople(createDefaultQuery(query));
    }

    @Override
    public List<Person> searchForPeople(AttributeQuery<Criteria> attributeQuery) {
        //First stop is the mergeCache to see if there is a cached result for the query
        Serializable mergeCacheKey = null;
        final Ehcache mergeCache = config.getMergeCache();
        if (mergeCache != null) {
            final CacheKeyGenerator cacheKeyGenerator = config.getCacheKeyGenerator();
            mergeCacheKey = cacheKeyGenerator.generateCriteriaCacheKey(attributeQuery);
            
            final Element mergeElement = mergeCache.get(mergeCacheKey);
            if (mergeElement != null) {
                @SuppressWarnings("unchecked")
                final List<Person> result = (List<Person>)mergeElement.getObjectValue();
                return result;
            }
        }
        
        //Set of workers that have been submitted for execution
        final Set<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> 
            runningWorkers = new HashSet<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>>();
        
        //Queue where workers are placed when they finish execution
        final BlockingQueue<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> 
            completeWorkers = new LinkedBlockingQueue<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>>();
        
        //Set of sources to be run in passes 2..N
        //First Pass: runs the Criteria against any DAO that it matches against
        final Set<AttributeSourceConfig<? extends BaseAttributeSource>> 
            multiPassSources = this.runFirstPassSources(attributeQuery, runningWorkers, completeWorkers);
        
        //Collects attribute results
        final Map<String, PersonBuilder> personBuilders = new LinkedHashMap<String, PersonBuilder>();

        //Tracks if there are more sources to execute
        boolean hasMoreSources = !multiPassSources.isEmpty();
        
        //Tracks if the result set has changed
        boolean resultHaveChanged = false;
        
        //Loop while hasMoreSources AND resultHaveChanged
        do {
            //We have new results, execute any pending sources on the current result set
            if (resultHaveChanged) {
                //Reset hasMoreSources so that it is only true if one of the results actually has more sources to run
                hasMoreSources = false;

                //Additional Passes: run query on remaining sources if the result set has changed enough so that we might be able to run new queries
                for (final PersonBuilder personBuilder : personBuilders.values()) {
                    final boolean hasMorePending = this.runPendingSources(personBuilder, attributeQuery, runningWorkers, completeWorkers);
                    hasMoreSources = hasMoreSources || hasMorePending;
                }
            }
            
            //Reset resultsHaveChanged flag so that it is only true if we get additional result data from the currently executing workers
            resultHaveChanged = false;
            
            //Get the maximum time to wait for a result based on the currently executing workers
            final long maxWaitTime = getMaxWaitTime(runningWorkers);
            
            //Wait for a result to appear in the completeWorker Queue
            AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>> completeWorker = null;
            try {
                completeWorker = completeWorkers.poll(maxWaitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                //Interrupted by something while waiting for a result, assume we need to abort blocking operations and just return
                //mark the thread as interrupted and break out of the wait-for-results loop
                Thread.currentThread().interrupt();
                break;
            }
            
            if (completeWorker == null) {
                //TODO do we need logging here about the timeouts?

                // uhoh everything in-progress took longer than it should have and we can't have any new results at this point
                // Cancel all in-progress workers and break out of the loop
                for (final AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>> queryWorker : runningWorkers) {
                    queryWorker.cancelFuture(true);
                    break;
                }
            }
            else {
                //A worker has completed, get the result data from the worker and continue to loop on any additional workers
                //that complete during result processing
                while (completeWorker != null) {
                    //Remove the worker from the workers set
                    runningWorkers.remove(completeWorker);
                    
                    //Handle the completed worker
                    resultHaveChanged = resultHaveChanged || handleCompleteWorker(completeWorker, personBuilders, multiPassSources);

                    //Check if there are any more completed workers without waiting
                    completeWorker = completeWorkers.poll();
                }
            }
        } while (hasMoreSources && resultHaveChanged);
        
        //*************************//
        // Querying Is Complete!
        //*************************//
        
        //Build the results list
        final List<Person> results = buildResults(attributeQuery, personBuilders.values());
        
        //Cache the result if the merge cache is enabled
        if (mergeCache != null) {
            mergeCache.put(new Element(mergeCacheKey, results));
        }
        
        return results;
    }

    /**
     * @return true if the handling of the completed worker modified the overal result set 
     */
    protected boolean handleCompleteWorker(
            AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>> completeWorker,
            Map<String, PersonBuilder> personBuilders,
            Set<AttributeSourceConfig<? extends BaseAttributeSource>> multiPassSources) {
        
        boolean resultHaveChanged = false;
        
        //Get the result/error from the complete worker
        final AttributeSourceConfig<? extends BaseAttributeSource> sourceConfig = completeWorker.getSourceConfig();
        try {
            final List<PersonAttributes> results = completeWorker.getResult();
            
            int resultIndex = 0;
            for (final PersonAttributes personAttributes : results) {
                resultIndex++;
                
                //Determine the primary id for the attributes
                final String primaryId = getPrimaryId(personAttributes);

                //Determine the PersonBuilder to merge the attributes into
                PersonBuilder personBuilder;
                if (primaryId != null) {
                    //get/create PersonAttributesBuilder based on the primary id
                    personBuilder = personBuilders.get(primaryId);
                    if (personBuilder == null) {
                        final AttributeQuery<Criteria> originalQuery = completeWorker.getOriginalQuery();
                        personBuilder = new PersonBuilder(primaryId, originalQuery.getQuery(), multiPassSources);
                        personBuilders.put(primaryId, personBuilder);
                    }
                }
                else {
                    //Use the person builder the worker was executed as a sub-query for
                    personBuilder = completeWorker.getPersonBuilder();
                }

                if (personBuilder != null) {
                    //Merge the new results into the builder
                    resultHaveChanged = resultHaveChanged || personBuilder.mergeAttributes(personAttributes, sourceConfig);
                }
                else {
                    logger.warn("No primaryId {} present in result {} from {} for query {}, the result will be ignored.", 
                            primaryId, resultIndex, sourceConfig.getName(), completeWorker.getFilteredQuery());
                }
            }
        }
        catch (Throwable t) {
            logger.warn("'" + sourceConfig.getName() + "' threw an exception while retrieving attributes for query and will be ignored for query: " + completeWorker.getFilteredQuery(), t);
        }
        
        return resultHaveChanged;
    }

    /**
     * @return The set of sources that were not executed on the first pass
     */
    protected Set<AttributeSourceConfig<? extends BaseAttributeSource>> runFirstPassSources(
            AttributeQuery<Criteria> attributeQuery,
            final Set<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> runningWorkers,
            final BlockingQueue<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> completeWorkers) {
        
        final Set<AttributeSourceConfig<? extends BaseAttributeSource>> 
            multiPassSources = new HashSet<AttributeSourceConfig<? extends BaseAttributeSource>>();
        
        for (final AttributeSourceConfig<?> sourceConfig : config.getSourceConfigs()) {

            //Only run Criteria Searchable sources in the first pass since all we have is a original Criteria
            //Check if the source config can run the original Criteria
            if (sourceConfig instanceof CriteriaSearchableAttributeSourceConfig && canRun(attributeQuery, sourceConfig)) {
                final CriteriaSearchableAttributeSourceConfig criteriaSearchableSourceConfig = (CriteriaSearchableAttributeSourceConfig)sourceConfig;
                final CriteriaSearchableAttributeQueryWorker criteriaSearchableQueryWorker = new CriteriaSearchableAttributeQueryWorker(this.config, criteriaSearchableSourceConfig, attributeQuery, completeWorkers);
                runningWorkers.add(criteriaSearchableQueryWorker);
                
                criteriaSearchableQueryWorker.submit(attributeSourceExecutor);
            }
            //Sources that are not run on the first pass are added to the second pass set
            else {
                multiPassSources.add(sourceConfig);
            }
        }
        
        return multiPassSources;
    }

    /**
     * @return The maximum time to wait from "now" for all workers currently in the runningWorkers set to reach their maximum execution time
     */
    protected long getMaxWaitTime(Set<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> runningWorkers) {
        long maxWaitTime = 0;
        for (final AttributeQueryWorker<?, ?> queryWorker : runningWorkers) {
            maxWaitTime = Math.max(maxWaitTime, queryWorker.getCurrentWaitTime());
        }
        return maxWaitTime;
    }

    /**
     * @return true if there there are still un-executed attribute sources for the {@link PersonBuilder}
     */
    protected boolean runPendingSources(
            PersonBuilder personBuilder,
            AttributeQuery<Criteria> attributeQuery,
            Set<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> runningWorkers,
            BlockingQueue<AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>>> completeWorkers) {
        
        //Short-circuit if there are no more pending sources for this person
        final Set<AttributeSourceConfig<? extends BaseAttributeSource>> pendingSources = personBuilder.getPendingSources();
        if (pendingSources.isEmpty()) {
            return false;
        }
        
        //Generate the critera and attribute query to use for subqueries on this person
        final Criteria subqueryCriteria = personBuilder.getSubqueryCriteria();
        final AttributeQuery<Criteria> subAttributeQuery = new AttributeQuery<Criteria>(subqueryCriteria, attributeQuery);

        //Iterate over all sources that have not yet been run for this person
        for (final Iterator<AttributeSourceConfig<? extends BaseAttributeSource>> pendingSourcesItr = pendingSources.iterator(); pendingSourcesItr.hasNext(); ) {
            final AttributeSourceConfig<? extends BaseAttributeSource> sourceConfig = pendingSourcesItr.next();
            
            //Check if the source can execute this query
            if (canRun(subAttributeQuery, sourceConfig)) {
                
                //Create the source-specific query worker
                final AttributeQueryWorker<?, ? extends AttributeSourceConfig<? extends BaseAttributeSource>> queryWorker;
                if (sourceConfig instanceof CriteriaSearchableAttributeSourceConfig) {
                    final CriteriaSearchableAttributeSourceConfig criteriaSearchableSourceConfig = (CriteriaSearchableAttributeSourceConfig)sourceConfig;
                    queryWorker = new CriteriaSearchableAttributeQueryWorker(this.config, criteriaSearchableSourceConfig, subAttributeQuery, completeWorkers);
                }
                else if (sourceConfig instanceof SimpleAttributeSourceConfig) {
                    final SimpleAttributeSourceConfig simpleAttributeSourceConfig = (SimpleAttributeSourceConfig)sourceConfig;
                    queryWorker = new SimpleAttributeQueryWorker(this.config, simpleAttributeSourceConfig, subAttributeQuery, completeWorkers);
                }
                else {
                    throw new IllegalArgumentException(sourceConfig.getClass() + " is not a supported AttributeSourceConfig implementation");
                }
                
                //Add to set of tracked workers
                runningWorkers.add(queryWorker);
                
                //Create worker complete callback and submit the worker
                queryWorker.submit(attributeSourceExecutor);
                
                pendingSourcesItr.remove();
            }
        }
        
        return !pendingSources.isEmpty();
    }
    
    protected String getPrimaryId(PersonAttributes personAttributes) {
        final String primaryIdAttribute = this.config.getPrimaryIdAttribute();
        
        final Map<String, List<Object>> attributes = personAttributes.getAttributes();
        final List<Object> values = attributes.get(primaryIdAttribute);
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        final Object value = values.get(0);
        if (value == null) {
            return null;
        }
        
        return value.toString();
    }
    
    protected boolean canRun(AttributeQuery<Criteria> attributeQuery, AttributeSourceConfig<? extends BaseAttributeSource> sourceConfig) {
        //Capture attribute names from the criteria
        final Criteria criteria = attributeQuery.getQuery();
        final AttributeNamesCriteriaProcessor criteriaAttributeNamesHandler = new AttributeNamesCriteriaProcessor();
        criteria.process(criteriaAttributeNamesHandler);
        final Set<String> queryAttributeNames = criteriaAttributeNamesHandler.getAttributeNames();
        
        final Set<String> requiredQueryAttributes = sourceConfig.getRequiredQueryAttributes();
        final Set<String> optionalQueryAttributes = sourceConfig.getOptionalQueryAttributes();
        if (    //If there are required attributes the query map must contain all of them
                (requiredQueryAttributes.isEmpty() || !queryAttributeNames.containsAll(requiredQueryAttributes))
                &&
                //If there are no required attributes the query map must contain at least one optional attribute
                (!requiredQueryAttributes.isEmpty() || Collections.disjoint(queryAttributeNames, optionalQueryAttributes))) {
            return false;
        }
        
        for (final AttributeSourceGate gate : sourceConfig.getGates()) {
            if (!gate.checkSearch(attributeQuery)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Build the result list from the personBuilders.
     * 
     * @param attributeQuery Original attribute query
     * @param personBuilders map of person builders
     * @return
     */
    protected List<Person> buildResults(AttributeQuery<Criteria> attributeQuery, Collection<PersonBuilder> personBuilders) {
        //Get the original criteria query to do in-memory result filtering
        final Criteria originalCriteria = attributeQuery.getQuery();
        
        //Generate the List<Person> from the builders
        //Each attribute map is matched against the original Criteria
        final ImmutableList.Builder<Person> resultsBuilder = ImmutableList.builder();
        for (final PersonBuilder personBuilder : personBuilders) {
            final Map<String, List<Object>> attributes = personBuilder.getAttributes();
            if (originalCriteria.matches(attributes)) {
                //Only include results that match the original filter
                resultsBuilder.add(personBuilder.build());
            }
        }
        
        return resultsBuilder.build();
    }
    
    @Override
    public Set<String> getSearchableAttributeNames() {
        //TODO cache this for a short time
        final Builder<String> attributeNamesBuilder = ImmutableSet.builder();
        
        for (final AttributeSourceConfig<?> sourceConfig : this.config.getSourceConfigs()) {
            final Set<String> requiredAttributes = sourceConfig.getRequiredQueryAttributes();
            attributeNamesBuilder.addAll(requiredAttributes);
            
            final Set<String> optionalAttributes = sourceConfig.getOptionalQueryAttributes();
            attributeNamesBuilder.addAll(optionalAttributes);
        }
        
        return attributeNamesBuilder.build();
    }

    @Override
    public Set<String> getAvailableAttributeNames() {
        //TODO cache this for a short time
        final Builder<String> attributeNamesBuilder = ImmutableSet.builder();
        
        for (final AttributeSourceConfig<?> sourceConfig : this.config.getSourceConfigs()) {
            final Set<String> availableAttributes = sourceConfig.getAvailableAttributes();
            attributeNamesBuilder.addAll(availableAttributes);
        }
        
        return attributeNamesBuilder.build();
    }

    
    /**
     * Create an {@link AttributeQuery} using the default configuration, wrapping the specified query.
     */
    protected AttributeQuery<Criteria> createDefaultQuery(Criteria criteria) {
        return new AttributeQuery<Criteria>(criteria, config.getDefaultMaxResults(), config.getDefaultQueryTimeout());
    }
}
