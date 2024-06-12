package rk.ramin.fractor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManager {
	
	public static final int MAGIC_NUMBER = 707106781;
	
	/**
	 * Writes the given FileContent into the given OutputStream.
	 * You may receive IOExceptions doing this. However you have to close the stream after calling this, even if you are interrupted by an error.
	 */
	public static void writeFile(OutputStream os, FileContent fc) throws IOException {
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
		dos.writeInt(MAGIC_NUMBER); //Magic number
		dos.writeByte(0); //Version number.
		dos.writeInt(fc.getImageSize()[0]);
		dos.writeInt(fc.getImageSize()[1]);
		dos.writeBoolean(fc.getEffects());
		dos.writeInt(fc.getSampling());
		dos.writeDouble(fc.getAtomSizes()[0]);
		dos.writeDouble(fc.getAtomSizes()[1]);
		for (Double d : fc.getPointOfView()) {
			dos.writeDouble(d);
		}
		dos.writeInt(fc.getKnobs().length);
		for (Knob k : fc.getKnobs()) {
			dos.writeDouble(k.getXPosition());
			dos.writeDouble(k.getYPosition());
			dos.writeDouble(k.getScale());
			dos.writeDouble(k.getRotation());
			dos.writeBoolean(k.getIgnored());
		} 
		dos.writeUTF(" This is a fractal. Open it with Fractor!");
		dos.flush();
		os.close();
	}
	
	/**
	 * Reads out the file. 
	 * If we encounter any problems (no access / endOfStream / noMagicNumber / anything else)
	 * an exception is thrown. 
	 * You need to close the stream after calling this - no matter whether successful or with exception.
	 */
	public static FileContent readFile(InputStream is) throws Exception {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
		if (dis.readInt() != MAGIC_NUMBER) {
			throw new IOException("Wrong magic number. File seems to be no Fractor file. ");
		}
		FractorFilesReader ffr = null;
		byte version = dis.readByte();
		switch (version) {
			case 0: ffr = FractorFilesReaderVer0.instance;
		}
		if (ffr == null) {
			throw new Exception("Unkonwn version number ("+version+"). Is this version of Fractor out of date? ");
		} else {
			FileContent fc = ffr.read(dis);
			fc.verify();
			return fc;
		}
	}
}
