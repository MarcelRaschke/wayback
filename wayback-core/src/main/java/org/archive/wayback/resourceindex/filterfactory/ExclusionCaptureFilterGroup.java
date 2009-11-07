/* ExclusionCaptureFilterGroup
 *
 * $Id$
 *
 * Created on Nov 7, 2009.
 *
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of wayback.
 *
 * wayback is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * wayback is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with wayback-svn; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.wayback.resourceindex.filterfactory;

import java.util.List;

import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.SearchResults;
import org.archive.wayback.core.WaybackRequest;
import org.archive.wayback.exception.AccessControlException;
import org.archive.wayback.exception.ResourceNotInArchiveException;
import org.archive.wayback.resourceindex.filters.CounterFilter;
import org.archive.wayback.util.ObjectFilter;
import org.archive.wayback.util.ObjectFilterChain;

public class ExclusionCaptureFilterGroup implements CaptureFilterGroup {

	private ObjectFilterChain<CaptureSearchResult> chain = null;
	private CounterFilter preCounter = null;
	private CounterFilter postCounter = null;
	String requestUrl = null;
	
	public ExclusionCaptureFilterGroup(WaybackRequest request) {
		
		// checks an exclusion service for every matching record
		ObjectFilter<CaptureSearchResult> exclusion = 
			request.getExclusionFilter();
		chain = new ObjectFilterChain<CaptureSearchResult>();
		if(exclusion != null) {
			preCounter = new CounterFilter();
			// count how many results got to the ExclusionFilter:
			chain.addFilter(preCounter);
			chain.addFilter(exclusion);
			// count how many results got past the ExclusionFilter:
			requestUrl = request.getRequestUrl();
		}
		postCounter = new CounterFilter();
		chain.addFilter(postCounter);
	}
	
	public List<ObjectFilter<CaptureSearchResult>> getFilters() {
		return chain.getFilters();
	}

	public void annotateResults(SearchResults results)
			throws AccessControlException, ResourceNotInArchiveException {
		if(postCounter.getNumMatched() == 0) {

			// nothing got to the counter after exclusions. If we have 
			// exclusions (detected by preCounter being non-null, and the 
			// preCounter passed any results, then they were all filtered by
			// the exclusions filter.
			if(preCounter != null && preCounter.getNumMatched() > 0) {
				throw new AccessControlException("All results Excluded");
			}
			ResourceNotInArchiveException e = 
				new ResourceNotInArchiveException("the URL " + requestUrl
					+ " is not in the archive.");
			e.setCloseMatches(results.getCloseMatches());
			throw e;
		}
	}
}