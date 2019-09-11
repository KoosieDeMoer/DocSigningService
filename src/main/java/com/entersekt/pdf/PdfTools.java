package com.entersekt.pdf;

import java.io.File;
import java.util.Currency;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfTools {

	static final PdfImageManipulation pdfImageManipulation = new PdfImageManipulation();

	public static void main(String[] args) throws Exception {

		Locale locale = Locale.forLanguageTag("en-ZA");
		Currency currency = Currency.getInstance(locale);

		File file = new File("docs/blank_cheque.pdf");
		PDDocument doc = PDDocument.load(file);

		pdfImageManipulation.applyChequeSpecifics("standardbank", locale, "Rupert Murdock", 1123456789, doc);

		doc.save("docs/unsigned/cheque.pdf");
		doc.close();

	}
}
