package com.entersekt.certificate;

import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public interface CertificateService {

	public static final String KEY_STORE_FILENAME = "keyStore.JKS";
	public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

	public static final char[] KEYSTORE_PASSWORD = "secret".toCharArray();
	public static final String ROOT_ALIAS = "CA";

	// use with care - this replaces the 'CA' certificate for demos
	void createRootCACert() throws Exception;

	void initialiseKeyStoreForSigner(String middleAlias) throws Exception;

	String generateCSR(String alias, String CN, String O, String C) throws Exception;

	String certificateSigningResponse(String csr) throws Exception;

	X509Certificate signCert(String csr) throws Exception;

	String registerSignedCert(String pem) throws Exception;

	String extractAlias(X509Certificate cert) throws CertificateEncodingException;

	KeyStore getKeyStore();

	void putKeyStore() throws Exception;

}
