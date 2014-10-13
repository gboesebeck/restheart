/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.injectors;

import com.google.common.cache.LoadingCache;
import com.mongodb.DBObject;
import com.softinstigate.restheart.db.CollectionDAO;
import com.softinstigate.restheart.handlers.PipedHttpHandler;
import com.softinstigate.restheart.handlers.RequestContext;
import io.undertow.server.HttpServerExchange;
import java.util.Optional;

/**
 *
 * @author uji
 */
public class CollectionPropsInjectorHandler extends PipedHttpHandler
{
    private static final String SEPARATOR = "_@_@_";
    
    private static boolean cacheEnabled = false;

    /**
     * Creates a new instance of MetadataInjecterHandler
     *
     * @param next
     * @param propertiesLocalCacheEnabled
     */
    public CollectionPropsInjectorHandler(PipedHttpHandler next, boolean propertiesLocalCacheEnabled)
    {
        super(next);

        cacheEnabled = propertiesLocalCacheEnabled;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception
    {
        if (context.getDBName() != null && context.getCollectionName() != null)
        {
            DBObject collProps = null;

            if (!cacheEnabled)
            {
                collProps = CollectionDAO.getCollectionProps(context.getDBName(), context.getCollectionName());
                
                if (collProps != null)
                    collProps.put("_collection-props-cached", false);
            }
            else
            {
                LoadingCache<String, Optional<DBObject>> collectionPropsCache = LocalCachesSingleton.getInstance().getCollectionCache();
                
                Optional<DBObject> _collMetadata = collectionPropsCache.getIfPresent(context.getDBName() + SEPARATOR + context.getCollectionName());
                
                if (_collMetadata != null)
                {
                    if (_collMetadata.isPresent())
                    {
                        collProps = _collMetadata.get();
                        collProps.put("_collection-props-cached", true);
                    }
                    else
                        collProps = null;
                }
                else
                {
                    _collMetadata = collectionPropsCache.get(context.getDBName() + SEPARATOR + context.getCollectionName());
                    
                    if (_collMetadata.isPresent())
                    {
                        collProps = _collMetadata.get();
                        collProps.put("_collection-props-cached", false);
                    }
                    else
                        collProps = null;
                }
            }

            context.setCollectionProps(collProps);
        }

        next.handleRequest(exchange, context);
    }
}