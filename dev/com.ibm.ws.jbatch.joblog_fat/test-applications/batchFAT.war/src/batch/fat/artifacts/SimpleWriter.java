package batch.fat.artifacts;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;

public class SimpleWriter implements ItemWriter {

	@Override
	public void open(Serializable checkpoint) throws Exception {
		//NOP
	}

	@Override
	public void close() throws Exception {
		//NOP
	}

	@Override
	public void writeItems(List<Object> items) throws Exception {
		for (Object o : items) {
			System.out.println("Writer writing: " + o);
		}
	}

	@Override
	public Serializable checkpointInfo() throws Exception {
		return null;
	}
	
}