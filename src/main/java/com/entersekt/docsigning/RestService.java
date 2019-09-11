package com.entersekt.docsigning;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.pdfbox.examples.signature.CreateSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entersekt.certificate.CertificateService;
import com.entersekt.graphics.WriteImageType;
import com.entersekt.utils.AppUtils;

@Path("/DocSigner")
@Api(value = "/DocSigner")
public class RestService {

	private static final Logger log = LoggerFactory.getLogger(RestService.class);

	@GET
	@Path("createSigner")
	@ApiOperation(value = "Creates a signer with valid unsigned cert - returns a csr")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Read successful"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response createSigner(
			@ApiParam(value = "Uniquely identifies the key", required = true) @QueryParam("alias") String alias,
			@ApiParam(value = "X500.CommonName") @QueryParam("CN") String cn,
			@ApiParam(value = "X500.Organisation", required = true) @QueryParam("O") String o,
			@ApiParam(value = "X500.Country", required = true) @QueryParam("C") String c) throws Exception {

		if (cn == null) {
			cn = alias;
		}
		// String certInfo = App.certificateChainGeneration.createCertFor(alias, cn, o, c);
		App.certForSigner = alias;

		(new WriteImageType()).createSignatureImageFile(alias, alias, "docs/signatures", Font.BOLD, 72);

		(new WriteImageType()).createSignatureImageFile(alias, WriteImageType.extractInitials(alias), "docs/initials",
				Font.BOLD, 72);

		String csr = getCsrForNewKeyPair(alias, cn, o, c);

		return Response.ok().entity(csr).build();
	}

	@GET
	@Path("createSignerJourneyEmulate")
	@ApiOperation(value = "Creates a signer with valid unsigned cert - returns the alias of the signing key & cert - diagnostic")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Read successful"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response createSignerJourneyEmulate(
			@ApiParam(value = "Uniquely identifies the key", required = true) @QueryParam("alias") String alias)
			throws Exception {

		App.certForSigner = alias;

		(new WriteImageType()).createSignatureImageFile(alias, alias, "docs/signatures", Font.BOLD, 72);
		(new WriteImageType()).createSignatureImageFile(alias, WriteImageType.extractInitials(alias), "docs/initials",
				Font.BOLD, 72);

		String csr = getCsrForNewKeyPair(alias, alias, "Organisation", "Country");
		final String pemSignedCert = App.certificateService.certificateSigningResponse(csr);
		String name = App.certificateService.registerSignedCert(pemSignedCert);

		return Response.ok().entity(name).build();
	}

	private String getCsrForNewKeyPair(String alias, String cn, String o, String c) throws Exception {
		return App.certificateService.generateCSR(alias, alias, o, c);
	}

	@PUT
	@Path("signCertificate")
	@ApiOperation(value = "Creates a signed cert PEM")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Read successful"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response signCertificate(String csr) throws Exception {

		return Response.ok().entity(App.certificateService.certificateSigningResponse(csr)).build();
	}

	@PUT
	@Path("registerCertificate")
	@ApiOperation(value = "Registers the signer's signed certificate")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Read successful"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response registerCertificate(String signedCertificatePem) throws Exception {

		return Response.ok().entity(App.certificateService.registerSignedCert(signedCertificatePem)).build();
	}

	@POST
	@Path("uploadInitials")
	@ApiOperation(value = "Uploads and persists an initials image file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response uploadInitials(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {

		return uploadFile("docs/initials", uploadedInputStream, App.certForSigner, "png");

	}

	@POST
	@Path("uploadSignature")
	@ApiOperation(value = "Uploads and persists an signature image file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response uploadSignature(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {

		return uploadFile("docs/signatures", uploadedInputStream, App.certForSigner, "png");

	}

	@POST
	@Path("uploadFile")
	@ApiOperation(value = "Uploads and persists a file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {

		String[] filenameParts = fileDetail.getFileName().split("\\.");
		return uploadFile("docs/unsigned", uploadedInputStream, filenameParts[0], filenameParts[1]);

	}

	@GET
	@Path("numberDoc/{docId}")
	@ApiOperation(value = "Signs a doc")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Signed successfully"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response numberDoc(
			@ApiParam(value = "Uniquely identifies the document to be signed", required = true) @PathParam("docId") String docId)
			throws Exception {

		applyDocIdentifyingInfo(docId);

		String signedDocId = "/DocSigner/identified/" + docId;
		return Response.ok().entity(signedDocId).build();
	}

	@GET
	@Path("numberAndMarkDoc/{docId}")
	@ApiOperation(value = "Signs a doc")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Signed successfully"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response numberAndMarkDoc(
			@ApiParam(value = "Uniquely identifies the document to be signed", required = true) @PathParam("docId") String docId)
			throws Exception {

		applyDocIdentifyingInfo(docId);
		applyDocBoxes(docId);

		String signedDocId = "/DocSigner/marked/" + docId;
		return Response.ok().entity(signedDocId).build();
	}

	@GET
	@Path("signDoc/{docId}")
	@ApiOperation(value = "Signs a doc")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Signed successfully"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response signDoc(
			@ApiParam(value = "Uniquely identifies the document to be signed", required = true) @PathParam("docId") String docId,
			@ApiParam(value = "Who is signing") @QueryParam("who") String who,
			@ApiParam(value = "Bypasses the device auth") @QueryParam("preAuthorised") boolean preAuthorised)
			throws Exception {

		if (!preAuthorised) {
			// get authorisation
			String userMessage = "Do you approve the signing of the document with identification mark:&nbsp;&nbsp"
					+ "<image src=\"DocSigner/docs/identicons/" + App.identNumber + ".png\"/>&nbsp;&nbsp;"
					+ Integer.toString(App.identNumber, 16);
			String auth = getAuthFromBrowser(userMessage, who);

			if (!auth.equals(AppUtils.RESPONSE_YES)) {
				return Response.status(Response.Status.OK).entity("DECLINED").build();
			}
		}

		// sign

		insertTimestampedSignatureImage(docId, who);

		signCryptographically(docId, who);

		// TODO - tell the device that doc is signed

		String signedDocId = "/DocSigner/signed/" + docId;
		return Response.ok().entity(signedDocId).build();
	}

	private void signCryptographically(String docId, String signerAlias) throws KeyStoreException,
			UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException {
		CreateSignature signing = new CreateSignature(signerAlias, App.keyStore, CertificateService.KEYSTORE_PASSWORD);
		signing.setExternalSigning(false);

		File inFile = new File("docs/half-signed/" + docId);
		File outFile = new File("docs/signed", docId);
		signing.signDetached(inFile, outFile, null);
	}

	@GET
	@Path("docs/{path}/{docId}")
	@ApiOperation(value = "Reads a doc")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Read successful"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response getDoc(
			@ApiParam(value = "Path to the doc - signed or unsigned", required = true) @PathParam("path") String path,
			@ApiParam(value = "Uniquely identifies the signed document to be loaded", required = true) @PathParam("docId") String docId)
			throws Exception {

		byte[] fileContent = AppUtils.readBytesFromFile("docs/" + path + "/" + docId);
		return Response.ok(fileContent).build();
	}

	@POST
	@Path("authResponse")
	@ApiOperation(value = "Deals with response to auth popup on device")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK") })
	public Response authResponse(
			@ApiParam(value = "Yes or No", required = true) @QueryParam("responseOption") String responseOption)
			throws Exception {
		App.authResponse = responseOption;
		// Thread.sleep(100);
		App.semaphore = true;
		return Response.ok().build();
	}

	@GET
	@Path("authRequest")
	@ApiOperation(value = "Prompts a 'device' confirmation message - diagnostic")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad request") })
	public Response authRequest(@ApiParam(value = "Who is signing") @QueryParam("who") String who,
			@ApiParam(value = "message to display", required = true) @QueryParam("message") String message)
			throws Exception {

		String auth = getAuthFromBrowser(message, who);

		return Response.ok().entity(auth).build();
	}

	@GET
	@Path("clearEventSources")
	@ApiOperation(value = "Clears out Event Sources - serlet doesn't know when they die")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad request") })
	public Response clearEventSources() throws Exception {

		App.eventSource.emitters = new ArrayList<>();

		return Response.ok().entity("Ok").build();
	}

	@GET
	@Path("prepCheque")
	@ApiOperation(value = "Build a cheque for signing")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad request") })
	public Response prepCheque(@ApiParam(value = "Payee", required = true) @QueryParam("payee") String payee,
			@ApiParam(value = "Cheque amount in cents", required = true) @QueryParam("amountCents") long amountCents)
			throws Exception {

		File file = new File("docs/blank_cheque.pdf");
		PDDocument doc = PDDocument.load(file);

		App.pdfImageManipulation.applyChequeSpecifics(App.brand, App.locale, payee, amountCents, doc);

		doc.save("docs/unsigned/cheque.pdf");
		doc.close();

		return Response.ok().entity("").build();
	}

	private Response uploadFile(String writeFolder, InputStream uploadedInputStream, String name, String extension)
			throws IOException {
		byte[] data = AppUtils.readBytesFromInputStream(uploadedInputStream);
		(new WriteImageType()).writeUploadedFile(name, writeFolder, data, extension);
		return Response.status(200).entity("").build();
	}

	@GET
	@Path("sendDoc/{docId}")
	@ApiOperation(value = "Signs a doc")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Sent successfully"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response sendDoc(
			@ApiParam(value = "Uniquely identifies the document to be sent", required = true) @PathParam("docId") String docId,
			@ApiParam(value = "Who is signing") @QueryParam("from") String from,
			@ApiParam(value = "Recipient") @QueryParam("to") String recipient,
			@ApiParam(value = "Bypasses the device auth") @QueryParam("preAuthorised") boolean preAuthorised)
			throws Exception {

		if (!preAuthorised) {
			// get authorisation
			String userMessage = "Do you approve the sending of the document \'" + docId
					+ "\' with identification mark:&nbsp;&nbsp<image src=\"DocSigner/docs/identicons/"
					+ App.identNumber + ".png\"/>&nbsp;&nbsp;" + Integer.toString(App.identNumber, 16) + " to "
					+ recipient;
			String auth = getAuthFromBrowser(userMessage, from);

			if (!auth.equals(AppUtils.RESPONSE_YES)) {
				return Response.status(Response.Status.OK).entity("DECLINED").build();
			}
		}

		insertCoverPageWithSenderSignature(docId, from);

		// recipient
		notifyDesktop(docId, recipient, "recipient", from, "javascript:void(0);", "available");

		String signedDocId = "/DocSigner/docs/sender-added/" + docId;
		return Response.ok().entity(signedDocId).build();
	}

	@GET
	@Path("notifyRecipient/{docId}")
	@ApiOperation(value = "Notifies the recipient that a doc is available for unsealing - diagnostic")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Sent successfully"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response notifyRecipient(
			@ApiParam(value = "Uniquely identifies the document that has arrived", required = true) @PathParam("docId") String docId,
			@ApiParam(value = "Who is signing") @QueryParam("from") String from,
			@ApiParam(value = "Recipient") @QueryParam("to") String recipient) throws Exception {

		notifyDesktop(docId, recipient, "recipient", from, "javascript:void(0);", "available");

		String signedDocId = "/DocSigner/docs/sender-added/" + docId;
		return Response.ok().entity(signedDocId).build();
	}

	@GET
	@Path("requestDoc/{docId}")
	@ApiOperation(value = "Requests to read a doc")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Made available for reading successfully"),
			@ApiResponse(code = 400, message = "Bad request") })
	public Response requestDoc(
			@ApiParam(value = "Uniquely identifies the document to be read", required = true) @PathParam("docId") String docId,
			@ApiParam(value = "Sender") @QueryParam("from") String from,
			@ApiParam(value = "Recipient") @QueryParam("to") String recipient,
			@ApiParam(value = "Bypasses the device auth") @QueryParam("preAuthorised") boolean preAuthorised)
			throws Exception {

		if (!preAuthorised) {
			// get authorisation
			String userMessage = "Acknowledge receipt of document " + docId + " from " + from
					+ " with identification mark:&nbsp;&nbsp<image src=\"DocSigner/docs/identicons/" + App.identNumber
					+ ".png\"/>&nbsp;&nbsp;" + Integer.toString(App.identNumber, 16) + "";
			String auth = getAuthFromBrowser(userMessage, recipient);

			if (!auth.equals(AppUtils.RESPONSE_YES)) {
				return Response.status(Response.Status.OK).entity("DECLINED").build();
			}
		}

		addRecipientSignatureToCoverPage(docId, recipient);

		// update sender desktop
		notifyDesktop(docId, from, "sender", recipient, "javascript:void(0);", "read");

		String readDocId = "/DocSigner/docs/recipient-added/" + docId;
		return Response.ok().entity(readDocId).build();
	}

	@GET
	@Path("notifyDesktop")
	@ApiOperation(value = "Lets a desktop know a doc has arrived - diagnostic")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad request") })
	public Response notifyDesktopDiagnostic(@ApiParam(value = "Which desktop") @QueryParam("who") String who,
			@ApiParam(value = "Document that has arrived", required = true) @QueryParam("docId") String docId)
			throws Exception {

		notifyDesktop(docId, who, "recipient", "a sender", "javascript:void(0);", "available");

		return Response.ok().entity("Ok").build();
	}

	private void notifyDesktop(String docId, String who, String role, String otherParty, String link, String status)
			throws InvalidPasswordException, IOException {
		AppUtils.emitEventSafely(App.eventSource, who + "|receivedoc|" + role + "|" + docId + "|" + otherParty + "|"
				+ link + "|" + status);

	}

	private void insertCoverPageWithSenderSignature(String docId, String from) throws IOException,
			InvalidPasswordException {
		File file = new File("docs/unsigned/" + docId);
		PDDocument doc = PDDocument.load(file);
		doc.setAllSecurityToBeRemoved(true);
		App.pdfImageManipulation.insertCoverPageWithSenderSignature(App.brand, from, doc);

		doc.save("docs/sender-added/" + docId);
		doc.close();

	}

	private void addRecipientSignatureToCoverPage(String docId, String to) throws IOException, InvalidPasswordException {
		File file = new File("docs/sender-added/" + docId);
		PDDocument doc = PDDocument.load(file);
		doc.setAllSecurityToBeRemoved(true);
		App.pdfImageManipulation.addRecipientSignatureToCoverPage(to, doc);

		doc.save("docs/recipient-added/" + docId);
		doc.close();

	}

	private void insertTimestampedSignatureImage(String docId, final String signer) throws InvalidPasswordException,
			IOException {
		File file = new File("docs/identified/" + docId);
		PDDocument doc = PDDocument.load(file);
		doc.setAllSecurityToBeRemoved(true);
		App.pdfImageManipulation.insertTimestampedSignature(signer, true, doc);

		doc.save("docs/half-signed/" + docId);
		doc.close();
	}

	private void applyDocIdentifyingInfo(String docId) throws IOException {
		File file = new File("docs/unsigned/" + docId);
		PDDocument doc = PDDocument.load(file);
		doc.setAllSecurityToBeRemoved(true);
		App.pdfImageManipulation.applyDocIdentifyingInfo(doc);

		doc.save("docs/identified/" + docId);

		doc.close();
	}

	private void applyDocBoxes(String docId) throws IOException {
		File file = new File("docs/identified/" + docId);
		PDDocument doc = PDDocument.load(file);
		doc.setAllSecurityToBeRemoved(true);

		App.pdfImageManipulation.applyDocBoxes(doc);

		doc.save("docs/marked/" + docId);

		doc.close();
	}

	private String getAuthFromBrowser(String userMessage, String who) {
		App.semaphore = false;
		AppUtils.emitEventSafely(App.eventSource, who + "|authoriserequest|" + userMessage);

		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.info("A thread sleep was interupted");
			}
			if (App.semaphore) {
				App.semaphore = false;
				return App.authResponse;
			}
		}
	}

}