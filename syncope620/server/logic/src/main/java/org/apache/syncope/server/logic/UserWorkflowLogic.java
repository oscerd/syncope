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
package org.apache.syncope.server.logic;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.mod.AbstractAttributableMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.persistence.api.dao.UserDAO;
import org.apache.syncope.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.server.logic.data.UserDataBinder;
import org.apache.syncope.server.workflow.api.UserWorkflowAdapter;
import org.apache.syncope.provisioning.api.WorkflowResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserWorkflowLogic extends AbstractTransactionalLogic<WorkflowFormTO> {

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private UserDataBinder binder;

    @Autowired
    private UserDAO userDAO;

    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @Transactional(rollbackFor = { Throwable.class })
    public WorkflowFormTO claimForm(final String taskId) {
        return uwfAdapter.claimForm(taskId);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    public UserTO executeWorkflowTask(final UserTO userTO, final String taskId) {
        WorkflowResult<Long> updated = uwfAdapter.execute(userTO, taskId);

        UserMod userMod = new UserMod();
        userMod.setKey(userTO.getKey());

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                        new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, null),
                        updated.getPropByRes(), updated.getPerformedTasks()));

        taskExecutor.execute(tasks);

        return binder.getUserTO(updated.getResult());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public WorkflowFormTO getFormForUser(final Long key) {
        User user = userDAO.authFecthUser(key);
        return uwfAdapter.getForm(user.getWorkflowId());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @Transactional(rollbackFor = { Throwable.class })
    public List<WorkflowFormTO> getForms() {
        return uwfAdapter.getForms();
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public List<WorkflowFormTO> getForms(final Long key, final String formName) {
        User user = userDAO.authFecthUser(key);
        return uwfAdapter.getForms(user.getWorkflowId(), formName);
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @Transactional(rollbackFor = { Throwable.class })
    public UserTO submitForm(final WorkflowFormTO form) {
        WorkflowResult<? extends AbstractAttributableMod> updated = uwfAdapter.submitForm(form);

        // propByRes can be made empty by the workflow definition if no propagation should occur 
        // (for example, with rejected users)
        if (updated.getResult() instanceof UserMod
                && updated.getPropByRes() != null && !updated.getPropByRes().isEmpty()) {

            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                            new AbstractMap.SimpleEntry<UserMod, Boolean>((UserMod) updated.getResult(), Boolean.TRUE),
                            updated.getPropByRes(),
                            updated.getPerformedTasks()));

            taskExecutor.execute(tasks);
        }

        return binder.getUserTO(updated.getResult().getKey());
    }

    @Override
    protected WorkflowFormTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}