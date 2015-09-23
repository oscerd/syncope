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
package org.apache.syncope.client.console.panels;

import static org.apache.wicket.Component.ENABLE;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.provision.ProvisionWizardBuilder;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Resource form.
 */
public class ResourceModal extends AbstractResourceModal {

    private static final long serialVersionUID = 1734415311027284221L;

    private final boolean createFlag;

    public ResourceModal(
            final BaseModal<Serializable> modal,
            final PageReference pageRef,
            final ResourceTO resourceTO,
            final boolean createFlag) {

        super(modal, pageRef);

        this.createFlag = createFlag;

        final List<ITab> tabs = new ArrayList<>();
        add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", tabs));

        //--------------------------------
        // Resource details panel
        //--------------------------------
        tabs.add(new AbstractTab(new ResourceModel("resource", "resource")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ResourceDetailsPanel(panelId, resourceTO,
                        resourceRestClient.getPropagationActionsClasses(), createFlag);
            }
        });
        //--------------------------------

        //--------------------------------
        // Resource provision panels
        //--------------------------------
        final ListViewPanel.Builder<ProvisionTO> builder = ListViewPanel.builder(ProvisionTO.class, pageRef);
        builder.setItems(resourceTO.getProvisions());
        builder.includes("anyType", "objectClass");

        builder.addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435504L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<ProvisionTO>(provisionTO, 2, target));
            }
        }, ActionLink.ActionType.MAPPING, Entitlement.RESOURCE_UPDATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435514L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<ProvisionTO>(provisionTO, 3, target));
            }
        }, ActionLink.ActionType.ACCOUNT_LINK, Entitlement.RESOURCE_UPDATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435524L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                provisionTO.setSyncToken(null);
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemFinishEvent<ProvisionTO>(provisionTO, target));
            }
        }, ActionLink.ActionType.RESET_TIME, Entitlement.RESOURCE_UPDATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435534L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemActionEvent<ProvisionTO>(SerializationUtils.clone(provisionTO), target));
            }
        }, ActionLink.ActionType.CLONE, Entitlement.RESOURCE_CREATE).addAction(new ActionLink<ProvisionTO>() {

            private static final long serialVersionUID = -3722207913631435544L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ProvisionTO provisionTO) {
                resourceTO.getProvisions().remove(provisionTO);
                send(pageRef.getPage(), Broadcast.DEPTH,
                        new AjaxWizard.NewItemFinishEvent<ProvisionTO>(null, target));
            }
        }, ActionLink.ActionType.DELETE, Entitlement.RESOURCE_DELETE);

        builder.addNewItemPanelBuilder(new ProvisionWizardBuilder("wizard", resourceTO, pageRef));
        builder.addNotificationPanel(modal.getFeedbackPanel());

        tabs.add(new AbstractTab(new ResourceModel("provisions", "provisions")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return builder.build(panelId);
            }
        });
        //--------------------------------

        //--------------------------------
        // Resource connector configuration panel
        //--------------------------------
        tabs.add(new AbstractTab(new ResourceModel("connectorProperties", "connectorProperties")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                final ResourceConnConfPanel panel = new ResourceConnConfPanel(panelId, resourceTO, createFlag);
                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE, Entitlement.CONNECTOR_READ);
                return panel;
            }
        });
        //--------------------------------

        //--------------------------------
        // Resource security panel
        //--------------------------------
        tabs.add(new AbstractTab(new ResourceModel("security", "security")) {

            private static final long serialVersionUID = -5861786415855103549L;

            @Override
            public Panel getPanel(final String panelId) {
                return new ResourceSecurityPanel(panelId, resourceTO);
            }
        });
        //--------------------------------
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        modal.getFeedbackPanel().refresh(target);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        final ResourceTO resourceTO = (ResourceTO) form.getDefaultModelObject();

        boolean connObjectKeyError = false;

        final Collection<ProvisionTO> provisions = new ArrayList<>(resourceTO.getProvisions());

        for (ProvisionTO provision : provisions) {
            if (provision != null) {
                if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                    resourceTO.getProvisions().remove(provision);
                } else {
                    int uConnObjectKeyCount = CollectionUtils.countMatches(
                            provision.getMapping().getItems(), new Predicate<MappingItemTO>() {

                                @Override
                                public boolean evaluate(final MappingItemTO item) {
                                    return item.isConnObjectKey();
                                }
                            });

                    connObjectKeyError = uConnObjectKeyCount != 1;
                }
            }
        }

        if (connObjectKeyError) {
            error(getString("connObjectKeyValidation"));
            modal.getFeedbackPanel().refresh(target);
        } else {
            try {
                if (createFlag) {
                    resourceRestClient.create(resourceTO);
                    send(pageRef.getPage(), Broadcast.BREADTH, new CreateEvent(
                            resourceTO.getKey(),
                            resourceTO.getKey(),
                            TopologyNode.Kind.RESOURCE,
                            resourceTO.getConnector(),
                            target));
                } else {
                    resourceRestClient.update(resourceTO);
                }

                if (pageRef.getPage() instanceof AbstractBasePage) {
                    ((AbstractBasePage) pageRef.getPage()).setModalResult(true);
                }
                modal.close(target);
            } catch (Exception e) {
                LOG.error("Failure managing resource {}", resourceTO, e);
                error(getString(Constants.ERROR) + ": " + e.getMessage());
                modal.getFeedbackPanel().refresh(target);
            }
        }
    }

}