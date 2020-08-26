package minecraft.common.world.block;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import minecraft.client.renderer.model.IBlockModel;
import minecraft.client.renderer.world.BlockTextures;
import minecraft.common.ReferenceRegsitry;
import minecraft.common.world.Blocks;
import minecraft.common.world.Direction;
import minecraft.common.world.EntityHitbox;
import minecraft.common.world.IServerWorld;
import minecraft.common.world.IWorld;
import minecraft.common.world.block.state.BlockState;
import minecraft.common.world.block.state.IBlockState;

public class Block {

	public static final String AIR_BLOCK_ID              = "air";
	public static final String DIRT_BLOCK_ID             = "dirt";
	public static final String GRASS_BLOCK_ID            = "grass";
	public static final String PLANKS_BLOCK_ID           = "planks";
	public static final String STONE_BLOCK_ID            = "stone";
	public static final String COBBLESTONE_BLOCK_ID      = "cobblestone";
	public static final String PLANT_BLOCK_ID            = "plant";
	public static final String LEAVES_BLOCK_ID           = "leaves";
	public static final String LOG_BLOCK_ID              = "log";
	public static final String REDSTONE_WIRE_BLOCK_ID    = "redstone_wire";
	public static final String REDSTONE_BLOCK_ID         = "redstone_block";
	public static final String STONE_SLAB_BLOCK_ID       = "stone_slab";
	public static final String PLANKS_SLAB_BLOCK_ID      = "planks_slab";
	
	private static ReferenceRegsitry<String, Block> blockRegistry = null;
	
	private String name;
	private final IBlockState defaultState;
	
	protected Block() {
		name = null;
		
		defaultState = createDefaultState();
	}
	
	public IBlockState getPlacementState(IServerWorld world, IBlockPosition pos, IBlockState state) {
		return state;
	}
	
	public void onBlockAdded(IServerWorld world, IBlockPosition pos, IBlockState state) {
		if (!state.isOf(Blocks.AIR_BLOCK))
			world.updateNeighbors(pos, IServerWorld.COMMON_UPDATE_FLAGS);
	}
	
	public void onBlockRemoved(IServerWorld world, IBlockPosition pos, IBlockState state) {
		if (!state.isOf(Blocks.AIR_BLOCK))
			world.updateNeighbors(pos, IServerWorld.COMMON_UPDATE_FLAGS);
	}
	
	public void onStateChanged(IServerWorld world, IBlockPosition pos, IBlockState oldState, IBlockState newState) {
	}
	
	public void onStateUpdate(IServerWorld world, IBlockPosition pos, IBlockState state, Direction fromDir, IBlockState fromState) {
	}

	public void onBlockUpdate(IServerWorld world, IBlockPosition pos, IBlockState state, Direction fromDir, IBlockState fromState) {
	}
	
	public void onInventoryUpdate(IServerWorld world, IBlockPosition pos, IBlockState state, Direction fromDir, IBlockState fromState) {
	}
	
	public void onRandomUpdate(IServerWorld world, IBlockPosition pos, IBlockState state, Random random) {
	}
	
	public boolean hasRandomUpdate() {
		return false;
	}
	
	public void getEntityHitboxes(IWorld world, IBlockPosition pos, IBlockState state, List<EntityHitbox> hitboxes) {
		if (hasEntityHitbox(world, pos, state)) {
			float x = pos.getX();
			float y = pos.getY();
			float z = pos.getZ();
			
			hitboxes.add(new EntityHitbox(x, y, z, x + 1.0f, y + 1.0f, z + 1.0f));
		}
	}

	protected boolean hasEntityHitbox(IWorld world, IBlockPosition pos, IBlockState state) {
		return isSolid();
	}

	public IBlockModel getModel(IWorld world, IBlockPosition pos, IBlockState state) {
		// TODO: move this out of the common package.
		return null;
	}
	
	public boolean isSolid() {
		return false;
	}
	
	public boolean canGrowVegetation(IBlockState state) {
		return false;
	}
	
	public boolean isAligned(IBlockState state, Direction dir) {
		return isSolid();
	}
	
	public boolean isPowerComponent() {
		return false;
	}
	
	public boolean canPowerIndirectly(IBlockState state, Direction dir) {
		return isSolid();
	}
	
	public boolean canConnectToWire(IBlockState state, Direction dir) {
		return isPowerComponent();
	}
	
	public int getOutputPowerFlags(IBlockState state, Direction dir) {
		return canPowerIndirectly(state, dir) ? IServerWorld.INDIRECT_POWER_FLAGS : IServerWorld.NO_FLAGS;
	}

	public int getPowerTo(IServerWorld world, IBlockPosition pos, IBlockState state, Direction dir, int powerFlags) {
		if (canPowerIndirectly(state, dir) && (powerFlags & IServerWorld.INDIRECT_POWER_FLAGS) != 0) {
			if ((powerFlags & IServerWorld.INDIRECT_WEAK_POWER_FLAG) != 0)
				return world.getPowerExceptFrom(pos, dir, IServerWorld.DIRECT_POWER_FLAGS);
			
			return world.getPowerExceptFrom(pos, dir, IServerWorld.DIRECT_STRONG_POWER_FLAG);
		}
		
		return 0;
	}
	
	public final String getName() {
		return name;
	}
	
	protected IBlockState createDefaultState() {
		return BlockState.createStateTree(this);
	}
	
	public IBlockState getDefaultState() {
		return defaultState;
	}
	
	public static final void registerBlocks() {
		if (blockRegistry != null)
			throw new IllegalStateException("Already registered blocks!");
		
		blockRegistry = new ReferenceRegsitry<>();
	
		registerBlock(AIR_BLOCK_ID             , new Block());
		registerBlock(DIRT_BLOCK_ID            , new DirtBlock());
		registerBlock(GRASS_BLOCK_ID           , new GrassBlock());
		registerBlock(PLANKS_BLOCK_ID          , new WoodPlanksBlock());
		registerBlock(STONE_BLOCK_ID           , new BasicSolidBlock(BlockTextures.STONE_TEXTURE));
		registerBlock(COBBLESTONE_BLOCK_ID     , new BasicSolidBlock(BlockTextures.COBBLESTONE_TEXTURE));
		registerBlock(PLANT_BLOCK_ID           , new PlantBlock());
		registerBlock(LEAVES_BLOCK_ID          , new LeavesBlock());
		registerBlock(LOG_BLOCK_ID             , new WoodLogBlock());
		registerBlock(REDSTONE_WIRE_BLOCK_ID   , new RedstoneWireBlock());
		registerBlock(REDSTONE_BLOCK_ID        , new RedstoneBlock());
		registerBlock(STONE_SLAB_BLOCK_ID      , new StoneSlabBlock());
		registerBlock(PLANKS_SLAB_BLOCK_ID     , new WoodPlanksSlabBlock());
	}
	
	private static void registerBlock(String name, Block block) {
		blockRegistry.register(name, block);
		
		block.name = name;
	}
	
	public static Block getBlock(String name) {
		if (blockRegistry == null)
			throw new IllegalStateException("Blocks are not yet registered!");
		
		Block block = blockRegistry.getElement(name);
		
		if (block == null)
			throw new NoSuchElementException("Block '" + name + "' is not registered.");

		return block;
	}
}
