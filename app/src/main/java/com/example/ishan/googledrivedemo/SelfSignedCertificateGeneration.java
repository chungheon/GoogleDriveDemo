package com.example.ishan.googledrivedemo;

import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

public class SelfSignedCertificateGeneration {

    public X509Certificate selfSignedCertificate(KeyPair keyPair, String serialNumber, String hostName)
            throws GeneralSecurityException {

        Date from = new Date();
        Date to = new Date(from.getTime() + 365 * 86400000l);

        X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
        X500Principal issuer = new X500Principal("CN=" + hostName);
        X500Principal subject = new X500Principal("CN=" + hostName);
        generator.setSerialNumber(new BigInteger(serialNumber));
        generator.setIssuerDN(issuer);
        generator.setNotBefore(from);
        generator.setNotAfter(to);
        generator.setSubjectDN(subject);
        generator.setPublicKey(keyPair.getPublic());
        generator.setSignatureAlgorithm("SHA256WithRSAEncryption");
        return generator.generateX509Certificate(keyPair.getPrivate(), "BC");
    }

}
