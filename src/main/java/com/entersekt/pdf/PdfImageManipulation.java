package com.entersekt.pdf;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entersekt.docsigning.App;
import com.entersekt.json.JsonSerialisationService;

public class PdfImageManipulation {

	private static final String BOB_EMAIL = "name@someotherplace.com";
	private static final String ALICE_EMAIL = "alice@example.com";
	private static final int CHEQUE_WORDS_AMOUNT_LINE_PITCH = 30;
	private static final int CHEQUE_WORDS_AMOUNT_WRAP_LENGTH = 50;
	private static final Logger log = LoggerFactory.getLogger(PdfImageManipulation.class);

	public void applyDocIdentifyingInfo(PDDocument doc) throws IOException {
		PDPage page = doc.getPage(0);
		int pageWidth = (int) page.getMediaBox().getWidth();
		int pageHeight = (int) page.getMediaBox().getHeight();
		PDPageContentStream contents = new PDPageContentStream(doc, page, true, false);
		final int imageXOffset = 100;
		final int imageYOffset = 10;
		final int fontHeight = 6;
		int identiconImageHeight = 0;
		App.identNumber = (int) (Math.random() * Integer.MAX_VALUE);

		// writes an identicon file for picking up later
		App.identiconTools.generate(App.identNumber, 32, "PNG", "docs/identicons");

		try {
			PDImageXObject pdImage = PDImageXObject.createFromFile("docs/identicons/" + App.identNumber + ".png", doc);
			identiconImageHeight = pdImage.getHeight();
			contents.drawImage(pdImage, imageXOffset, pageHeight - imageYOffset - identiconImageHeight);
		} catch (Exception e) {
			log.error("Failed to add identicon graphic for " + App.identNumber + " with: " + e.getMessage());
		}

		contents.beginText();
		contents.setFont(PDType1Font.HELVETICA, 12);
		contents.newLineAtOffset(imageXOffset + identiconImageHeight + fontHeight, pageHeight - imageYOffset
				- identiconImageHeight / 2 - fontHeight);

		contents.showText(Integer.toString(App.identNumber, 16));
		contents.endText();

		contents.close();
	}

	public void applyDocBoxes(PDDocument doc) throws IOException {
		insertTimestampedSignature("box", false, doc);
	}

	public void insertTimestampedSignature(final String signer, boolean applyTimestamp, PDDocument doc)
			throws IOException {
		final int numberOfPages = doc.getNumberOfPages();

		for (int i = 0; i < (numberOfPages - 1); i++) {
			initialPage(signer, doc, i);
		}
		signLastPage(signer, doc, numberOfPages - 1, applyTimestamp);
	}

	private void initialPage(final String initials, PDDocument doc, final int pageIndex) throws IOException {
		PDPage page = doc.getPage(pageIndex);
		int pageWidth = (int) page.getMediaBox().getWidth();
		PDPageContentStream contents = new PDPageContentStream(doc, page, true, false);
		final int imageCornerOffset = 50;
		try {
			PDImageXObject pdImage = PDImageXObject.createFromFile("docs/initials/" + initials + ".png", doc);
			double scale = 0.3;
			final int imageWidth = (int) (pdImage.getWidth() * scale);
			final int imageHeight = (int) (pdImage.getHeight() * scale);
			contents.drawImage(pdImage, pageWidth - imageWidth - imageCornerOffset, imageCornerOffset, imageWidth,
					imageHeight);

		} catch (Exception e) {
			log.error("Failed to add signature graphic for " + initials + " with: " + e.getMessage());
		}
		contents.close();
	}

	private void signLastPage(final String signer, PDDocument doc, final int pageIndex, boolean applyTimestamp)
			throws IOException {
		PDPage page = doc.getPage(pageIndex);
		int pageWidth = (int) page.getMediaBox().getWidth();
		PDPageContentStream contents = new PDPageContentStream(doc, page, true, false);
		final int imageCornerOffset = 50;
		int xOffset = imageCornerOffset;
		int yOffset = imageCornerOffset;
		try {
			PDImageXObject pdImage = PDImageXObject.createFromFile("docs/signatures/" + signer + ".png", doc);
			double scale = 0.3;
			final int imageWidth = (int) (pdImage.getWidth() * scale);
			final int imageHeight = (int) (pdImage.getHeight() * scale);
			contents.drawImage(pdImage, pageWidth - imageWidth - xOffset, yOffset, imageWidth, imageHeight);

		} catch (Exception e) {
			log.error("Failed to add signature graphic for " + signer + " with: " + e.getMessage());
		}
		if (applyTimestamp) {
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(pageWidth - imageCornerOffset - 120, imageCornerOffset);
			contents.showText(JsonSerialisationService.DATE_FORMAT.format(new Date()));
			contents.endText();
		}
		contents.close();
	}

