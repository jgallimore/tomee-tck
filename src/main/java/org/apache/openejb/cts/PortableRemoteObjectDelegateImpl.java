/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openejb.cts;

import javax.rmi.CORBA.PortableRemoteObjectDelegate;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;

/**
 * The TCK's TSNamingContext narrows every lookup with javax.rmi.PortableRemoteObject, which needs
 * a delegate. GlassFish's delegate is part of its ORB. TomEE's remote EJB proxies already implement
 * the business interface, so narrowing only has to check the type.
 *
 * Selected in the app client with -Djavax.rmi.CORBA.PortableRemoteObjectClass (testsuite.properties).
 */
public class PortableRemoteObjectDelegateImpl implements PortableRemoteObjectDelegate {

    @Override
    public Object narrow(final Object narrowFrom, final Class narrowTo) throws ClassCastException {
        if (narrowFrom == null || narrowTo.isInstance(narrowFrom)) {
            return narrowFrom;
        }
        throw new ClassCastException(narrowFrom.getClass().getName() + " is not a " + narrowTo.getName());
    }

    @Override
    public void exportObject(final Remote obj) {
        // Nothing to export: the client has no ORB
    }

    @Override
    public void unexportObject(final Remote obj) throws NoSuchObjectException {
        throw new NoSuchObjectException("Not exported: " + obj);
    }

    @Override
    public Remote toStub(final Remote obj) throws NoSuchObjectException {
        return obj;
    }

    @Override
    public void connect(final Remote target, final Remote source) {
        // Nothing to connect: the client has no ORB
    }
}
