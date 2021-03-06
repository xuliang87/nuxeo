/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Pluggable policy descriptor for core security
 *
 * @author Anahide Tchertchian
 */
@XObject("policy")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class SecurityPolicyDescriptor implements Comparable<SecurityPolicyDescriptor> {

    @XNode("@name")
    @XRegistryId
    private String name;

    @XNode("@class")
    private Class<Object> policy;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    private boolean enabled;

    @XNode("@order")
    private int order = 0;

    public String getName() {
        return name;
    }

    public Class<Object> getPolicy() {
        return policy;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(SecurityPolicyDescriptor anotherPolicy) {
        return order - anotherPolicy.order;
    }

}
