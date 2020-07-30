package minecraft;

public interface IResource extends AutoCloseable {

	@Override
	default public void close() {
		dispose();
	}
	
	public void dispose();
	
}
