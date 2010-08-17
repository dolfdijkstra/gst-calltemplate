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

import COM.FutureTense.Interfaces.ICS;

/**
 * 
 * @author Dolf Dijkstra
 *
 */
		
public class CallTemplateWithContextSetting extends CallTemplate {

	private static final long serialVersionUID = 1L;
	private boolean hasContext = false;
	
	protected int doEndTag(ICS ics, boolean bDebug) throws Exception {
		if (!hasContext) {
			super.setContext(""); //setting context to empty string to help coder in not having to set context in each tag.
		}
		int ret = super.doEndTag(ics, bDebug);
        hasContext = false;
        return ret;
	}
    
    @Override
    public void setContext(String context) {
	    hasContext = true;
	    super.setContext(context);
    }

    @Override
    public void release() {
            hasContext = false;
            super.release();
    }


}
