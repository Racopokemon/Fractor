package rk.ramin.fractor;

import java.io.DataInputStream;
import java.io.IOException;

public class FractorFilesReaderVer0 extends FractorFilesReader {

	public static FractorFilesReaderVer0 instance = new FractorFilesReaderVer0();
	
	@Override
	public FileContent read(DataInputStream dis) throws Exception {
		FileContent fc = new FileContent();
		int w = dis.readInt(), h = dis.readInt();
		boolean e = dis.readBoolean();
		int s = dis.readInt();
		double as = dis.readDouble(), ms = dis.readDouble();
		fc.setRenderSettings(w, h, e, s, as, ms);
		//-----------------
		fc.setPointOfView(dis.readDouble(), dis.readDouble(), 
				dis.readDouble(), dis.readDouble());
		//-----------------
		int size = dis.readInt();
		Knob[] ks = new Knob[size];
		//-----------------
		for (int i = 0; i < size; i++) {
			ks[i] = new Knob(dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble());
			ks[i].setIgnored(dis.readBoolean());
		}
		fc.setKnobs(ks);
		return fc;
	}

}
