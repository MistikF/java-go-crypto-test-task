package com.example.cryptojava.crypto;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

@Service
public class Pkcs7Service {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final KeyStoreManager keyStoreManager;

    public Pkcs7Service(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    public byte[] sign(byte[] data, boolean detached) throws Exception {
        X509Certificate certificate = keyStoreManager.getCertificate();

        ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyStoreManager.getPrivateKey());

        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        generator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build())
                        .build(contentSigner, certificate));
        generator.addCertificates(new JcaCertStore(Collections.singletonList(certificate)));

        CMSTypedData content = new CMSProcessableByteArray(data);
        CMSSignedData signed = generator.generate(content, !detached);
        return signed.getEncoded();
    }

    public VerificationResult verify(byte[] signature, byte[] originalData, boolean detached) throws Exception {
        CMSSignedData signedData = detached
                ? new CMSSignedData(new CMSProcessableByteArray(originalData), signature)
                : new CMSSignedData(signature);

        Store<X509CertificateHolder> certificates = signedData.getCertificates();
        SignerInformationStore signers = signedData.getSignerInfos();

        for (SignerInformation signer : signers.getSigners()) {
            X509Certificate signerCertificate = matchCertificate(certificates, signer);
            if (signerCertificate == null) {
                continue;
            }
            try {
                boolean valid = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(signerCertificate));
                if (valid) {
                    return new VerificationResult(true, signerCertificate.getSubjectX500Principal().getName());
                }
            } catch (CMSException ignored) {
                return new VerificationResult(false, null);
            }
        }
        return new VerificationResult(false, null);
    }

    private X509Certificate matchCertificate(Store<X509CertificateHolder> certificates, SignerInformation signer)
            throws CMSException, java.security.cert.CertificateException {
        Collection<X509CertificateHolder> matches = certificates.getMatches(signer.getSID());
        if (matches.isEmpty()) {
            return null;
        }
        X509CertificateHolder holder = matches.iterator().next();
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(holder);
    }

    public static final class VerificationResult {
        private final boolean valid;
        private final String signer;

        public VerificationResult(boolean valid, String signer) {
            this.valid = valid;
            this.signer = signer;
        }

        public boolean isValid() {
            return valid;
        }

        public String getSigner() {
            return signer;
        }
    }
}
