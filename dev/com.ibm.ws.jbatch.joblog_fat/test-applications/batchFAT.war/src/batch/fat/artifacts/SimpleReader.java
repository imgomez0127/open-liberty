package batch.fat.artifacts;

import java.io.Serializable;

import javax.batch.api.chunk.ItemReader;

public class SimpleReader implements ItemReader {
	
	int next = 1;

	@Override
	public void open(Serializable checkpoint) throws Exception {
		if (checkpoint != null) {
			next = (Integer) checkpoint;
		}
	}

	@Override
	public void close() throws Exception {
		// NOP
	}

	@Override
	public Object readItem() throws Exception {
		if (next > 10) {
			next = 1;
			return null;
		} else {
			return "Read item " + (next++);
		}
	}

	@Override
	public Serializable checkpointInfo() throws Exception {
		return next;
	}
}