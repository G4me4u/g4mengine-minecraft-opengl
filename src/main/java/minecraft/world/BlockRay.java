package minecraft.world;

import minecraft.math.LinMath;
import minecraft.math.Mat4;
import minecraft.math.Vec3;
import minecraft.world.block.MutableBlockPosition;
import minecraft.world.block.state.BlockState;

public class BlockRay {

	public static final float DEFAULT_STEP_SIZE = 0.1f;
	public static final float DEFAULT_MAX_DIST = 5.0f;
	
	private final World world;
	
	private final float stepSize;
	private final float maxDist;
	
	public BlockRay(World world) {
		this(world, DEFAULT_STEP_SIZE, DEFAULT_MAX_DIST);
	}

	public BlockRay(World world, float stepSize) {
		this(world, stepSize, DEFAULT_MAX_DIST);
	}

	public BlockRay(World world, float stepSize, float maxDist) {
		if (stepSize < LinMath.EPSILON)
			throw new IllegalArgumentException("Step-size must be positive and larger than " + LinMath.EPSILON);

		this.world = world;
		
		this.stepSize = stepSize;
		this.maxDist = maxDist;
	}
	
	public BlockHitResult castRay(Vec3 pos, Vec3 dir) {
		return castRay(pos.x, pos.y, pos.z, dir);
	}

	public BlockHitResult castRay(Mat4 viewMatrix) {
		Vec3 forward = new Vec3(viewMatrix.m20, viewMatrix.m21, viewMatrix.m22);
		return castRay(viewMatrix.m30, viewMatrix.m31, viewMatrix.m32, forward);
	}

	public BlockHitResult castRay(float x, float y, float z, Vec3 dir) {
		dir = new Vec3(dir).normalize().mul(stepSize);
	
		MutableBlockPosition blockPos = new MutableBlockPosition();
		
		boolean first = false;
		
		float dist = 0.0f;
		while (dist < maxDist) {
			int xx = (int)x;
			int yy = (int)y;
			int zz = (int)z;
			
			if (first || !blockPos.equals(xx, yy, zz)) {
				first = false;
				
				blockPos.x = xx;
				blockPos.y = yy;
				blockPos.z = zz;
				
				BlockState block = world.getBlockState(blockPos);
				if (block.getBlock() != Blocks.AIR_BLOCK) {
					Vec3 hitOffset = new Vec3(x - xx - 0.5f, y - yy - 0.5f, z - zz - 0.5f);
					Direction face = Direction.fromVector(hitOffset);
					return new BlockHitResult(blockPos, x, y, z, face);
				}
			}
			
			x += dir.x;
			y += dir.y;
			z += dir.z;
			
			dist += stepSize;
		}
		
		return null;
	}
}
