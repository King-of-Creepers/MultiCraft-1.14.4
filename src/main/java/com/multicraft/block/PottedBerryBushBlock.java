package com.multicraft.block;

import com.multicraft.block.properties.BerryType;
import com.multicraft.block.properties.BerryTypeProperty;
import com.multicraft.block.properties.MulticraftBlockStateProperties;
import com.multicraft.registries.BlockRegistry;
import com.multicraft.registries.ItemRegistry;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class PottedBerryBushBlock extends Block implements IGrowable {
	
	public static final BooleanProperty IS_HARVESTED = MulticraftBlockStateProperties.IS_HARVESTED;
	public static final BerryTypeProperty BERRY_TYPE = MulticraftBlockStateProperties.BERRY_TYPE;
	
	protected static final VoxelShape SHAPE = Block.makeCuboidShape(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
	
	public PottedBerryBushBlock(Block.Properties properties) {
		super(properties);
		
		this.setDefaultState(this.stateContainer.getBaseState().with(BERRY_TYPE, BerryType.SWEET_BERRY_BUSH).with(IS_HARVESTED, Boolean.TRUE));
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return SHAPE;
		
	}
	
	@SuppressWarnings("deprecation")
	public void tick(BlockState state, World world, BlockPos pos, Random random) {
		super.tick(state, world, pos, random);
		
		if (random.nextInt(14) == 0 && world.getLightSubtracted(pos.up(), 0) >= 9) {
			
			this.grow(world, random, pos, state);
			
		}

	}
	
	public BlockRenderType getRenderType(BlockState state) {
		
		return BlockRenderType.MODEL;
		
	}
	
	@Override
	public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		
		if (!world.isRemote) {
			
			if (state.get(PottedBerryBushBlock.IS_HARVESTED) == Boolean.FALSE) {
				
				if (state.get(PottedBerryBushBlock.BERRY_TYPE) == BerryType.SWEET_BERRY_BUSH) {
					
					Block.spawnAsEntity(world, pos, new ItemStack(Items.SWEET_BERRIES, 1));
					world.playSound((PlayerEntity)null, pos, SoundEvents.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
					world.setBlockState(pos, BlockRegistry.POTTED_BERRY_BUSH.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, BerryType.SWEET_BERRY_BUSH).with(PottedBerryBushBlock.IS_HARVESTED, Boolean.TRUE));
					
				}
				
				if (state.get(PottedBerryBushBlock.BERRY_TYPE) == BerryType.BLUE_BERRY_BUSH) {
					
					Block.spawnAsEntity(world, pos, new ItemStack(ItemRegistry.BLUE_BERRIES, 1));
					world.playSound((PlayerEntity)null, pos, SoundEvents.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
					world.setBlockState(pos, BlockRegistry.POTTED_BERRY_BUSH.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, BerryType.BLUE_BERRY_BUSH).with(PottedBerryBushBlock.IS_HARVESTED, Boolean.TRUE));
					
				}
				
			}
			
		}
		
		return true;
		
	}
	
	public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
		
		return facing == Direction.DOWN && !stateIn.isValidPosition(worldIn, currentPos) ? Blocks.AIR.getDefaultState() : super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
		
	}
	
	public BlockRenderLayer getRenderLayer() {
		
		return BlockRenderLayer.CUTOUT;
		
	}
	
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		
		builder.add(BERRY_TYPE, IS_HARVESTED);
		
	}
	
	@Override
	public boolean canGrow(IBlockReader worldIn, BlockPos pos, BlockState state, boolean isClient) {
		
		return true;
		
	}
	
	@Override
	public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, BlockState state) {
		
		return true;
		
	}
	
	@Override
	public void grow(World world, Random rand, BlockPos pos, BlockState state) {
		
		if (!world.isRemote) {
			
			if (state.get(PottedBerryBushBlock.IS_HARVESTED) == Boolean.TRUE) {
				
				world.setBlockState(pos, BlockRegistry.POTTED_BERRY_BUSH.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, state.get(PottedBerryBushBlock.BERRY_TYPE)).with(PottedBerryBushBlock.IS_HARVESTED, Boolean.FALSE));
				
			}
			
			/**
			if (world.getBlockState(pos.down()).getBlock() instanceof HopperBlock) {
				
				ItemEntity itementity = new ItemEntity(world, (double)pos.getX() + 0.5, (double)pos.getY() - 0.3, (double)pos.getZ() + 0.5, new ItemStack(Items.SWEET_BERRIES));
				
				if (this.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, BerryType.SWEET_BERRY_BUSH) == state) {
					
					itementity = new ItemEntity(world, (double)pos.getX() + 0.5, (double)pos.getY() - 0.3, (double)pos.getZ() + 0.5, new ItemStack(Items.SWEET_BERRIES));
					
				}
				
				if (this.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, BerryType.BLUE_BERRY_BUSH) == state) {
					
					itementity = new ItemEntity(world, (double)pos.getX() + 0.5, (double)pos.getY() - 0.3, (double)pos.getZ() + 0.5, new ItemStack(ItemRegistry.BLUE_BERRIES));
					
				}
				
				itementity.setDefaultPickupDelay();
				world.addEntity(itementity);
				world.playSound((PlayerEntity)null, pos, SoundEvents.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
				world.setBlockState(pos, BlockRegistry.POTTED_BERRY_BUSH_HARVESTED.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, state.get(PottedBerryBushBlock.BERRY_TYPE)));
				
			} else {
				
				if (this.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, BerryType.SWEET_BERRY_BUSH) == state) {
					
					world.setBlockState(pos, BlockRegistry.POTTED_SWEET_BERRY_BUSH.getDefaultState());
					
				}
				
				if (this.getDefaultState().with(PottedBerryBushBlock.BERRY_TYPE, BerryType.BLUE_BERRY_BUSH) == state) {
					
					world.setBlockState(pos, BlockRegistry.POTTED_BLUE_BERRY_BUSH.getDefaultState());
					
				}
				
			}
			**/
		}
		
	}
	
}