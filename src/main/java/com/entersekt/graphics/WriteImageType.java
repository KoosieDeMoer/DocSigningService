package com.entersekt.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class WriteImageType {
	public static final int CHAR_WIDTH = 52;
	private static final int CHAR_HEIGHT = 100;

	static public void main(String args[]) throws Exception {
		try {
			String signer = "Koosie de Moer";

			int width = 200, height = 200;

			final int fontStyle = Font.PLAIN;
			final int fontSize = 24;
			final String writeFolder = "docs/signatures/";

			// TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
			// into integer pixels
			(new WriteImageType()).createSignatureImageFile(signer, signer, writeFolder, fontStyle, fontSize);

		} catch (IOException ie) {
			ie.printStackTrace();
		}

	}

	public void createSignatureImageFile(String signer, String text, final String writeFolder, final int fontStyle,
			final int fontSize) throws FontFormatException, IOException {

		Font font = Font.createFont(Font.TRUETYPE_FONT, new File("docs/fonts/Hijrnotes.ttf")); // or
		font = font.deriveFont(fontStyle, fontSize);

		// to calculate the image size
		BufferedImage bi1 = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig1 = bi1.createGraphics();
		ig1.setFont(font);
		FontMetrics fontMetrics1 = ig1.getFontMetrics();
		int stringHeight1 = fontMetrics1.getAscent();
		int stringWidth1 = fontMetrics1.stringWidth(text);

		final int width = (text.length() + 1) * CHAR_WIDTH;
		BufferedImage bi = new BufferedImage(stringWidth1, stringHeight1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig2 = bi.createGraphics();

		ig2.setFont(font);
		FontMetrics fontMetrics = ig2.getFontMetrics();
		int stringHeight = fontMetrics.getAscent();
		ig2.setPaint(Color.black);
		ig2.drawString(text, 0, (int) (stringHeight * 0.8));

		ImageIO.write(bi, "PNG", new File(writeFolder + "/" + signer + ".png"));
	}

	public void writeUploadedFile(String name, final String writeFolder, byte[] data, String extension)
			throws IOException {

		final File file = new File(writeFolder + "/" + name + "." + extension);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
			fos.close();
		}
	}

	public static String extractInitials(String alias) {
		String initials = "";
		String[] words = alias.split(" ");
		for (String word : words) {
			initials += word.substring(0, 1).toUpperCase();
		}
		return initials;
	}
}