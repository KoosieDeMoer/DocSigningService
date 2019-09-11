package com.entersekt.certificate;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.input.ReaderInputStream;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
//import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs10.PKCS10;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import com.entersekt.configuration.ConfigurationService;
import com.entersekt.hub.GuiceBindingsModule;
import com.entersekt.json.JsonSerialisationService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class CertificateServiceBcImpl implements CertificateService {

	private static final Logger log = LoggerFactory.getLogger(CertificateServiceBcImpl.class);

	protected static final Injector injector = Guice.createInjector(new GuiceBindingsModule());
	JsonSerialisationService jsonSerialisationService = injector.getInstance(JsonSerialisationService.class);
	public String middleAlias;
	protected static ConfigurationService configService = injector.getInstance(ConfigurationService.class);

	private KeyStore keyStore;
	private Map<String, PrivateKey> privateKeys = new HashMap<>();
	private Map<String, PublicKey> publicKeys = new HashMap<>();

	public CertificateServiceBcImpl() throws Exception {
		// Security.insertProviderAt(new BouncyCastleProvider(), 1);
		// keyStore = KeyStore.getInstance("BKS");
		keyStore = KeyStore.getInstance("PKCS12");

		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(KEY_STORE_FILENAME);
		} catch (FileNotFoundException e) {
			// this is fine
			log.info("Creating the keystore for first time");
		}
		keyStore.load(fileInputStream, "secret".toCharArray());

	}

	// use with care - this replaces the 'CA' certificate for demos
	@Override
	public void createRootCACert() throws Exception {

		CertAndKeyGen keyGen = new CertAndKeyGen("RSA", CertificateService.SIGNATURE_ALGORITHM, null);
		keyGen.generate(1024);
		PrivateKey rootPrivateKey = keyGen.getPrivateKey();

		X509Certificate rootCertificate = keyGen.getSelfCertificate(new X500Name("CN=" + ROOT_ALIAS),
				(long) 3650 * 24 * 60 * 60);

		X509Certificate[] chain = new X509Certificate[1];
		chain[0] = rootCertificate;
		keyStore.setKeyEntry(ROOT_ALIAS, rootPrivateKey, CertificateService.KEYSTORE_PASSWORD, chain);
		putKeyStore();
	}

	@Override
	public void initialiseKeyStoreForSigner(String middleAlias) throws Exception {

		PrivateKey rootPrivateKey = (PrivateKey) keyStore.getKey(ROOT_ALIAS, CertificateService.KEYSTORE_PASSWORD);

		java.security.cert.Certificate[] rootChain = keyStore.getCertificateChain(ROOT_ALIAS);

		// Generate intermediate certificate
		CertAndKeyGen keyGen1 = new CertAndKeyGen("RSA", CertificateService.SIGNATURE_ALGORITHM, null);
		keyGen1.generate(1024);
		PrivateKey middlePrivateKey = keyGen1.getPrivateKey();

		X509Certificate middleCertificate = keyGen1.getSelfCertificate(new X500Name("CN=" + middleAlias),
				(long) 365 * 24 * 60 * 60);

		middleCertificate = createSignedCertificate(middleAlias, middleCertificate, (X509Certificate) rootChain[0],
				rootPrivateKey, true);

		X509Certificate[] chain = new X509Certificate[2];
		chain[0] = middleCertificate;
		chain[1] = (X509Certificate) rootChain[0];
		keyStore.setKeyEntry(middleAlias, middlePrivateKey, CertificateService.KEYSTORE_PASSWORD, chain);
		putKeyStore();
		this.middleAlias = middleAlias;
	}

	private static X509Certificate createSignedCertificate(String signerAlias, X509Certificate cetrificate,
			X509Certificate issuerCertificate, PrivateKey issuerPrivateKey, boolean addBasicConstraints)
			throws Exception {
		Principal issuer = issuerCertificate.getSubjectDN();
		String issuerSigAlg = issuerCertificate.getSigAlgName();

		byte[] inCertBytes = cetrificate.getTBSCertificate();
		X509CertInfo info = new X509CertInfo(inCertBytes);
		info.set(X509CertInfo.ISSUER, (X500Name) issuer);

		// No need to add the BasicContraint for leaf cert
		if (addBasicConstraints) {
			CertificateExtensions exts = new CertificateExtensions();
			BasicConstraintsExtension bce = new BasicConstraintsExtension(true, -1);
			exts.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(false, bce.getExtensionValue()));
			info.set(X509CertInfo.EXTENSIONS, exts);
		}

		X509CertImpl outCert = new X509CertImpl(info);
		outCert.sign(issuerPrivateKey, issuerSigAlg);

		return outCert;
	}

	@Override
	public String generateCSR(String alias, String CN, String O, String C) throws Exception {
		try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(outStream)) {

			CertAndKeyGen keyGen1 = new CertAndKeyGen("RSA", CertificateService.SIGNATURE_ALGORITHM, null);
			keyGen1.generate(1024);

			privateKeys.put(alias, keyGen1.getPrivateKey());
			publicKeys.put(alias, keyGen1.getPublicKey());

			PKCS10 certRequest = keyGen1.getCertRequest(new X500Name("CN=" + CN + ", O=" + O + ", C=" + C));

			certRequest.print(printStream);

			byte[] csrBytes = outStream.toByteArray();

			return new String(csrBytes);
		}
	}

	@Override
	public String certificateSigningResponse(String csr) throws Exception {

		X509Certificate signedCert = signCert(csr);
		return certToPem(signedCert);

	}

	private String certToPem(X509Certificate x509Cert) throws Exception {

		final StringWriter stringWriter = new StringWriter();
		JcaPEMWriter pemWrt = new JcaPEMWriter(stringWriter);
		pemWrt.writeObject(x509Cert);
		pemWrt.flush();
		pemWrt.close();
		return stringWriter.toString();
	}

	@Override
	public X509Certificate signCert(String csr) throws Exception {
		PrivateKey caPrivateKey = (PrivateKey) keyStore.getKey(middleAlias, CertificateService.KEYSTORE_PASSWORD);

		java.security.cert.Certificate[] rootChain = keyStore.getCertificateChain(middleAlias);

		try (PemReader pemReader = new PemReader(new StringReader(csr))) {

			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());

			final Calendar notBefore = Calendar.getInstance();
			notBefore.add(Calendar.MONTH, -3);
			final Calendar notAfter = Calendar.getInstance();
			notAfter.add(Calendar.YEAR, 1);

			return signCertificateRequest((X509Certificate) rootChain[0], caPrivateKey, req, notBefore.getTime(),
					notAfter.getTime());
		}
	}

	@Override
	public String registerSignedCert(String pem) throws Exception {
		X509Certificate cert = convertToX509Certificate(pem);
		String alias = extractAlias(cert);
		PrivateKey principalPrivateKey = privateKeys.get(alias);

		java.security.cert.Certificate[] middleChain = keyStore.getCertificateChain(middleAlias);

		X509Certificate[] chain = new X509Certificate[3];
		chain[0] = cert;
		chain[1] = (X509Certificate) middleChain[0];
		chain[2] = (X509Certificate) middleChain[1];

		keyStore.setKeyEntry(alias, principalPrivateKey, CertificateService.KEYSTORE_PASSWORD, chain);
		putKeyStore();

		return alias;

	}

	@Override
	public String extractAlias(X509Certificate cert) throws CertificateEncodingException {
		return new JcaX509CertificateHolder(cert).getSubject().getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
	}

	@Override
	public void putKeyStore() throws Exception {
		FileOutputStream fileOutputStream = new FileOutputStream(KEY_STORE_FILENAME);
		keyStore.store(fileOutputStream, CertificateService.KEYSTORE_PASSWORD);
		fileOutputStream.flush();
		fileOutputStream.close();
	}

	@Override
	public KeyStore getKeyStore() {
		return keyStore;
	}

	private X509Certificate signCertificateRequest(X509Certificate caCert, PrivateKey caPrivateKey,
			PKCS10CertificationRequest csr, Date notBefore, Date notAfter) throws NoSuchAlgorithmException,
			InvalidKeyException, CertificateException, CertIOException, OperatorCreationException {

		JcaPKCS10CertificationRequest jcaRequest = new JcaPKCS10CertificationRequest(csr);
		X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(caCert, BigInteger.valueOf(System
				.currentTimeMillis()), notBefore, notAfter, jcaRequest.getSubject(), jcaRequest.getPublicKey());

		ContentSigner signer = new JcaContentSignerBuilder(CertificateService.SIGNATURE_ALGORITHM).build(caPrivateKey);
		return new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(signer));
	}

	private X509Certificate convertToX509Certificate(String pem) throws Exception {
		CertificateFactory certificateFactory = new CertificateFactory();
		return (X509Certificate) certificateFactory.engineGenerateCertificate(new ReaderInputStream(new StringReader(
				pem), "UTF-8"));
	}

}
