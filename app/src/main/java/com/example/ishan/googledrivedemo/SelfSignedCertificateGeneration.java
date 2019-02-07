package com.example.ishan.googledrivedemo;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class SelfSignedCertificateGeneration {

    public static Certificate selfSign(KeyPair keyPair, String subjectDN)
            throws OperatorCreationException, CertificateException
    {
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);

        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        X500Name dnName = new X500Name(subjectDN);

        // Using the current timestamp as the certificate serial number
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        // 1 Yr validity
        calendar.add(Calendar.YEAR, 1);

        Date endDate = calendar.getTime();

        // Use appropriate signature algorithm based on your keyPair algorithm.
        String signatureAlgorithm = "SHA256WithRSA";

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair
                .getPublic().getEncoded());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName,
                certSerialNumber, startDate, endDate, dnName, subjectPublicKeyInfo);

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(
                bcProvider).build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        Certificate selfSignedCert = new JcaX509CertificateConverter()
                .getCertificate(certificateHolder);

        return selfSignedCert;
    }

}
