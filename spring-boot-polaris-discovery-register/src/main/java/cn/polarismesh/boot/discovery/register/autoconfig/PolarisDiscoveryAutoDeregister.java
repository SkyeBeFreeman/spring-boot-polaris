/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package cn.polarismesh.boot.discovery.register.autoconfig;

import cn.polarismesh.boot.context.PolarisContextConst;
import cn.polarismesh.boot.discovery.register.properties.PolarisDiscoveryProperties;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.client.api.SDKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PolarisDiscoveryAutoDeregister implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisDiscoveryAutoDeregister.class);

    @Autowired
    private SDKContext sdkContext;

    @Autowired
    private ProviderAPI providerAPI;

    @Autowired
    private PolarisDiscoveryProperties polarisDiscoveryProperties;

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        if (!polarisDiscoveryProperties.isEnable()) {
            return;
        }
        InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();
        deregisterRequest.setPort(polarisDiscoveryProperties.getPort());
        deregisterRequest.setService(polarisDiscoveryProperties.getApplicationName());
        if (StringUtils.hasText(polarisDiscoveryProperties.getHost())) {
            deregisterRequest.setHost(polarisDiscoveryProperties.getHost());
        } else {
            deregisterRequest.setHost(sdkContext.getConfig().getGlobal().getAPI().getBindIP());
        }
        if (StringUtils.hasText(polarisDiscoveryProperties.getNamespace())) {
            deregisterRequest.setNamespace(polarisDiscoveryProperties.getNamespace());
        } else {
            deregisterRequest.setNamespace(PolarisContextConst.DEFAULT_NAMESPACE);
        }
        if (StringUtils.hasText(polarisDiscoveryProperties.getToken())) {
            deregisterRequest.setToken(polarisDiscoveryProperties.getToken());
        }
        providerAPI.deRegister(deregisterRequest);
        LOG.info("[Polaris] success to deregister instance {}:{}, service is {}, namespace is {}",
                deregisterRequest.getHost(), deregisterRequest.getPort(), deregisterRequest.getService(),
                deregisterRequest.getNamespace());
        InstanceKey instanceKey = new InstanceKey(deregisterRequest.getNamespace(), deregisterRequest.getService(),
                deregisterRequest.getHost(), deregisterRequest.getPort());
        applicationContext.publishEvent(new InstanceDeregisterEvent(this, instanceKey));
    }
}
