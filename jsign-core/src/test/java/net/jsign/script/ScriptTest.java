/**
 * Copyright 2019 Emmanuel Bourg
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

package net.jsign.script;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSSignedData;
import org.junit.Test;

import net.jsign.AuthenticodeSigner;
import net.jsign.DigestAlgorithm;

import static org.junit.Assert.*;

public abstract class ScriptTest {

    protected static String PRIVATE_KEY_PASSWORD = "password";
    protected static String ALIAS = "test";

    protected abstract String getFileExtension();

    protected abstract SignableScript createScript();

    protected KeyStore getKeyStore() throws Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(new FileInputStream("target/test-classes/keystores/keystore.jks"), "password".toCharArray());
        return keystore;
    }

    private AuthenticodeSigner getSigner() throws Exception {
        return new AuthenticodeSigner(getKeyStore(), ALIAS, PRIVATE_KEY_PASSWORD)
                        .withDigestAlgorithm(DigestAlgorithm.SHA1)
                        .withProgramName("Hello World")
                        .withProgramURL("http://example.com");
    }

    @Test
    public void testRemoveSignature() throws Exception {
        File sourceFile = new File("target/test-classes/hello-world." + getFileExtension());
        String content = FileUtils.readFileToString(sourceFile, "ISO-8859-1");

        SignableScript script = createScript();
        script.setContent(content);
        getSigner().sign(script);
        
        List<CMSSignedData> signatures = script.getSignatures();
        assertNotNull(signatures);
        assertEquals(1, signatures.size());
        CMSSignedData signature = signatures.get(0);
        assertNotNull(signature);
        
        assertEquals("script with the signature removed", content, script.getContentWithoutSignatureBlock());
    }
}
