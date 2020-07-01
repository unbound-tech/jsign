/**
 * Copyright 2012 Emmanuel Bourg
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

package net.jsign;

import java.io.File;
import java.security.Permission;
import java.security.ProviderException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.handler.codec.http.HttpRequest;
import junit.framework.TestCase;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSSignedData;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import net.jsign.pe.PEFile;

public class PESignerCLITest extends TestCase {

    private JsignCLI cli;
    private File sourceFile = new File("target/test-classes/wineyes.exe");
    private File targetFile = new File("target/test-classes/wineyes-signed-with-cli.exe");
    
    private String keystore = "keystore.jks";
    private String alias    = "test";
    private String keypass  = "password";

    private static final long SOURCE_FILE_CRC32 = 0xA6A363D8L;

    protected void setUp() throws Exception {
        cli = new JsignCLI();
        
        // remove the files signed previously
        if (targetFile.exists()) {
            assertTrue("Unable to remove the previously signed file", targetFile.delete());
        }
        
        assertEquals("Source file CRC32", SOURCE_FILE_CRC32, FileUtils.checksumCRC32(sourceFile));
        Thread.sleep(100);
        FileUtils.copyFile(sourceFile, targetFile);
    }

    public void testPrintHelp() throws Exception {
        JsignCLI.main("--help");
    }

    public void testMissingKeyStore() throws Exception {
        try {
            cli.execute("" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testUnsupportedKeyStoreType() throws Exception  {
        try {
            cli.execute("--keystore=keystore.jks", "--storetype=ABC", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testKeyStoreNotFound() throws Exception  {
        try {
            cli.execute("--keystore=keystore2.jks", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testCorruptedKeyStore() throws Exception  {
        try {
            cli.execute("--keystore=" + targetFile, "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testMissingAlias() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testAliasNotFound() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "--alias=unknown", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testCertificateNotFound() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "--alias=foo", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testMissingFile() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "--alias=test", "--keypass=password");
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testFileNotFound() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "--alias=test", "--keypass=password", "wineyes-foo.exe");
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testCorruptedFile() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "--alias=test", "--keypass=password", "target/test-classes/keystore.jks");
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testConflictingAttributes() throws Exception  {
        try {
            cli.execute("--keystore=target/test-classes/keystore.jks", "--alias=test", "--keypass=password", "--keyfile=privatekey.pvk", "--certfile=jsign-test-certificate-full-chain.spc", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testMissingCertFile() throws Exception  {
        try {
            cli.execute("--keyfile=target/test-classes/privatekey.pvk", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testMissingKeyFile() throws Exception  {
        try {
            cli.execute("--certfile=target/test-classes/jsign-test-certificate-full-chain.spc", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testCertFileNotFound() throws Exception  {
        try {
            cli.execute("--certfile=target/test-classes/certificate2.spc", "--keyfile=target/test-classes/privatekey.pvk", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testKeyFileNotFound() throws Exception  {
        try {
            cli.execute("--certfile=target/test-classes/jsign-test-certificate-full-chain.spc", "--keyfile=target/test-classes/privatekey2.pvk", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testCorruptedCertFile() throws Exception  {
        try {
            cli.execute("--certfile=target/test-classes/privatekey.pvk", "--keyfile=target/test-classes/privatekey.pvk", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testCorruptedKeyFile() throws Exception  {
        try {
            cli.execute("--certfile=target/test-classes/jsign-test-certificate-full-chain.spc", "--keyfile=target/test-classes/jsign-test-certificate-full-chain.spc", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testUnsupportedDigestAlgorithm() throws Exception  {
        try {
            cli.execute("--alg=SHA-123", "--keystore=target/test-classes/keystore.jks", "--alias=test", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
        }
    }

    public void testSigning() throws Exception {
        cli.execute("--name=WinEyes", "--url=http://www.steelblue.com/WinEyes", "--alg=SHA-1", "--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile);

        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    public void testSigningPKCS12() throws Exception {
        cli.execute("--name=WinEyes", "--url=http://www.steelblue.com/WinEyes", "--alg=SHA-256", "--keystore=target/test-classes/keystore.p12", "--alias=test", "--storepass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    public void testSigningPVKSPC() throws Exception {
        cli.execute("--url=http://www.steelblue.com/WinEyes", "--certfile=target/test-classes/jsign-test-certificate-full-chain.spc", "--keyfile=target/test-classes/privatekey-encrypted.pvk", "--storepass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    public void testSigningPEM() throws Exception {
        cli.execute("--certfile=target/test-classes/jsign-test-certificate.pem", "--keyfile=target/test-classes/privatekey.pkcs8.pem", "--keypass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    public void testSigningEncryptedPEM() throws Exception {
        cli.execute("--certfile=target/test-classes/jsign-test-certificate.pem", "--keyfile=target/test-classes/privatekey-encrypted.pkcs1.pem", "--keypass=password", "" + targetFile);
        
        assertTrue("The file " + targetFile + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile));

        try (PEFile peFile = new PEFile(targetFile)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    public void testTimestampingAuthenticode() throws Exception {
        File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-authenticode.exe");
        FileUtils.copyFile(sourceFile, targetFile2);
        cli.execute("--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "--tsaurl=http://timestamp.comodoca.com/authenticode", "--tsmode=authenticode", "" + targetFile2);
        
        assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));

        try (PEFile peFile = new PEFile(targetFile2)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());
            
            CMSSignedData signature = signatures.get(0);
            
            assertNotNull(signature);
        }
    }

    public void testTimestampingRFC3161() throws Exception {
        File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-rfc3161.exe");
        FileUtils.copyFile(sourceFile, targetFile2);
        cli.execute("--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "--tsaurl=http://timestamp.comodoca.com/rfc3161", "--tsmode=rfc3161", "" + targetFile2);

        assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));

        try (PEFile peFile = new PEFile(targetFile2)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            CMSSignedData signature = signatures.get(0);

            assertNotNull(signature);
        }
    }

    public void testTimestampingWithProxyUnauthenticated() throws Exception {
        final AtomicBoolean proxyUsed = new AtomicBoolean(false);
        HttpProxyServer proxy = DefaultHttpProxyServer.bootstrap().withPort(12543)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        proxyUsed.set(true);
                        return super.filterRequest(originalRequest);
                    }
                })
                .start();
        
        try {
            File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-rfc3161-proxy-unauthenticated.exe");
            FileUtils.copyFile(sourceFile, targetFile2);
            cli.execute("--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass,
                        "--tsaurl=http://timestamp.comodoca.com/rfc3161", "--tsmode=rfc3161", "--tsretries=1", "--tsretrywait=1",
                        "--proxyUrl=localhost:" + proxy.getListenAddress().getPort(),
                        "" + targetFile2);
            
            assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));
            assertTrue("The proxy wasn't used", proxyUsed.get());
    
            try (PEFile peFile = new PEFile(targetFile2)) {
                List<CMSSignedData> signatures = peFile.getSignatures();
                assertNotNull(signatures);
                assertEquals(1, signatures.size());
    
                CMSSignedData signature = signatures.get(0);
    
                assertNotNull(signature);
            }
        } finally {
            proxy.stop();
        }
    }

    public void testTimestampingWithProxyAuthenticated() throws Exception {
        final AtomicBoolean proxyUsed = new AtomicBoolean(false);
        HttpProxyServer proxy = DefaultHttpProxyServer.bootstrap().withPort(12544)
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        proxyUsed.set(true);
                        return super.filterRequest(originalRequest);
                    }
                })
                .withProxyAuthenticator(new ProxyAuthenticator() {
                    @Override
                    public boolean authenticate(String username, String password) {
                        return "jsign".equals(username) && "jsign".equals(password);
                    }

                    @Override
                    public String getRealm() {
                        return "Jsign Tests";
                    }
                })
                .start();

        try {
            File targetFile2 = new File("target/test-classes/wineyes-timestamped-with-cli-rfc3161-proxy-authenticated.exe");
            FileUtils.copyFile(sourceFile, targetFile2);
            cli.execute("--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass,
                        "--tsaurl=http://timestamp.comodoca.com/rfc3161", "--tsmode=rfc3161", "--tsretries=1", "--tsretrywait=1",
                        "--proxyUrl=http://localhost:" + proxy.getListenAddress().getPort(),
                        "--proxyUser=jsign",
                        "--proxyPass=jsign",
                        "" + targetFile2);
            
            assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));
            assertTrue("The proxy wasn't used", proxyUsed.get());
    
            try (PEFile peFile = new PEFile(targetFile2)) {
                List<CMSSignedData> signatures = peFile.getSignatures();
                assertNotNull(signatures);
                assertEquals(1, signatures.size());
    
                CMSSignedData signature = signatures.get(0);
    
                assertNotNull(signature);
            }
        } finally {
            proxy.stop();
        }
    }

    public void testReplaceSignature() throws Exception {
        File targetFile2 = new File("target/test-classes/wineyes-re-signed.exe");
        FileUtils.copyFile(sourceFile, targetFile2);
        cli.execute("--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "" + targetFile2);
        
        assertTrue("The file " + targetFile2 + " wasn't changed", SOURCE_FILE_CRC32 != FileUtils.checksumCRC32(targetFile2));
        
        cli.execute("--keystore=target/test-classes/" + keystore, "--alias=" + alias, "--keypass=" + keypass, "--alg=SHA-512", "--replace", "" + targetFile2);
        
        try (PEFile peFile = new PEFile(targetFile2)) {
            List<CMSSignedData> signatures = peFile.getSignatures();
            assertNotNull(signatures);
            assertEquals(1, signatures.size());

            assertNotNull(signatures.get(0));
            
            assertEquals("Digest algorithm", DigestAlgorithm.SHA512.oid, signatures.get(0).getDigestAlgorithmIDs().iterator().next().getAlgorithm());
        }
    }

    public void testExitOnError() {
        NoExitSecurityManager manager = new NoExitSecurityManager();
        System.setSecurityManager(manager);

        try {
            JsignCLI.main("foo.exe");
            fail("VM not terminated");
        } catch (SecurityException e) {
            // expected
            assertEquals("Exit code", Integer.valueOf(1), manager.getStatus());
        } finally {
            System.setSecurityManager(null);
        }
    }

    private static class NoExitSecurityManager extends SecurityManager {
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void checkPermission(Permission perm) { }
        
        public void checkPermission(Permission perm, Object context) { }

        public void checkExit(int status) {
            this.status = status;
            throw new SecurityException("Exit disabled");
        }
    }

    public void testUnknownOption() throws Exception {
        try {
            cli.execute("--jsign");
            fail("No exception thrown");
        } catch (ParseException e) {
            // expected
        }
    }

    public void testUnknownPKCS11Provider() throws Exception {
        try {
            cli.execute("--storetype=PKCS11", "--keystore=SunPKCS11-jsigntest", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            assertEquals("exception message", "Security provider SunPKCS11-jsigntest not found", e.getMessage());
        }
    }

    public void testMissingPKCS11Configuration() throws Exception {
        try {
            cli.execute("--storetype=PKCS11", "--keystore=jsigntest.cfg", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            assertEquals("keystore option should either refer to the SunPKCS11 configuration file or to the name of the provider configured in jre/lib/security/java.security", e.getMessage());
        }
    }

    public void testBrokenPKCS11Configuration() throws Exception {
        try {
            cli.execute("--storetype=PKCS11", "--keystore=pom.xml", "--keypass=password", "" + targetFile);
            fail("No exception thrown");
        } catch (SignerException e) {
            // expected
            assertTrue(e.getCause().getCause() instanceof ProviderException);
        }
    }
}
