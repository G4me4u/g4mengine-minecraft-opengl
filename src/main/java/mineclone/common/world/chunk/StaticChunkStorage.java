package mineclone.common.world.chunk;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class StaticChunkStorage<T extends IChunk> implements IChunkStorage<T> {

	public static final int DEFAULT_CHUNK_COUNT_X = 16;
	public static final int DEFAULT_CHUNK_COUNT_Y = 8;
	public static final int DEFAULT_CHUNK_COUNT_Z = 16;
	
	private int chunkCountX;
	private int chunkCountY;
	private int chunkCountZ;
	
	private int strideZ;
	
	private IChunk[] chunks;

	private final ChunkPosition originChunkPos;
	private int originChunkOffset;
	
	public StaticChunkStorage() {
		setSize(DEFAULT_CHUNK_COUNT_X, DEFAULT_CHUNK_COUNT_Y, DEFAULT_CHUNK_COUNT_Z);
		
		originChunkPos = new ChunkPosition();
		originChunkOffset = 0;
	}
	
	public void setSize(int chunkCountX, int chunkCountY, int chunkCountZ) {
		if (chunkCountX <= 0 || chunkCountY <= 0 || chunkCountZ <= 0)
			throw new IllegalArgumentException("Chunk count must be positive!");

		int strideZ = chunkCountX * chunkCountY;
		
		IChunk[] newChunks = new IChunk[strideZ * chunkCountZ];
		
		int rx1 = Math.min(this.chunkCountX, chunkCountX);
		int ry1 = Math.min(this.chunkCountY, chunkCountY);
		int rz1 = Math.min(this.chunkCountZ, chunkCountZ);
		
		int i0 = originChunkOffset, i1 = 0;
		
		for (int rz = 0; rz < rz1; rz++) {
			for (int ry = 0; ry < ry1; ry++) {
				for (int rx = 0; rx < rx1; rx++)
					newChunks[i1++] = chunks[i0++ % chunks.length];
				
				i0 += this.chunkCountX - rx1;
				i1 += chunkCountX - rx1;
			}
			
			i0 += this.strideZ - ry1 * this.chunkCountX;
			i1 += strideZ - ry1 * chunkCountX;
		}
		
		this.chunkCountX = chunkCountX;
		this.chunkCountY = chunkCountY;
		this.chunkCountZ = chunkCountZ;
		
		this.strideZ = strideZ;
		
		chunks = newChunks;
		originChunkOffset = 0;
	}

	private int getChunkOffset(IChunkPosition chunkPos) {
		int rx = chunkPos.getChunkX() - originChunkPos.getChunkX();
		int ry = chunkPos.getChunkY() - originChunkPos.getChunkY();
		int rz = chunkPos.getChunkZ() - originChunkPos.getChunkZ();
		
		if (rx < 0 || rx >= chunkCountX)
			return -1;
		if (ry < 0 || ry >= chunkCountY)
			return -1;
		if (rz < 0 || rz >= chunkCountZ)
			return -1;
		
		return getRelativeChunkOffset(rx, ry, rz);
	}
	
	private int getRelativeChunkOffset(int rx, int ry, int rz) {
		int offset = rx + ry * chunkCountX + rz * strideZ + originChunkOffset;
		return (offset + chunks.length) % chunks.length;
	}
	
	@Override
	public T getChunk(IChunkPosition chunkPos) {
		int offset = getChunkOffset(chunkPos);
		if (offset < 0)
			return null;

		@SuppressWarnings("unchecked")
		T chunk = (T)chunks[offset];
		
		return chunk;
	}
	
	@Override
	public boolean setChunk(IChunkPosition chunkPos, T chunk) {
		int offset = getChunkOffset(chunkPos);
		if (offset < 0)
			return false;
		
		chunks[offset] = chunk;
		
		return true;
	}

	@Override
	public boolean contains(IChunkPosition chunkPos) {
		return (getChunkOffset(chunkPos) >= 0);
	}
	
	@Override
	public Iterator<ChunkEntry<T>> chunkIterator() {
		return new ChunkIterator();
	}
	
	public IChunkPosition getOriginChunkPos() {
		return originChunkPos;
	}
	
	public void setOrigin(IChunkPosition chunkPos) {
		int rx = chunkPos.getChunkX() - originChunkPos.getChunkX();
		int ry = chunkPos.getChunkY() - originChunkPos.getChunkY();
		int rz = chunkPos.getChunkZ() - originChunkPos.getChunkZ();

		if (rx >= chunkCountX || rx <= -chunkCountX ||
		    ry >= chunkCountY || ry <= -chunkCountY ||
		    rz >= chunkCountZ || rz <= -chunkCountZ) {
			
			originChunkPos.set(chunkPos);
			originChunkOffset = 0;
			
			invalidateChunks(0, 0, 0, chunkCountX, chunkCountY, chunkCountZ);
		} else {
			int chunkOffset = getRelativeChunkOffset(rx, ry, rz);

			if (chunkOffset != originChunkOffset) {
				originChunkPos.set(chunkPos);
				originChunkOffset = chunkOffset;
				
				int rx0 = 0, rx1 = chunkCountX;
				int ry0 = 0, ry1 = chunkCountY;
				int rz0 = 0, rz1 = chunkCountZ;

				if (rx > 0) {
					rx0 += rx;
					invalidateChunks(0, ry0, rz0, rx0, ry1, rz1);
				} else if (rx < 0) {
					rx1 += rx;
					invalidateChunks(rx1, ry0, rz0, chunkCountX, ry1, rz1);
				}

				if (ry > 0) {
					ry0 += ry;
					invalidateChunks(rx0, 0, rz0, rx1, ry0, rz1);
				} else if (ry < 0) {
					ry1 += ry;
					invalidateChunks(rx0, ry1, rz0, rx1, chunkCountY, rz1);
				}

				if (rz > 0) {
					rz0 += rz;
					invalidateChunks(rx0, ry0, 0, rx1, ry1, rz0);
				} else if (rz < 0) {
					rz1 += rz;
					invalidateChunks(rx0, ry0, rz1, rx1, ry1, chunkCountZ);
				}
			}
		}
	}
	
	private void invalidateChunks(int rx0, int ry0, int rz0, int rx1, int ry1, int rz1) {
		int iz = getRelativeChunkOffset(rx0, ry0, rz0);
		
		for (int rz = rz0; rz < rz1; rz++, iz += strideZ) {
			for (int ry = ry0, iy = iz; ry < ry1; ry++, iy += chunkCountX) {
				for (int rx = rx0, i = iy; rx < rx1; rx++, i++)
					invalidateChunk(originChunkPos.offset(rx, ry, rz), i % chunks.length);
			}
		}
	}
	
	private void invalidateChunk(IChunkPosition chunkPos, int offset) {
		chunks[offset] = null;
	}
	
	private class ChunkIterator implements Iterator<ChunkEntry<T>> {

		private int offset;
		
		private ChunkIterator() {
			offset = 0;
		}
		
		@Override
		public boolean hasNext() {
			return (offset < chunks.length);
		}

		@Override
		public ChunkEntry<T> next() {
			if (!hasNext())
				throw new NoSuchElementException();
			
			IChunkPosition chunkPos = getNextChunkPos();
			@SuppressWarnings("unchecked")
			T chunk = (T)chunks[(offset + originChunkOffset) % chunks.length];

			offset++;
			
			return new ChunkEntry<T>(chunkPos, chunk);
		}
		
		private IChunkPosition getNextChunkPos() {
			int offsetYZ = offset / chunkCountX;
			
			int rx = offset % chunkCountX;
			int ry = offsetYZ % chunkCountY;
			int rz = offsetYZ / chunkCountY;
			
			return originChunkPos.offset(rx, ry, rz);
		}
	}
}
