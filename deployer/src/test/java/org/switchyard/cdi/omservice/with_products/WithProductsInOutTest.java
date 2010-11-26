/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.switchyard.cdi.omservice.with_products;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.milyn.io.StreamUtils;
import org.switchyard.*;
import org.switchyard.cdi.AbstractCDITest;
import org.switchyard.cdi.ServiceProxyHandler;
import org.switchyard.cdi.omservice.model.OrderRequest;
import org.switchyard.cdi.omservice.model.OrderResponse;
import org.switchyard.cdi.transform.PayloadSpec;
import org.switchyard.internal.ServiceDomains;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WithProductsInOutTest extends AbstractCDITest {

    @Test
    public void test_Java() {
        ServiceDomain domain = ServiceDomains.getDomain();

        // Consume the OM model...
        MockHandler responseConsumer = new MockHandler();
        Exchange exchange = domain.createExchange(new QName("WithProductsOrderManagementService"), ExchangePattern.IN_OUT, responseConsumer);

        ServiceProxyHandler.setOperationName(exchange, "createOrder");
        
        Message inMessage = MessageBuilder.newInstance().buildMessage();
        inMessage.setContent(new OrderRequest("D123", "ABCD"));

        exchange.send(inMessage);

        // wait, since this is async
        responseConsumer.waitForMessage();

        OrderResponse response = (OrderResponse) responseConsumer._messages.poll().getMessage().getContent();
        Assert.assertEquals("D123", response.orderId);
        Assert.assertEquals("ABCD", response.product.id);
        Assert.assertEquals("MacBook Pro", response.product.name);
    }

    @Test
    public void test_SOAP() throws IOException, SAXException {
        ServiceDomain domain = ServiceDomains.getDomain();

        // Consume the OM model...
        MockHandler responseConsumer = new MockHandler();
        Exchange exchange = domain.createExchange(new QName("WithProductsOrderManagementService"), ExchangePattern.IN_OUT, responseConsumer);

        ServiceProxyHandler.setOperationName(exchange, "createOrder");
        PayloadSpec.setInPayloadSpec(exchange, "application/soap+xml", "urn:createOrderRequest:v1");
        PayloadSpec.setOutPayloadSpec(exchange, "application/soap+xml", "urn:createOrderResponse:v1");

        Message inMessage = MessageBuilder.newInstance().buildMessage();
        String request = StreamUtils.readStreamAsString(getClass().getResourceAsStream("createOrderRequest.xml"));
        inMessage.setContent(request);

        exchange.send(inMessage);

        // wait, since this is async
        responseConsumer.waitForMessage();

        String response = (String) responseConsumer._messages.poll().getMessage().getContent();
        System.out.println("\nRequest:");
        System.out.println(request);
        System.out.println("\nResponse:");
        System.out.println(response);

        XMLUnit.setIgnoreWhitespace( true );
        XMLAssert.assertXMLEqual(new InputStreamReader(getClass().getResourceAsStream("expected_createOrderResponse.xml")), new StringReader(response));
    }

}