	public void applyChequeSpecifics(final String brand, Locale locale, String payeeName, long amountCents,
			PDDocument doc) throws IOException {
		PDPage page = doc.getPage(0);
		int pageWidth = (int) page.getMediaBox().getWidth();
		int pageHeight = (int) page.getMediaBox().getHeight();
		PDPageContentStream contents = new PDPageContentStream(doc, page, true, false);
		final int imageCornerOffset = 60;

		Currency currency = Currency.getInstance(locale);
		NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);

		int fractional = (int) Math.pow(10, currency.getDefaultFractionDigits());
		long amountInWholeCurrencyUnits = amountCents / fractional;
		int amountInFractionalCurrencyUnits = (int) (amountCents % fractional);

		try {
			PDImageXObject pdImage = PDImageXObject.createFromFile("branding_images/" + brand + ".png", doc);
			double scale = 0.6;
			final int imageWidth = (int) (pdImage.getWidth() * scale);
			final int imageHeight = (int) (pdImage.getHeight() * scale);
			contents.drawImage(pdImage, imageCornerOffset / 4, pageHeight - imageCornerOffset, imageWidth, imageHeight);

			// date
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(pageWidth - 100, pageHeight - 42);
			contents.showText((new SimpleDateFormat("dd-MM-yyyy")).format(new Date()));
			contents.endText();

			// payee
			contents.beginText();
			contents.newLineAtOffset(75, 147);
			contents.showText(payeeName);
			contents.endText();

			// amount in words
			final String[] formattedAmount = wordifyAmount(currency, fractional, amountInWholeCurrencyUnits,
					amountInFractionalCurrencyUnits);
			int lineNumber = 0;
			for (String line : formattedAmount) {
				contents.beginText();
				contents.newLineAtOffset(25, 117 - (lineNumber++ * CHEQUE_WORDS_AMOUNT_LINE_PITCH));
				contents.showText(line);

				contents.endText();
			}
			// amount in numbers
			contents.beginText();
			contents.newLineAtOffset(310, 125);
			contents.showText(currencyFormatter.format(amountCents / (double) fractional));
			contents.endText();

		} catch (Exception e) {
			log.error("Failed to add signature graphic for " + brand + " with: " + e.getMessage());
		}
		contents.close();
	}

	public void insertCoverPageWithSenderSignature(String brand, String sender, PDDocument doc) throws IOException {
		PDPage coverPage = new PDPage();
		int pageWidth = (int) coverPage.getMediaBox().getWidth();
		int pageHeight = (int) coverPage.getMediaBox().getHeight();
		PDPageContentStream contents = new PDPageContentStream(doc, coverPage, true, false);
		final int imageCornerOffset = 50;
		final int linePitch = 25;

		try {
			PDImageXObject pdImage = PDImageXObject.createFromFile("branding_images/" + brand + ".png", doc);
			double scale = 1.0;
			int imageWidth = (int) (pdImage.getWidth() * scale);
			int imageHeight = (int) (pdImage.getHeight() * scale);
			contents.drawImage(pdImage, imageCornerOffset, pageHeight - imageCornerOffset * 3, imageWidth, imageHeight);

			// heading
			final int xPosFieldName = imageCornerOffset * 3;
			final int xPosFieldValue = xPosFieldName + imageCornerOffset * 2;
			int yPos = (int) (pageHeight - imageCornerOffset * 2.2);
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 24);
			contents.newLineAtOffset(xPosFieldName + imageCornerOffset, yPos);
			contents.showText("Transmission Record");
			contents.endText();

			// add identity info
			yPos -= (linePitch * 2);
			try {
				pdImage = PDImageXObject.createFromFile("docs/identicons/" + App.identNumber + ".png", doc);
				int identiconImageHeight = pdImage.getHeight();
				contents.drawImage(pdImage, xPosFieldName + imageCornerOffset, yPos - identiconImageHeight / 2);
			} catch (Exception e) {
				log.error("Failed to add identicon graphic for " + App.identNumber + " with: " + e.getMessage());
			}

			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(xPosFieldValue, yPos);
			contents.showText(Integer.toString(App.identNumber, 16));
			contents.endText();

			// Sent subheading
			yPos -= (linePitch * 3);
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 18);
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Sent");
			contents.endText();

			// sender
			yPos -= (linePitch * 1.2);
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Name:");
			contents.endText();
			contents.beginText();
			contents.newLineAtOffset(xPosFieldValue, yPos);
			contents.showText(sender);
			contents.endText();

			// email
			yPos -= linePitch;
			contents.beginText();
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Email:");
			contents.endText();
			contents.beginText();
			contents.newLineAtOffset(xPosFieldValue, yPos);
			contents.showText(sender.equals("Alice") ? ALICE_EMAIL : BOB_EMAIL);
			contents.endText();

			// signature
			yPos -= linePitch;
			contents.beginText();
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Signature:");
			contents.endText();
			int xOffset = xPosFieldValue;
			int yOffset = yPos;
			try {
				pdImage = PDImageXObject.createFromFile("docs/signatures/" + sender + ".png", doc);
				scale = 0.3;
				imageWidth = (int) (pdImage.getWidth() * scale);
				imageHeight = (int) (pdImage.getHeight() * scale);
				contents.drawImage(pdImage, xOffset, yOffset - imageHeight / 2, imageWidth, imageHeight);

			} catch (Exception e) {
				log.error("Failed to add signature graphic for " + sender + " with: " + e.getMessage());
			}

			// date
			yPos -= linePitch;
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Date:");
			contents.endText();
			contents.beginText();
			contents.newLineAtOffset(xPosFieldValue, yPos);
			contents.showText("" + (new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")).format(new Date()));
			contents.endText();

			// Received subheading
			yPos = pageHeight - imageCornerOffset * 8;
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 18);
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Received");
			contents.endText();

			// recipient
			yPos -= (linePitch * 1.2);
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Name:");
			contents.endText();

			// email
			yPos -= linePitch;
			contents.beginText();
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Email:");
			contents.endText();

			// signature
			yPos -= linePitch;
			contents.beginText();
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Signature:");
			contents.endText();

			// date
			yPos -= linePitch;
			contents.beginText();
			contents.setFont(PDType1Font.HELVETICA, 12);
			contents.newLineAtOffset(xPosFieldName, yPos);
			contents.showText("Date:");
			contents.endText();

		} catch (Exception e) {
			log.error("Failed to add signature graphic for " + brand + " with: " + e.getMessage());
		}
		contents.close();
		doc.addPage(coverPage);

		// move the added page to the front
		PDPageTree allPages = doc.getDocumentCatalog().getPages();
		if (allPages.getCount() > 1) {
			PDPage lastPage = allPages.get(allPages.getCount() - 1);
			allPages.remove(allPages.getCount() - 1);
			PDPage firstPage = allPages.get(0);
			allPages.insertBefore(lastPage, firstPage);
		}

	}

	public void addRecipientSignatureToCoverPage(String recipient, PDDocument doc) throws IOException {
		PDPage coverPage = doc.getPage(0);
		int pageWidth = (int) coverPage.getMediaBox().getWidth();
		int pageHeight = (int) coverPage.getMediaBox().getHeight();
		PDPageContentStream contents = new PDPageContentStream(doc, coverPage, true, false);
		final int imageCornerOffset = 50;
		final int linePitch = 25;
		final int xPosFieldName = imageCornerOffset * 3;
		final int xPosFieldValue = xPosFieldName + imageCornerOffset * 2;

		// recipient
		int yPos = pageHeight - imageCornerOffset * 8 - (int) (linePitch * 1.2);
		contents.beginText();
		contents.setFont(PDType1Font.HELVETICA, 12);
		contents.newLineAtOffset(xPosFieldValue, yPos);
		contents.showText(recipient);
		contents.endText();

		// email
		yPos -= linePitch;
		contents.beginText();
		contents.newLineAtOffset(xPosFieldValue, yPos);
		contents.showText(recipient.equals("Alice") ? ALICE_EMAIL : BOB_EMAIL);
		contents.endText();

		// signature
		yPos -= linePitch;
		int xOffset = xPosFieldValue;
		int yOffset = yPos;
		try {
			PDImageXObject pdImage = PDImageXObject.createFromFile("docs/signatures/" + recipient + ".png", doc);
			double scale = 0.3;
			int imageWidth = (int) (pdImage.getWidth() * scale);
			int imageHeight = (int) (pdImage.getHeight() * scale);
			contents.drawImage(pdImage, xOffset, yOffset - imageHeight / 2, imageWidth, imageHeight);

		} catch (Exception e) {
			log.error("Failed to add signature graphic for " + recipient + " with: " + e.getMessage());
		}

		// date
		yPos -= linePitch;
		contents.beginText();
		contents.newLineAtOffset(xPosFieldValue, yPos);
		contents.showText("" + (new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")).format(new Date()));
		contents.endText();

		contents.close();

	}

	private String[] wordifyAmount(Currency currency, int fractional, long amountInWholeCurrencyUnits,
			int amountInFractionalCurrencyUnits) {
		String wholePart = EnglishNumberToWords.convert(amountInWholeCurrencyUnits);
		wholePart = wholePart.substring(0, 1).toUpperCase() + wholePart.substring(1);
		String fractionalPart = (amountInFractionalCurrencyUnits == 0) ? "" : " and " + amountInFractionalCurrencyUnits
				+ "/" + fractional;

		final String unwrapped = wholePart + " " + currency.getDisplayName() + fractionalPart + " only";
		if (unwrapped.length() > CHEQUE_WORDS_AMOUNT_WRAP_LENGTH) {

			final int breakAt = unwrapped.lastIndexOf(" ", CHEQUE_WORDS_AMOUNT_WRAP_LENGTH);
			return new String[] { unwrapped.substring(0, breakAt), unwrapped.substring(breakAt + 1) };

		} else {
			return new String[] { unwrapped };
		}

	}
}
