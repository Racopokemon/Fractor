package rk.ramin.fractor;

import java.io.DataInputStream;

public abstract class FractorFilesReader {
	
	public abstract FileContent read(DataInputStream is) throws Exception;
	
}
