package com.docuverse.identicon;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class IdenticonTools {

	private IdenticonRenderer renderer = new NineBlockIdenticonRenderer2();

	public static void main(String[] args) throws IOException {
		(new IdenticonTools()).generate(10000001, 128, "PNG", "docs/identicons");

	}

	public void generate(int identNumber, int size, String type, String storagePath) throws IOException {

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		RenderedImage image = renderer.render(identNumber, size);
		ImageIO.write(image, type, byteOut);
		byte[] imageBytes = byteOut.toByteArray();

		FileOutputStream fos = new FileOutputStream(storagePath + "/" + identNumber + "." + type.toLowerCase());
		fos.write(imageBytes);
		fos.close();

	}
}
