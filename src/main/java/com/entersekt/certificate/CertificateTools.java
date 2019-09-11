package com.entersekt.certificate;

public class CertificateTools {

	public static void main(String[] args) throws Exception {
		CertificateService certificateService = new CertificateServiceBcImpl();
		certificateService.createRootCACert();
		certificateService.initialiseKeyStoreForSigner("Koosie");
		certificateService.initialiseKeyStoreForSigner("Alice");
		certificateService.initialiseKeyStoreForSigner("Bob");

		final String csr = certificateService.generateCSR("Carol", "Carol", "Next Door", "Wonderland");

		System.out.println(csr);

		final String pemSignedCert = certificateService.certificateSigningResponse(csr);

		System.out.println(pemSignedCert);

		String name = certificateService.registerSignedCert(pemSignedCert);

		System.out.println(name);

	}
}
