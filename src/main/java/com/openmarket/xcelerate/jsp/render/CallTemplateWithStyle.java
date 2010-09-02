/*
 * Copyright 2009 FatWire Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.openmarket.xcelerate.jsp.render;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import COM.FutureTense.Cache.CacheManager;
import COM.FutureTense.Interfaces.FTValList;
import COM.FutureTense.Interfaces.ICS;
import COM.FutureTense.Interfaces.Utilities;
import COM.FutureTense.Util.ftMessage;

public class CallTemplateWithStyle extends CallTemplateWithContextSetting {
    private static Log LOG = LogFactory.getLog(ftMessage.PAGE_CACHE_DEBUG + ".calltemplate");
    private static final long serialVersionUID = 6785976437241718221L;
    private boolean configLoaded = false;
    /**
     * The default style, used if present
     */
    private String defaultStyle = null;
    /**
     * Do not use the user provided value for style, override
     * 
     */
    private boolean override = true;
    private boolean fixPageCriteria = false;
    private String site, type, tname, cid, style;

    public CallTemplateWithStyle() {
        super();

    }

    void readConfig(ICS ics) {
        if (configLoaded)
            return;
        this.defaultStyle = getProperty(ics, "style");
        this.override = "true".equals(getProperty(ics, "override"));
        this.fixPageCriteria = "true".equals(getProperty(ics, "fixPageCriteria"));
        configLoaded = true;

    }

    private String getProperty(ICS ics, String name) {
        String val = System.getProperty(CallTemplate.class.getName() + "." + name);
        if (!Utilities.goodString(val)) {
            val = ics.GetProperty(CallTemplate.class.getName() + "." + name, "futuretense_xcel.ini", true);
        }
        LOG.trace(CallTemplate.class.getName() + "." + name + "=" + val);
        return val;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallTemplateWithContextSetting#doEndTag
     * (COM.FutureTense.Interfaces.ICS, boolean)
     */
    @Override
    protected int doEndTag(ICS ics, boolean bDebug) throws Exception {
        readConfig(ics);
        if (defaultStyle != null) {
            super.setStyle(defaultStyle);
        } else if (override || style == null) {
            String newStyle = fixStyle(ics);

            super.setStyle(newStyle);
        }
        site = null;
        type = null;
        tname = null;
        cid = null;
        style = null;
        return super.doEndTag(ics, bDebug);
    }

    private String fixStyle(ICS ics) {

        /**
         * Considerations 1) Check target for parameter renderstyle and use that
         * 
         */
        String pname;

        if (tname.startsWith("/")) // typeless
        {
            pname = site + tname;
        } else {
            pname = site + "/" + type + "/" + tname;
        }
        String proposal = (String) ics.getPageData(pname).getDefaultArguments().get("renderstyle");
        if (!Utilities.goodString(proposal)) {
            boolean targetCached = this.isCacheable(ics, pname);
            boolean currentCached = this.isCacheable(ics, ics.GetVar(ftMessage.PageName));
           /* TODO consider time dimension
            * 
            * consider that if the target pagelet is short lived and current is long lived, style=pagelet is appropriate
            * if current is short lived (maybe also due to a logged unknowndeps) and target is long, style=embedded
            * if both are short lived, style=element might be appropriate  
            */
            
            proposal = proposeStyle(ics, pname, currentCached, targetCached);
            if (LOG.isDebugEnabled())
                LOG.debug("Setting style to '" + proposal + (style != null ? "' (user did set '" + style + "')" : "'")
                        + " for calltemplate to '" + pname + "' with " + type + "," + cid + "," + this.getAttributes()
                        + " in element: '" + ics.ResolveVariables("CS.elementname") + "', caching: '" + currentCached
                        + "/" + targetCached + "', page: " + ics.pageURL());

        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Setting style to '" + proposal + (style != null ? "' (user did set '" + style + "')" : "'")
                        + " for calltemplate to '" + pname + "' with " + type + "," + cid + "," + this.getAttributes()
                        + " in element: '" + ics.ResolveVariables("CS.elementname")
                        + "', based on renderstyle defined on the target, page: " + ics.pageURL());
        }
        return proposal;

    }

    private String proposeStyle(ICS ics, String pname, boolean currentCache, boolean targetCache) {
        if (currentCache == false) // we are not caching for the current pagelet
        {
            if (targetCache == false) {
                return "element"; // call as element is target is also not
                                  // cacheable
            } else {
                checkPageCriteria(ics, pname);
                return "pagelet"; // otherwise call as pagelet
            }

        } else { // currently we are caching

            if (targetCache == false) {
                checkPageCriteria(ics, pname);
                return "pagelet";
            } else {
                // LOG.debug("getvar.cid=" + ics.GetVar("cid") + " at " +
                // ics.pageURL());

                FTValList m = COM.FutureTense.Interfaces.Utilities.getParams(ics.pageURL());
                String pageCid = m.getValString("cid");
                if (pageCid != null && !pageCid.equals(ics.GetVar("cid"))) {
                    LOG.warn(ics.GetVar("cid") + " does not match cid (" + pageCid + ") in " + ics.pageURL());
                }
                // should we check if cid is current page criteria, we are a
                // Template??
                if (cid != null && cid.equals(pageCid)) {
                    // if c/cid does not change than we call this as an element,
                    // as reuse is unlikely
                    if (LOG.isTraceEnabled())
                        LOG.trace("Calling " + pname + " as an element from " + ics.ResolveVariables("CS.elementname")
                                + " because cid is same as on current pagelet.");
                    return "element";
                } else {
                    checkPageCriteria(ics, pname);
                    return "embedded"; // this is calltemplate, assuming that
                                       // headers/footers/leftnavs etc will be
                                       // via CSElements/SiteEntry
                }
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void checkPageCriteria(ICS ics, String target) {
        Object o = this.getAttributes();
        if (o instanceof Map) {
            String[] pc = ics.pageCriteriaKeys(target);
            if (pc == null)
                pc = new String[0];
            Map<String, ?> m = (Map<String, ?>) o;
            // m.keySet().retainAll(Arrays.asList(pc));
            for (Iterator<?> i = m.entrySet().iterator(); i.hasNext();) {
                Entry<String, ?> e = (Entry<String, ?>) i.next();
                String key = e.getKey();
                boolean found = false;
                for (String c : pc) {
                    if (c.equals(key)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOG.error("Argument '" + key + "' not found as PageCriterium on " + target
                            + ". Calling element is " + ics.ResolveVariables("CS.elementname") + ". Arguments are: "
                            + m.keySet().toString() + ". PageCriteria: " + Arrays.asList(pc));
                    // we could correct this by calling as an element
                    // or by removing the argument
                    if (fixPageCriteria) {
                        i.remove();
                        LOG.warn("Argument '" + key + "' is removed from the call to '" + target
                                + "' as it is not a PageCriterium.");
                    }

                }
            }

        }

    }

    /**
     * Checks if the pagelet should be cached. Takes into consideration if
     * current pagelet is rendered for Satellite Server.
     * 
     * @param ics
     * @param pname
     *            the pagename
     * @return
     */
    boolean isCacheable(ICS ics, String pname) {
        return CacheManager.clientIsSS(ics) ? ics.getPageData(pname).getSSCacheInfo().shouldCache() : ics.getPageData(
                pname).getCSCacheInfo().shouldCache();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallTemplate#setC(java.lang.String)
     */
    @Override
    public void setC(String sType) {
        type = sType;
        super.setC(sType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallTemplate#setSite(java.lang.String
     * )
     */
    @Override
    public void setSite(String sSite) {
        site = sSite;
        super.setSite(sSite);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallTemplate#setTname(java.lang.String
     * )
     */
    @Override
    public void setTname(String sTName) {
        tname = sTName;
        super.setTname(sTName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallTemplate#setCid(java.lang.String)
     */
    @Override
    public void setCid(String sAssetId) {
        cid = sAssetId;
        super.setCid(sAssetId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.openmarket.xcelerate.jsp.render.CallTemplate#setStyle(java.lang.String
     * )
     */
    @Override
    public void setStyle(String s) {
        style = s;
        super.setStyle(s);
    }

}
