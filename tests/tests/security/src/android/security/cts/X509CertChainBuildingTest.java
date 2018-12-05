/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security.cts;

import android.content.res.AssetManager;
import android.net.http.X509TrustManagerExtensions;
import android.platform.test.annotations.SecurityTest;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Test that all {@link X509TrustManager} build the correct certificate chain during
 * {@link X509TrustManagerExtensions#checkServerTrusted(X509Certificate[], String, String)} when
 * multiple possible certificate paths exist.
 */
@SecurityTest
public class X509CertChainBuildingTest extends AndroidTestCase {
    private static final String CERT_ASSET_DIR = "path_building";

    /* Certificates for tests. These are initialized in setUp.
     * All certificates use 2048 bit RSA keys and SHA-256 digests unless otherwise specified.
     * First certificate graph:
     *
     * rootA: A root CA
     * rootASha1: rootA but with SHA-1 as the digest algorithm.
     * rootB: Another root CA
     * leaf1: Certificate issued by rootA
     * rootAtoB: rootA cross signed by rootB
     * rootBtoA: rootB cross signed by rootA
     *
     *   [A] <-------> [B]
     *    |
     *    v
     * [leaf1]
     * Second certificate graph:
     *
     * intermediateA: Intermediate I issued by rootA
     * intermediateB: Intermediate I issued by rootB
     * leaf2: Leaf issued by I
     *
     * [A]   [B]
     *    \ /
     *    [I]
     *     |
     *     v
     *  [leaf2]
     *
     *  These can be generated by running cts/tools/utils/certificates.py
     */
    private X509Certificate rootA;
    private X509Certificate rootASha1;
    private X509Certificate rootB;
    private X509Certificate rootAtoB;
    private X509Certificate rootBtoA;
    private X509Certificate leaf1;
    private X509Certificate leaf2;
    private X509Certificate intermediateA;
    private X509Certificate intermediateB;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        rootA = loadCertificate("a.pem");
        rootASha1 = loadCertificate("a_sha1.pem");
        rootB = loadCertificate("b.pem");
        leaf1 = loadCertificate("leaf1.pem");
        leaf2 = loadCertificate("leaf2.pem");
        rootAtoB = loadCertificate("a_to_b.pem");
        rootBtoA = loadCertificate("b_to_a.pem");
        intermediateA = loadCertificate("intermediate_a.pem");
        intermediateB = loadCertificate("intermediate_b.pem");
    }

    public void testBasicChain() throws Exception {
        assertExactPath(new X509Certificate[] {leaf1, rootA},
                new X509Certificate[] {leaf1},
                new X509Certificate[] {rootA});
    }
    public void testCrossSign() throws Exception {
        // First try a path that doesn't have the cross signed A to B certificate.
        assertNoPath(new X509Certificate[] {leaf1, rootA}, new X509Certificate[] {rootB});
        // Now try with one valid chain (leaf1 -> rootAtoB -> rootB).
        assertExactPath(new X509Certificate[] {leaf1, rootAtoB, rootB},
                new X509Certificate[] {leaf1, rootAtoB},
                new X509Certificate[] {rootB});
        // Now try with two possible chains present only one of which chains to a trusted root.
        assertExactPath(new X509Certificate[] {leaf1, rootAtoB, rootB},
                new X509Certificate[] {leaf1, rootA, rootAtoB},
                new X509Certificate[] {rootB});
    }

    public void testUntrustedLoop() throws Exception {
        // Verify that providing all the certificates doesn't cause the path building to get stuck
        // in the loop caused by the cross signed certificates.
        assertNoPath(new X509Certificate[] {leaf1, rootAtoB, rootBtoA, rootA, rootB},
                new X509Certificate[] {});
    }

    public void testAvoidCrossSigned() throws Exception {
        // Check that leaf1 -> rootA is preferred over using the cross signed cert when both rootA
        // and rootB are trusted.
        assertExactPath(new X509Certificate[] {leaf1, rootA},
                new X509Certificate[] {leaf1, rootAtoB},
                new X509Certificate[] {rootA, rootB});
    }

    public void testSelfIssuedPreferred() throws Exception {
        // Check that when there are multiple possible trusted issuers that we prefer self-issued
        // certificates over bridge versions of the same certificate.
        assertExactPath(new X509Certificate[] {leaf1, rootA},
                new X509Certificate[] {leaf1, rootAtoB},
                new X509Certificate[] {rootA, rootAtoB, rootB});
    }

    public void testBridgeCrossing() throws Exception {
        // Check that when provided with leaf2, intermediateA, intermediateB, rootA that it builds
        // the leaf2 -> intermediateB -> B path.
        assertExactPath(new X509Certificate[] {leaf2, intermediateB, rootB},
                new X509Certificate[] {leaf2, intermediateA, rootA, intermediateB},
                new X509Certificate[] {rootB});
    }

    public void testDigestOrdering() throws Exception {
        // Check that leaf1 -> rootASha1 is valid
        assertExactPath(new X509Certificate[] {leaf1, rootASha1},
                new X509Certificate[] {leaf1},
                new X509Certificate[] {rootASha1});
        // Check that when a SHA-256 and SHA-1 are available the SHA-256 cert is used
        assertExactPath(new X509Certificate[] {leaf1, rootA},
                new X509Certificate[] {leaf1},
                new X509Certificate[] {rootASha1, rootA});
    }

    private X509Certificate loadCertificate(String file) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        AssetManager assetManager = getContext().getAssets();
        InputStream in = null;
        try {
            in = assetManager.open(new File(CERT_ASSET_DIR, file).toString());
            return (X509Certificate) certFactory.generateCertificate(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static X509TrustManager getTrustManager(KeyStore ks, Provider p) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", p);
        tmf.init(ks);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        fail("Unable to find X509TrustManager");
        return null;
    }

    /**
     * Asserts that all PKIX TrustManagerFactory providers build the expected certificate path or
     * throw a {@code CertificateException} if {@code expected} is {@code null}.
     */
    private static void assertExactPath(X509Certificate[] expected, X509Certificate[] bagOfCerts,
            X509Certificate[] roots) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null);
        int i = 0;
        for (X509Certificate root : roots) {
            ks.setEntry(String.valueOf(i++), new KeyStore.TrustedCertificateEntry(root), null);
        }
        Provider[] providers = Security.getProviders("TrustManagerFactory.PKIX");
        assertNotNull(providers);
        assertTrue("No providers found", providers.length != 0);
        for (Provider p : providers) {
            try {
                X509TrustManager tm = getTrustManager(ks, p);
                X509TrustManagerExtensions xtm = new X509TrustManagerExtensions(tm);
                List<X509Certificate> result;
                try {
                    result = xtm.checkServerTrusted(bagOfCerts, "RSA", null);
                } catch (CertificateException e) {
                    if (expected == null) {
                        // Exception expected.
                        continue;
                    }
                    throw e;
                }
                if (expected == null) {
                    fail("checkServerTrusted expected to fail, instead returned: " + result);
                }
                assertEquals(Arrays.asList(expected), result);
            } catch (Exception e) {
                throw new Exception("Failed with provider " + p, e);
            }
        }
    }

    private static void assertNoPath(X509Certificate[] bagOfCerts, X509Certificate[] roots)
            throws Exception {
        assertExactPath(null, bagOfCerts, roots);
    }

}
