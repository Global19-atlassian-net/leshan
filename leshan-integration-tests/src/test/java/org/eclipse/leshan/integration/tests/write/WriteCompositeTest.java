/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.write;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.integration.tests.observe.TestObservationListener;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WriteCompositeTest {
    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                                { ContentFormat.SENML_JSON }, //
                                { ContentFormat.SENML_CBOR } });
    }

    private ContentFormat contentFormat;

    public WriteCompositeTest(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;
    }

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_write_resources() throws InterruptedException {
        // write device timezone and offset
        LwM2mSingleResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mSingleResource defaultMinPeriod = LwM2mSingleResource.newIntegerResource(2, 100);

        Map<String, LwM2mNode> nodes = new HashMap<>();
        nodes.put("/3/0/14", utcOffset);
        nodes.put("/1/0/2", defaultMinPeriod);

        WriteCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(contentFormat, nodes));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 14));
        assertEquals(utcOffset, readResponse.getContent());
        readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(1, 0, 2));
        assertEquals(defaultMinPeriod, readResponse.getContent());
    }

    @Test
    public void can_write_resource_and_instance() throws InterruptedException {
        // create value
        LwM2mSingleResource utcOffset = LwM2mSingleResource.newStringResource(14, "+02");
        LwM2mPath resourceInstancePath = new LwM2mPath(IntegrationTestHelper.TEST_OBJECT_ID, 0,
                IntegrationTestHelper.STRING_RESOURCE_INSTANCE_ID, 100);
        LwM2mResourceInstance testStringResourceInstance = LwM2mResourceInstance
                .newStringInstance(resourceInstancePath.getResourceInstanceId(), "test_string_instance");

        // add it to the map
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(new LwM2mPath("/3/0/14"), utcOffset);
        nodes.put(resourceInstancePath, testStringResourceInstance);

        WriteCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(contentFormat, nodes, null));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 14));
        assertEquals(utcOffset, readResponse.getContent());

        readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, resourceInstancePath, null));
        assertEquals(testStringResourceInstance, readResponse.getContent());
    }

    @Test
    public void can_add_resource_instances() throws InterruptedException {
        // Prepare node
        LwM2mPath resourceInstancePath = new LwM2mPath(IntegrationTestHelper.TEST_OBJECT_ID, 0,
                IntegrationTestHelper.STRING_RESOURCE_INSTANCE_ID, 100);
        LwM2mResourceInstance testStringResourceInstance = LwM2mResourceInstance
                .newStringInstance(resourceInstancePath.getResourceInstanceId(), "test_string_instance");
        Map<LwM2mPath, LwM2mNode> nodes = new HashMap<>();
        nodes.put(resourceInstancePath, testStringResourceInstance);

        // Write it
        WriteCompositeResponse response = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(contentFormat, nodes, null));

        // verify result
        assertEquals(ResponseCode.CHANGED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));

        // read resource to check the value changed
        ReadResponse readResponse = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(contentFormat, resourceInstancePath.toResourcePath(), null));
        LwM2mMultipleResource multiResource = (LwM2mMultipleResource) readResponse.getContent();
        assertEquals(3, multiResource.getInstances().size());
        assertEquals(testStringResourceInstance,
                multiResource.getInstance(resourceInstancePath.getResourceInstanceId()));
    }

    @Test
    public void can_observe_instance_with_composite_write() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device instance
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        System.out.println(observeResponse.getContent());

        // write device timezone
        LwM2mResource utcOffset = LwM2mSingleResource.newStringResource(14, "+11");
        LwM2mResource timeZone = LwM2mSingleResource.newStringResource(15, "Moon");
        Map<String, LwM2mNode> nodes = new HashMap<>();
        nodes.put("/3/0/14", utcOffset);
        nodes.put("/3/0/15", timeZone);

        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(ContentFormat.SENML_CBOR, nodes));

        // verify result both resource must have new value
        listener.waitForNotification(1000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertTrue(listener.getResponse().getContent() instanceof LwM2mObjectInstance);
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mObjectInstance instance = (LwM2mObjectInstance) listener.getResponse().getContent();
        assertEquals(utcOffset, instance.getResource(14));
        assertEquals(timeZone, instance.getResource(15));

        // Ensure we received only one notification.
        Thread.sleep(1000);// wait 1 second more to catch more notification ?
        assertEquals(1, listener.getNotificationCount());
    }
}
