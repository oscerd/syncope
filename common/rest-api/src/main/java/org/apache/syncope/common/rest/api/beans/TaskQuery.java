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
package org.apache.syncope.common.rest.api.beans;

import javax.validation.constraints.Min;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class TaskQuery extends AbstractQuery {

    private static final long serialVersionUID = -8792519310029596796L;

    public static class Builder extends AbstractQuery.Builder<TaskQuery, Builder> {

        @Override
        protected TaskQuery newInstance() {
            return new TaskQuery();
        }

        public Builder resource(final String resource) {
            getInstance().setResource(resource);
            return this;
        }

        public Builder anyTypeKind(final AnyTypeKind anyTypeKind) {
            getInstance().setAnyTypeKind(anyTypeKind);
            return this;
        }

        public Builder anyTypeKey(final Long anyTypeKey) {
            getInstance().setAnyTypeKey(anyTypeKey);
            return this;
        }

    }

    private String resource;

    private AnyTypeKind anyTypeKind;

    private Long anyTypeKey;

    public String getResource() {
        return resource;
    }

    @QueryParam(JAXRSService.PARAM_RESOURCE)
    public void setResource(final String resource) {
        this.resource = resource;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @QueryParam(JAXRSService.PARAM_ANYTYPE_KIND)
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public Long getAnyTypeKey() {
        return anyTypeKey;
    }

    @Min(1)
    @QueryParam(JAXRSService.PARAM_ANYTYPE_KEY)
    public void setAnyTypeKey(final Long anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

}