package com.example.cryptojava.crypto;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class EnvelopeService {

    private final KeyStoreManager keyStoreManager;

    public EnvelopeService(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    public byte[] encrypt(byte[] data) throws Exception {
        CMSEnvelopedDataGenerator generator = new CMSEnvelopedDataGenerator();
        generator.addRecipientInfoGenerator(
                new JceKeyTransRecipientInfoGenerator(keyStoreManager.getCertificate())
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME));

        CMSEnvelopedData envelopedData = generator.generate(
                new CMSProcessableByteArray(data),
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build());

        return envelopedData.getEncoded();
    }

    public byte[] decrypt(byte[] envelope) throws Exception {
        CMSEnvelopedData envelopedData = new CMSEnvelopedData(envelope);
        Iterator<RecipientInformation> recipients = envelopedData.getRecipientInfos().getRecipients().iterator();
        if (!recipients.hasNext()) {
            throw new IllegalArgumentException("Envelope contains no recipients");
        }
        RecipientInformation recipient = recipients.next();
        return recipient.getContent(new JceKeyTransEnvelopedRecipient(keyStoreManager.getPrivateKey())
                .setProvider(BouncyCastleProvider.PROVIDER_NAME));
    }
}
