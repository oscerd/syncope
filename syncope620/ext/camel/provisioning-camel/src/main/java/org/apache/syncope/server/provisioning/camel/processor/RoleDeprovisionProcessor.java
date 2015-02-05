/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.server.provisioning.camel.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.server.misc.spring.ApplicationContextProvider;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.server.provisioning.api.propagation.PropagationException;
import org.apache.syncope.server.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.server.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.server.provisioning.api.propagation.PropagationTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleDeprovisionProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(UserDeprovisionProcessor.class);

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected RoleDAO roleDAO;

    @SuppressWarnings("unchecked")
    @Override
    public void process(final Exchange exchange) {
        Long roleKey = exchange.getIn().getBody(Long.class);
        List<String> resources = exchange.getProperty("resources", List.class);

        Role role = roleDAO.authFetch(roleKey);

        Set<String> noPropResourceName = role.getResourceNames();
        noPropResourceName.removeAll(resources);

        List<PropagationTask> tasks =
                propagationManager.getRoleDeleteTaskIds(roleKey, new HashSet<>(resources), noPropResourceName);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        exchange.getOut().setBody(propagationReporter.getStatuses());
    }

}