// Copyright 2006, 2007 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.internal.services;

import org.apache.tapestry.internal.events.InvalidationListener;
import org.apache.tapestry.internal.structure.Page;
import org.apache.tapestry.ioc.internal.util.CollectionFactory;
import static org.apache.tapestry.ioc.internal.util.CollectionFactory.newMap;
import org.apache.tapestry.ioc.services.ThreadLocale;
import org.apache.tapestry.services.ComponentClassResolver;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A very naive implementation just to get us past the start line.
 * <p/>
 * Registered as an invalidation listener with the
 * {@link org.apache.tapestry.internal.services.PageLoader}; thus the pool is cleared whenever
 */
public class PagePoolImpl implements PagePool, InvalidationListener
{
    private final Logger _logger;

    private final PageLoader _pageLoader;

    private final ThreadLocale _threadLocale;

    private final ComponentClassResolver _resolver;

    private final Map<PageLocator, List<Page>> _pool = newMap();

    public PagePoolImpl(Logger logger, PageLoader pageLoader, ThreadLocale threadLocale,
                        ComponentClassResolver resolver)
    {
        _logger = logger;
        _pageLoader = pageLoader;
        _threadLocale = threadLocale;
        _resolver = resolver;
    }

    public synchronized Page checkout(String logicalPageName)
    {
        String canonicalPageName = _resolver.canonicalizePageName(logicalPageName);

        Locale locale = _threadLocale.getLocale();
        List<Page> pages = _pool.get(new PageLocator(canonicalPageName, locale));

        // When we load a page, we load it in the active locale, whatever that is.
        // Even if the locale later changes, we keep the version we originally got.
        // This is not as bad in T5 as in T4, since a seperate request will
        // render the response (and will have a chance to get the page in a different locale).

        if (pages == null || pages.isEmpty())
            return _pageLoader.loadPage(canonicalPageName, locale);

        // Remove and return the last page in the pool.

        return pages.remove(pages.size() - 1);
    }

    public synchronized void release(Page page)
    {
        boolean dirty = page.detached();

        if (dirty)
        {
            _logger.error(ServicesMessages.pageIsDirty(page));
            return;
        }

        PageLocator locator = new PageLocator(page.getLogicalName(), page.getLocale());
        List<Page> pages = _pool.get(locator);

        if (pages == null)
        {
            pages = CollectionFactory.newList();
            _pool.put(locator, pages);
        }

        pages.add(page);
    }

    public synchronized void objectWasInvalidated()
    {
        _pool.clear();
    }

}