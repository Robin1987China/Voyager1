/*
 * Copyright (c) 2026 Voyager1
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
 */

package io.voyager1.service.k8s;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * fabric8 客户端 mock 冒烟测试：验证 SDK 在运行时可正常 list/create/scale/delete
 *
 * @since 2026/8/14
 */
@EnableKubernetesMockClient(crud = true, https = false)
public class Fabric8ClientSmokeTest {

    KubernetesClient client;

    @Test
    public void testDeploymentLifecycle() {
        Deployment deployment = new DeploymentBuilder()
            .withNewMetadata().withName("test-deploy").withNamespace("default").endMetadata()
            .withNewSpec()
            .withReplicas(1)
            .withNewSelector().addToMatchLabels("app", "test").endSelector()
            .withNewTemplate()
            .withNewMetadata().addToLabels("app", "test").endMetadata()
            .withNewSpec().addNewContainer().withName("c").withImage("nginx").endContainer().endSpec()
            .endTemplate()
            .endSpec()
            .build();
        client.resource(deployment).create();

        DeploymentList list = client.apps().deployments().inNamespace("default").list();
        Assertions.assertEquals(1, list.getItems().size());
        Assertions.assertEquals("test-deploy", list.getItems().get(0).getMetadata().getName());

        // scale
        Deployment scaled = client.apps().deployments().inNamespace("default").withName("test-deploy").scale(3);
        Assertions.assertEquals(3, scaled.getSpec().getReplicas());

        // delete
        boolean deleted = client.apps().deployments().inNamespace("default").withName("test-deploy").delete().size() > 0;
        Assertions.assertTrue(deleted);
        Assertions.assertTrue(client.apps().deployments().inNamespace("default").list().getItems().isEmpty());
    }

    @Test
    public void testPodList() {
        client.resource(new PodBuilder()
            .withNewMetadata().withName("pod1").withNamespace("default").endMetadata()
            .withNewSpec().addNewContainer().withName("c").withImage("nginx").endContainer().endSpec()
            .build()).create();

        Assertions.assertEquals(1, client.pods().inNamespace("default").list().getItems().size());
        Assertions.assertEquals("pod1", client.pods().inNamespace("default").withName("pod1").get().getMetadata().getName());
    }
}
