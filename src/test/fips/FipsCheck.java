/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

/**
 * Run by JavaTestCommand with -o fips, on the server's JDK and with the server's FIPS options, as a
 * single-file program (java FipsCheck.java). Exits non-zero unless the JVM is in Semeru's FIPS mode,
 * so a JDK or option problem can't quietly turn a FIPS run into a normal one.
 * <p>
 * It also prints whether SHA-1 is available, which TomEE replaces with a built-in implementation for the WebSocket
 * handshake and EclipseLink weaving when it isn't, and whether the JDK's own serialization can still compute a
 * default serialVersionUID (SHA-1 too), which stateful bean passivation and session persistence rely on.
 */
public class FipsCheck {

    public static void main(final String[] args) {
        System.out.println("FIPS check: " + System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name")
                + " " + System.getProperty("java.runtime.version"));

        final Provider[] providers = Security.getProviders();
        for (final Provider provider : providers) {
            System.out.println("  provider: " + provider.getName() + " " + provider.getVersionStr());
        }

        System.out.println("  MD5 MessageDigest: " + available("MD5"));
        System.out.println("  SHA-1 MessageDigest: " + available("SHA-1"));
        System.out.println("  default serialVersionUID: " + defaultSerialVersionUid());

        if (providers.length == 0 || !providers[0].getName().startsWith("OpenJCEPlusFIPS")) {
            System.out.println("FIPS check failed: OpenJCEPlusFIPS isn't the first provider");
            System.exit(1);
        }

        System.out.println("FIPS check passed");
    }

    private static String defaultSerialVersionUid() {
        try {
            return Long.toString(ObjectStreamClass.lookup(NoSerialVersionUid.class).getSerialVersionUID());
        } catch (final RuntimeException e) {
            return "failed, " + e;
        }
    }

    private static String available(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm).getProvider().getName();
        } catch (final NoSuchAlgorithmException e) {
            return "not available";
        }
    }

    private static class NoSerialVersionUid implements Serializable {
    }
}
