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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

/**
 * Run by JavaTestCommand with -o fips, on the server's JDK and with the server's FIPS options, as a
 * single-file program (java FipsCheck.java). Exits non-zero unless the JVM is in Semeru's FIPS mode,
 * so a JDK or option problem can't quietly turn a FIPS run into a normal one.
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

        if (providers.length == 0 || !providers[0].getName().startsWith("OpenJCEPlusFIPS")) {
            System.out.println("FIPS check failed: OpenJCEPlusFIPS isn't the first provider");
            System.exit(1);
        }

        System.out.println("FIPS check passed");
    }

    private static String available(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm).getProvider().getName();
        } catch (final NoSuchAlgorithmException e) {
            return "not available";
        }
    }
}
