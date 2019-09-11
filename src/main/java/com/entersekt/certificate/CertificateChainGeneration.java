package com.entersekt.certificate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class CertificateChainGeneration {

	private static final Logger log = LoggerFactory.getLogger(CertificateChainGeneration.class);

	public static final String SIGNER_ALIAS = "Lara Croft";
	public static final String KEY_STORE_FILENAME = "keyStore.JKS";
	public static final char[] KEYSTORE_PASSWORD = "secret".toCharArray();

	private static final String MIDDLE_ALIAS = "Chain Ltd";

	private KeyStore keyStore;

	public static void main(String[] args) throws Exception {

		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, KEYSTORE_PASSWORD);

		final CertificateChainGeneration certificateChainGeneration = new CertificateChainGeneration(keyStore);

		certificateChainGeneration.createRootCACert();

		certificateChainGeneration.initialiseKeyStoreForSigner();

		final String testCert = certificateChainGeneration.createCertFor("test", SIGNER_ALIAS, "Standard Bank", "ZA");

		System.out.println(testCert);
	}

	private void createRootCACert() throws Exception {

		CertAndKeyGen keyGen = new CertAndKeyGen("RSA", CertificateService.SIGNATURE_ALGORITHM, null);
		keyGen.generate(1024);
		PrivateKey rootPrivateKey = keyGen.getPrivateKey();

		X509Certificate rootCertificate = keyGen.getSelfCertificate(new X500Name("CN=" + CertificateService.ROOT_ALIAS),
				(long) 365 * 24 * 60 * 60);

		X509Certificate[] chain = new X509Certificate[1];
		chain[0] = rootCertificate;
		keyStore.setKeyEntry(CertificateService.ROOT_ALIAS, rootPrivateKey, CertificateService.KEYSTORE_PASSWORD, chain);
		putKeyStore();
	}

	private void initialiseKeyStoreForSigner() throws Exception {

		PrivateKey rootPrivateKey = (PrivateKey) keyStore.getKey(CertificateService.ROOT_ALIAS,
				CertificateService.KEYSTORE_PASSWORD);

		java.security.cert.Certificate[] rootChain = keyStore.getCertificateChain(CertificateService.ROOT_ALIAS);

		// Generate intermediate certificate
		CertAndKeyGen keyGen1 = new CertAndKeyGen("RSA", CertificateService.SIGNATURE_ALGORITHM, null);
		keyGen1.generate(1024);
		PrivateKey middlePrivateKey = keyGen1.getPrivateKey();

		X509Certificate middleCertificate = keyGen1.getSelfCertificate(new X500Name("CN=" + MIDDLE_ALIAS),
				(long) 365 * 24 * 60 * 60);

		middleCertificate = createSignedCertificate(MIDDLE_ALIAS, middleCertificate, (X509Certificate) rootChain[0],
				rootPrivateKey);

		X509Certificate[] chain = new X509Certificate[2];
		chain[0] = middleCertificate;
		chain[1] = (X509Certificate) rootChain[0];
		keyStore.setKeyEntry(MIDDLE_ALIAS, middlePrivateKey, CertificateService.KEYSTORE_PASSWORD, chain);
		putKeyStore();
		// System.out.println(Arrays.toString(chain));
	}

	public String createCertFor(String signerAlias, String commonName, String organisation, String country)
			throws Exception {

		PrivateKey middlePrivateKey = (PrivateKey) keyStore.getKey(MIDDLE_ALIAS, CertificateService.KEYSTORE_PASSWORD);

		java.security.cert.Certificate[] certChain = keyStore.getCertificateChain(MIDDLE_ALIAS);

		// Generate leaf certificate
		CertAndKeyGen keyGen2 = new CertAndKeyGen("RSA", CertificateService.SIGNATURE_ALGORITHM, null);
		keyGen2.generate(1024);

		PrivateKey topPrivateKey = keyGen2.getPrivateKey();

		X509Certificate topCertificate = keyGen2.getSelfCertificate(new X500Name("CN=" + commonName + ", O="
				+ organisation + ", C=" + country), (long) 365 * 24 * 60 * 60);

		topCertificate = createSignedCertificate(signerAlias, topCertificate, (X509Certificate) certChain[0],
				middlePrivateKey);

		X509Certificate[] chain = new X509Certificate[3];
		chain[0] = topCertificate;
		chain[1] = (X509Certificate) certChain[0];
		chain[2] = (X509Certificate) certChain[1];

		keyStore.setKeyEntry(signerAlias, topPrivateKey, CertificateService.KEYSTORE_PASSWORD, chain);
		putKeyStore();
		return topCertificate.toString();

	}

	public CertificateChainGeneration(KeyStore keyStore) throws KeyStoreException, IOException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException {
		this.keyStore = keyStore;
	}

	public static X509Certificate convertToX509Certificate(String pem) throws Exception {
		StringReader reader = new StringReader(pem);
		Object cert;
		try (PemReader pr = new PemReader(reader)) {
			final PemObject pemObject = pr.readPemObject();
			cert = new X509CertificateHolder(pemObject.getContent());
			return (X509Certificate) cert;
		} catch (Exception e) {
			throw e;
		}
	}

	public X509Certificate signCert(String csr) throws Exception {

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			X509Certificate signedCert = null;// sign(req, MIDDLE_CERT, MIDDLE_PRIVATE_KEY);
			return signedCert;
		} finally {
			pemReader.close();
		}
	}

	private static X509Certificate createSignedCertificate(String signerAlias, X509Certificate cetrificate,
			X509Certificate issuerCertificate, PrivateKey issuerPrivateKey) {
		try {
			Principal issuer = issuerCertificate.getSubjectDN();
			String issuerSigAlg = issuerCertificate.getSigAlgName();

			byte[] inCertBytes = cetrificate.getTBSCertificate();
			X509CertInfo info = new X509CertInfo(inCertBytes);
			info.set(X509CertInfo.ISSUER, (X500Name) issuer);

			// No need to add the BasicContraint for leaf cert
			if (!cetrificate.getSubjectDN().getName().equals("CN=" + signerAlias)) {
				CertificateExtensions exts = new CertificateExtensions();
				BasicConstraintsExtension bce = new BasicConstraintsExtension(true, -1);
				exts.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(false, bce.getExtensionValue()));
				info.set(X509CertInfo.EXTENSIONS, exts);
			}

			X509CertImpl outCert = new X509CertImpl(info);
			outCert.sign(issuerPrivateKey, issuerSigAlg);

			return outCert;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public void putKeyStore() throws Exception {
		FileOutputStream fileOutputStream = new FileOutputStream(KEY_STORE_FILENAME);
		keyStore.store(fileOutputStream, CertificateService.KEYSTORE_PASSWORD);
		fileOutputStream.flush();
		fileOutputStream.close();
	}

}