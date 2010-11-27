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

package org.switchyard.cdi.omservice.model.transforms;

import org.milyn.Smooks;
import org.milyn.payload.JavaResult;
import org.milyn.payload.JavaSource;
import org.milyn.payload.StringSource;
import org.switchyard.cdi.omservice.model.OrderRequest;
import org.switchyard.cdi.omservice.model.OrderResponse;
import org.switchyard.cdi.transform.From;
import org.switchyard.cdi.transform.To;

import java.io.*;

import org.switchyard.cdi.transform.Transformer;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamResult;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@Transformer
public class OrderModelTransforms {

    private Smooks readSOAP_V1;
    private Smooks writeSOAP_V1;

    public OrderModelTransforms() throws IOException, SAXException {
        readSOAP_V1 = new Smooks(getClass().getResourceAsStream("createOrderRequest_v1_read.xml"));
        writeSOAP_V1 = new Smooks(getClass().getResourceAsStream("createOrderResponse_v1_write.xml"));
    }

    public OrderRequest readSOAP_V1(@From("urn:createOrderRequest:v1:soap") String inXML) {

        JavaResult javaResult = new JavaResult();
        readSOAP_V1.filterSource(new StringSource(inXML), javaResult);
        return javaResult.getBean(OrderRequest.class);
    }

    public void writeSOAP_V1(@From OrderResponse orderResponse, @To("urn:createOrderResponse:v1:soap") Writer outXML) {

        JavaSource source = new JavaSource(orderResponse);
        source.setEventStreamRequired(false);
        writeSOAP_V1.filterSource(source, new StreamResult(outXML));
    }
}
