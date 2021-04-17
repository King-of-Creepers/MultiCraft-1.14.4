package com.multiexpansion.block;

import java.util.Optional;
import java.util.Random;

import javax.annotation.Nullable;

import com.multiexpansion.particles.ColoredCampfireCosySmokeParticleData;
import com.multiexpansion.particles.ColoredCampfireSignalSmokeParticleData;
import com.multiexpansion.particles.ColoredCampfireSmokeParticleData;
import com.multiexpansion.tileentity.ColoredCampfireTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CampfireCookingRecipe;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.block.AbstractBlock.Properties;

public class ColoredCampfireBlock extends ContainerBlock implements IWaterLoggable {
	
	protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape VIRTUAL_FENCE_POST = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
	
	private final boolean isBlueFire;
	private final int damage;
	
	public ColoredCampfireBlock(boolean isBlueFire, int damage, Properties properties) {
		super(properties);
		
		this.isBlueFire = isBlueFire;
		this.damage = damage;
		this.registerDefaultState(this.getStateDefinition().any().setValue(LIT, Boolean.valueOf(true)).setValue(SIGNAL_FIRE, Boolean.valueOf(false)).setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(FACING, Direction.NORTH));
	
	}
	
	public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		
		TileEntity tileentity = worldIn.getBlockEntity(pos);
		
		if (tileentity instanceof ColoredCampfireTileEntity) {
			
			ColoredCampfireTileEntity campfiretileentity = (ColoredCampfireTileEntity)tileentity;
			ItemStack itemstack = player.getItemInHand(handIn);
			Optional<CampfireCookingRecipe> optional = campfiretileentity.findMatchingRecipe(itemstack);
			
			if (optional.isPresent()) {
				
				if (!worldIn.isClientSide && campfiretileentity.addItem(player.isCreative() ? itemstack.copy() : itemstack, optional.get().getCookingTime())) {
					
					player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
					return ActionResultType.SUCCESS;
					
				}
				
				return ActionResultType.CONSUME;
				
			}
			
		}
		
		return ActionResultType.PASS;
		
	}

	public void entityInside(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		
		if (!entityIn.fireImmune() && state.getValue(LIT) && entityIn instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity)entityIn)) {
			
			entityIn.hurt(DamageSource.IN_FIRE, (float)this.damage);
			
		}
		
		super.entityInside(state, worldIn, pos, entityIn);
		
	}

	public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		
		if (!state.is(newState.getBlock())) {
			
			TileEntity tileentity = worldIn.getBlockEntity(pos);
			
			if (tileentity instanceof ColoredCampfireTileEntity) {
				
				InventoryHelper.dropContents(worldIn, pos, ((ColoredCampfireTileEntity)tileentity).getInventory());
				
			}
			
			super.onRemove(state, worldIn, pos, newState, isMoving);
			
		}
		
	}
	
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		
		IWorld iworld = context.getLevel();
		BlockPos blockpos = context.getClickedPos();
		
		boolean flag = iworld.getFluidState(blockpos).getType() == Fluids.WATER;
		
		return this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(flag)).setValue(SIGNAL_FIRE, Boolean.valueOf(this.isHayBlock(iworld.getBlockState(blockpos.below())))).setValue(LIT, Boolean.valueOf(!flag)).setValue(FACING, context.getHorizontalDirection());
		
	}

	public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
		
		if (stateIn.getValue(WATERLOGGED)) {
			
			worldIn.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
			
		}
		
		return facing == Direction.DOWN ? stateIn.setValue(SIGNAL_FIRE, Boolean.valueOf(this.isHayBlock(facingState))) : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
		
	}
	
	private boolean isHayBlock(BlockState stateIn) {
		
		return stateIn.is(Blocks.HAY_BLOCK);
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return SHAPE;
		
	}
	
	public BlockRenderType getRenderShape(BlockState state) {
		
		return BlockRenderType.MODEL;
		
	}
	
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		
		if (stateIn.getValue(LIT)) {
			
			if (rand.nextInt(10) == 0) {
				
				worldIn.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, SoundEvents.CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.5F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.6F, false);
				
			}
			
			if (this.isBlueFire && rand.nextInt(5) == 0) {
				
				for(int i = 0; i < rand.nextInt(1) + 1; ++i) {
					
					worldIn.addParticle(ParticleTypes.LAVA, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, (double)(rand.nextFloat() / 2.0), 5.0E-5D, (double)(rand.nextFloat() / 2.0));
					
				}
				
			}
			
		}
		
	}
	
	public static void dowse(IWorld IworldIn, BlockPos pos, BlockState blockState) {
		
		if (IworldIn.isClientSide()) {
			
			for (int i = 0; i < 20; ++i) {
				
				spawnSmokeParticles((World)IworldIn, pos, blockState.getValue(SIGNAL_FIRE), true);
				
			}
			
		}
		
		TileEntity tileentity = IworldIn.getBlockEntity(pos);
		
		if (tileentity instanceof ColoredCampfireTileEntity) {
			
			((ColoredCampfireTileEntity)tileentity).dropAllItems();
			
		}
		
	}
	
	public boolean placeLiquid(IWorld worldIn, BlockPos pos, BlockState state, FluidState fluidStateIn) {
		
		if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidStateIn.getType() == Fluids.WATER) {
			
			boolean flag = state.getValue(LIT);
			
			if (flag) {
				
				if (!worldIn.isClientSide()) {
					
					worldIn.playSound((PlayerEntity)null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
					
				}
				
				dowse(worldIn, pos, state);
				
			}
			
			worldIn.setBlock(pos, state.setValue(WATERLOGGED, Boolean.valueOf(true)).setValue(LIT, Boolean.valueOf(false)), 3);
			worldIn.getLiquidTicks().scheduleTick(pos, fluidStateIn.getType(), fluidStateIn.getType().getTickDelay(worldIn));
			
			return true;
			
		} else {
			
			return false;
			
		}
		
	}
	
	public void onProjectileHit(World worldIn, BlockState state, BlockRayTraceResult hit, ProjectileEntity projectile) {
		
		if (!worldIn.isClientSide && projectile.isOnFire()) {
			
			Entity entity = projectile.getOwner();
			boolean flag = entity == null || entity instanceof PlayerEntity || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(worldIn, entity);
			
			if (flag && !state.getValue(LIT) && !state.getValue(WATERLOGGED)) {
				
				BlockPos blockpos = hit.getBlockPos();
				worldIn.setBlock(blockpos, state.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
				
			}
			
		}
		
	}
	
	public static void spawnSmokeParticles(World world, BlockPos pos, boolean isSignalFire, boolean spawnExtraSmoke) {
		
		Random random = world.getRandom();
		
		if (!spawnColoredParticle(world, world.getBlockState(pos.below()).getBlock(), pos, random)) {
			
			BasicParticleType basicparticletype = isSignalFire ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
			world.addParticle(basicparticletype, true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
			
		}
		
		if (spawnExtraSmoke) {
			
			world.addParticle(ParticleTypes.SMOKE, (double)pos.getX() + 0.25D + random.nextDouble() / 2.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + 0.4D, (double)pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.005D, 0.0D);
			
		}
		
	}
	
	public static boolean spawnColoredParticle(World world, Block block, BlockPos pos, Random random) {
		
		CampfireParticlePlaceholder particle = getParticleResultFromBlock(block, random);
		
		if (particle != null) {
			
			if (isWool(block)) {
				
				world.addParticle(new ColoredCampfireCosySmokeParticleData(particle.getRed(), particle.getGreen(), particle.getBlue(), particle.getAlpha()), true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
				
				return true;
				
			} else if (isStainedGlass(block)) {
				
				if (random.nextFloat() < 0.2F) {
					
					world.addParticle(new ColoredCampfireSignalSmokeParticleData(particle.getRed(), particle.getGreen(), particle.getBlue(), particle.getAlpha()), true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
					
				}
				
				return true;
				
			} else if (isConcrete(block)) {
				
				world.addParticle(new ColoredCampfireSignalSmokeParticleData(particle.getRed(), particle.getGreen(), particle.getBlue(), particle.getAlpha()), true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
				
				return true;
				
			} else if (isConcretePowder(block)) {
				
				if (random.nextFloat() < 0.2F) {
					
					world.addParticle(new ColoredCampfireCosySmokeParticleData(particle.getRed(), particle.getGreen(), particle.getBlue(), particle.getAlpha()), true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
					
				}
				
				return true;
				
			} else {
				
				world.addParticle(new ColoredCampfireSignalSmokeParticleData(particle.getRed(), particle.getGreen(), particle.getBlue(), particle.getAlpha()), true, (double)pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), (double)pos.getY() + random.nextDouble() + random.nextDouble(), (double)pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double)(random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
				return true;
				
			}
 			
		} else {
			
			return false;
			
		}
		
	}
	
	public static boolean isWool(Block block) {
		
		return (block == Blocks.WHITE_WOOL || block == Blocks.ORANGE_WOOL || block == Blocks.MAGENTA_WOOL || block == Blocks.LIGHT_BLUE_WOOL || block == Blocks.YELLOW_WOOL || block == Blocks.LIME_WOOL || block == Blocks.PINK_WOOL || block == Blocks.GRAY_WOOL || block == Blocks.LIGHT_GRAY_WOOL || block == Blocks.CYAN_WOOL || block == Blocks.PURPLE_WOOL || block == Blocks.BLUE_WOOL || block == Blocks.BROWN_WOOL || block == Blocks.GREEN_WOOL || block == Blocks.RED_WOOL || block == Blocks.BLACK_WOOL);
		
	}
	
	public static boolean isStainedGlass(Block block) {
		
		return (block == Blocks.WHITE_STAINED_GLASS || block == Blocks.ORANGE_STAINED_GLASS || block == Blocks.MAGENTA_STAINED_GLASS || block == Blocks.LIGHT_BLUE_STAINED_GLASS || block == Blocks.YELLOW_STAINED_GLASS || block == Blocks.LIME_STAINED_GLASS || block == Blocks.PINK_STAINED_GLASS || block == Blocks.GRAY_STAINED_GLASS || block == Blocks.LIGHT_GRAY_STAINED_GLASS || block == Blocks.CYAN_STAINED_GLASS || block == Blocks.PURPLE_STAINED_GLASS || block == Blocks.BLUE_STAINED_GLASS || block == Blocks.BROWN_STAINED_GLASS || block == Blocks.GREEN_STAINED_GLASS || block == Blocks.RED_STAINED_GLASS || block == Blocks.BLACK_STAINED_GLASS);
		
	}
	
	public static boolean isConcrete(Block block) {
		
		return (block == Blocks.WHITE_CONCRETE || block == Blocks.ORANGE_CONCRETE || block == Blocks.MAGENTA_CONCRETE || block == Blocks.LIGHT_BLUE_CONCRETE || block == Blocks.YELLOW_CONCRETE || block == Blocks.LIME_CONCRETE || block == Blocks.PINK_CONCRETE || block == Blocks.GRAY_CONCRETE || block == Blocks.LIGHT_GRAY_CONCRETE || block == Blocks.CYAN_CONCRETE || block == Blocks.PURPLE_CONCRETE || block == Blocks.BLUE_CONCRETE || block == Blocks.BROWN_CONCRETE || block == Blocks.GREEN_CONCRETE || block == Blocks.RED_CONCRETE || block == Blocks.BLACK_CONCRETE);
		
	}
	
	public static boolean isConcretePowder(Block block) {
		
		return (block == Blocks.WHITE_CONCRETE_POWDER || block == Blocks.ORANGE_CONCRETE_POWDER || block == Blocks.MAGENTA_CONCRETE_POWDER || block == Blocks.LIGHT_BLUE_CONCRETE_POWDER || block == Blocks.YELLOW_CONCRETE_POWDER || block == Blocks.LIME_CONCRETE_POWDER || block == Blocks.PINK_CONCRETE_POWDER || block == Blocks.GRAY_CONCRETE_POWDER || block == Blocks.LIGHT_GRAY_CONCRETE_POWDER || block == Blocks.CYAN_CONCRETE_POWDER || block == Blocks.PURPLE_CONCRETE_POWDER || block == Blocks.BLUE_CONCRETE_POWDER || block == Blocks.BROWN_CONCRETE_POWDER || block == Blocks.GREEN_CONCRETE_POWDER || block == Blocks.RED_CONCRETE_POWDER || block == Blocks.BLACK_CONCRETE_POWDER);
		
	}
	
	public static CampfireParticlePlaceholder getParticleResultFromBlock(Block block, Random random) {
		
		if (block == Blocks.WHITE_WOOL || block == Blocks.WHITE_STAINED_GLASS || block == Blocks.WHITE_CONCRETE || block == Blocks.WHITE_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(246, 246, 246, 0);
			
		} else if (block == Blocks.ORANGE_WOOL || block == Blocks.ORANGE_STAINED_GLASS || block == Blocks.ORANGE_CONCRETE || block == Blocks.ORANGE_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(229, 128, 25, 0);
			
		} else if (block == Blocks.MAGENTA_WOOL || block == Blocks.MAGENTA_STAINED_GLASS || block == Blocks.MAGENTA_CONCRETE || block == Blocks.MAGENTA_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(194, 78, 185, 0);
			
		} else if (block == Blocks.LIGHT_BLUE_WOOL || block == Blocks.LIGHT_BLUE_STAINED_GLASS || block == Blocks.LIGHT_BLUE_CONCRETE || block == Blocks.LIGHT_BLUE_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(89, 205, 230, 0);
			
		} else if (block == Blocks.YELLOW_WOOL || block == Blocks.YELLOW_STAINED_GLASS || block == Blocks.YELLOW_CONCRETE || block == Blocks.YELLOW_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(243, 212, 58, 0);
			
		} else if (block == Blocks.LIME_WOOL || block == Blocks.LIME_STAINED_GLASS || block == Blocks.LIME_CONCRETE || block == Blocks.LIME_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(134, 204, 45, 0);
			
		} else if (block == Blocks.PINK_WOOL || block == Blocks.PINK_STAINED_GLASS || block == Blocks.PINK_CONCRETE || block == Blocks.PINK_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(246, 182, 206, 0);
			
		} else if (block == Blocks.GRAY_WOOL || block == Blocks.GRAY_STAINED_GLASS || block == Blocks.GRAY_CONCRETE || block == Blocks.GRAY_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(87, 93, 98, 0);
			
		} else if (block == Blocks.LIGHT_GRAY_WOOL || block == Blocks.LIGHT_GRAY_STAINED_GLASS || block == Blocks.LIGHT_GRAY_CONCRETE || block == Blocks.LIGHT_GRAY_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(157, 157, 151, 0);
			
		} else if (block == Blocks.CYAN_WOOL || block == Blocks.CYAN_STAINED_GLASS || block == Blocks.CYAN_CONCRETE || block == Blocks.CYAN_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(37, 145, 155, 0);
			
		} else if (block == Blocks.PURPLE_WOOL || block == Blocks.PURPLE_STAINED_GLASS || block == Blocks.PURPLE_CONCRETE || block == Blocks.PURPLE_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(122, 50, 170, 0);
			
		} else if (block == Blocks.BLUE_WOOL || block == Blocks.BLUE_STAINED_GLASS || block == Blocks.BLUE_CONCRETE || block == Blocks.BLUE_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(69, 70, 163, 0);
			
		} else if (block == Blocks.BROWN_WOOL || block == Blocks.BROWN_STAINED_GLASS || block == Blocks.BROWN_CONCRETE || block == Blocks.BROWN_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(147, 101, 67, 0);
			
		} else if (block == Blocks.GREEN_WOOL || block == Blocks.GREEN_STAINED_GLASS || block == Blocks.GREEN_CONCRETE || block == Blocks.GREEN_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(93, 113, 48, 0);
			
		} else if (block == Blocks.RED_WOOL || block == Blocks.RED_STAINED_GLASS || block == Blocks.RED_CONCRETE || block == Blocks.RED_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(178, 57, 52, 0);
			
		} else if (block == Blocks.BLACK_WOOL || block == Blocks.BLACK_STAINED_GLASS || block == Blocks.BLACK_CONCRETE || block == Blocks.BLACK_CONCRETE_POWDER) {
			
			return new CampfireParticlePlaceholder(28, 28, 28, 0);
			
		} else if (block == Blocks.TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(155, 96, 69, 0);
			
		} else if (block == Blocks.WHITE_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(210, 177, 161, 0);
			
		} else if (block == Blocks.ORANGE_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(168, 89, 42, 0);
			
		} else if (block == Blocks.MAGENTA_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(148, 86, 108, 0);
			
		} else if (block == Blocks.LIGHT_BLUE_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(114, 108, 138, 0);
			
		} else if (block == Blocks.YELLOW_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(190, 137, 38, 0);
			
		} else if (block == Blocks.LIME_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(103, 119, 54, 0);
			
		} else if (block == Blocks.PINK_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(166, 82, 82, 0);
			
		} else if (block == Blocks.GRAY_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(60, 45, 36, 0);
			
		} else if (block == Blocks.LIGHT_GRAY_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(139, 110, 100, 0);
			
		} else if (block == Blocks.CYAN_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(85, 90, 90, 0);
			
		} else if (block == Blocks.PURPLE_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(116, 69, 85, 0);
			
		} else if (block == Blocks.BLUE_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(73, 59, 91, 0);
			
		} else if (block == Blocks.BROWN_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(61, 44, 32, 0);
			
		} else if (block == Blocks.GREEN_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(79, 87, 46, 0);
			
		} else if (block == Blocks.RED_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(145, 63, 48, 0);
			
		} else if (block == Blocks.BLACK_TERRACOTTA) {
			
			return new CampfireParticlePlaceholder(39, 24, 18, 0);
			
		} else if (block == Blocks.WHITE_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.2F) {
				
				return new CampfireParticlePlaceholder(243, 212, 58, 0);
				
			} else if (chance <= 0.4F) {
				
				return new CampfireParticlePlaceholder(89, 205, 230, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(246, 246, 246, 0);
				
			}
			
		} else if (block == Blocks.ORANGE_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.05F) {
				
				return new CampfireParticlePlaceholder(246, 246, 246, 0);
				
			} else if (chance <= 0.4F) {
				
				return new CampfireParticlePlaceholder(37, 145, 155, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(229, 128, 25, 0);
				
			}
			
		} else if (block == Blocks.MAGENTA_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.15F) {
				
				return new CampfireParticlePlaceholder(246, 182, 206, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(194, 78, 185, 0);
				
			}
			
		} else if (block == Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.25F) {
					
					return new CampfireParticlePlaceholder(69, 70, 163, 0);
					
			} else if (chance <= 0.3F) {
				
				return new CampfireParticlePlaceholder(37, 145, 155, 0);
				
			} else if (chance <= 0.31F) {
				
				return new CampfireParticlePlaceholder(246, 246, 246, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(89, 205, 230, 0);
				
			}
			
		} else if (block == Blocks.YELLOW_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.15F) {
				
				return new CampfireParticlePlaceholder(147, 101, 67, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(243, 212, 58, 0);
				
			}
			
		} else if (block == Blocks.LIME_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.25F) {
				
				return new CampfireParticlePlaceholder(243, 212, 58, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(134, 204, 45, 0);
				
			}
			
		} else if (block == Blocks.PINK_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.05F) {
				
				return new CampfireParticlePlaceholder(157, 157, 151, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(246, 182, 206, 0);
				
			}
			
		} else if (block == Blocks.GRAY_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.15F) {
				
				return new CampfireParticlePlaceholder(157, 157, 151, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(87, 93, 98, 0);
				
			}
			
		} else if (block == Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.05F) {
				
				return new CampfireParticlePlaceholder(246, 246, 246, 0);
				
			} else if (chance <= 0.1F) {
				
				return new CampfireParticlePlaceholder(87, 93, 98, 0);
				
			} else if (chance <= 0.15F) {
				
				return new CampfireParticlePlaceholder(37, 145, 155, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(157, 157, 151, 0);
				
			}
			
		} else if (block == Blocks.CYAN_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.15F) {
				
				return new CampfireParticlePlaceholder(246, 246, 246, 0);
				
			} else if (chance <= 0.3F) {
				
				return new CampfireParticlePlaceholder(89, 205, 230, 0);
				
			} else if (chance <= 0.4F) {
				
				return new CampfireParticlePlaceholder(87, 93, 98, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(37, 145, 155, 0);
				
			}
			
		} else if (block == Blocks.PURPLE_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.2F) {
				
				return new CampfireParticlePlaceholder(28, 28, 28, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(122, 50, 170, 0);
				
			}
			
		} else if (block == Blocks.BLUE_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.1F) {
				
				return new CampfireParticlePlaceholder(28, 28, 28, 0);
				
			} else if (chance <= 0.3F) {
				
				return new CampfireParticlePlaceholder(89, 205, 230, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(69, 70, 163, 0);
				
			}
			
		} else if (block == Blocks.BROWN_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.3F) {
				
				return new CampfireParticlePlaceholder(37, 145, 155, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(147, 101, 67, 0);
				
			}
			
		} else if (block == Blocks.GREEN_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.2F) {
				
				return new CampfireParticlePlaceholder(246, 246, 246, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(93, 113, 48, 0);
				
			}
			
		} else if (block == Blocks.RED_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.05F) {
				
				return new CampfireParticlePlaceholder(246, 182, 206, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(178, 57, 52, 0);
				
			}
			
		} else if (block == Blocks.BLACK_GLAZED_TERRACOTTA) {
			
			float chance = random.nextFloat();
			
			if (chance <= 0.35F) {
				
				return new CampfireParticlePlaceholder(87, 93, 98, 0);
				
			} else if (chance <= 0.45F) {
				
				return new CampfireParticlePlaceholder(178, 57, 52, 0);
				
			} else {
				
				return new CampfireParticlePlaceholder(28, 28, 28, 0);
				
			}
			
		} else {
			
			return null;
			
		}
		
		/**
		 * if (block == Blocks.WHITE_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(1.0, 1.0, 1.0, 0);
			
		} else if (block == Blocks.ORANGE_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.91F, 0.67F, 0.32F, 0);
			
		} else if (block == Blocks.MAGENTA_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.88F, 0.56F, 0.85F, 0);
			
		} else if (block == Blocks.LIGHT_BLUE_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.58F, 0.72F, 0.91F, 0);
			
		} else if (block == Blocks.YELLOW_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.91F, 0.91F, 0.3F, 0);
			
		} else if (block == Blocks.LIME_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.58F, 0.85F, 0.25F, 0);
			
		} else if (block == Blocks.PINK_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.94F, 0.7F, 0.82F, 0);
			
		} else if (block == Blocks.GRAY_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.54F, 0.54F, 0.54F, 0);
			
		} else if (block == Blocks.LIGHT_GRAY_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.81F, 0.81F, 0.81F, 0);
			
		} else if (block == Blocks.CYAN_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.22F, 0.48F, 0.58F, 0);
			
		} else if (block == Blocks.PURPLE_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.74F, 0.48F, 0.86F, 0);
			
		} else if (block == Blocks.BLUE_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.23F, 0.42F, 0.7F, 0);
			
		} else if (block == Blocks.BROWN_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.61F, 0.44F, 0.32F, 0);
			
		} else if (block == Blocks.GREEN_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.39F, 0.5F, 0.21F, 0);
			
		} else if (block == Blocks.RED_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.7F, 0.25F, 0.25F, 0);
			
		} else if (block == Blocks.BLACK_WOOL) {
			
			return new ColoredCampfireSmokeParticleData(0.23F, 0.23F, 0.23F, 0);
			
		}

		 what the hell?
		 -Laz

		 */
		
	}
	
	public static boolean isSmokeyPos(World worldIn, BlockPos pos) {
		
		for (int i = 1; i <= 5; ++i) {
			
			BlockPos blockpos = pos.below(i);
			BlockState blockstate = worldIn.getBlockState(blockpos);
			
			if (isLitCampfire(blockstate)) {
				
				return true;
				
			}
			
			boolean flag = VoxelShapes.joinIsNotEmpty(VIRTUAL_FENCE_POST, blockstate.getCollisionShape(worldIn, pos, ISelectionContext.empty()), IBooleanFunction.AND);
			
			if (flag) {
				
				BlockState blockstate1 = worldIn.getBlockState(blockpos.below());
				
				return isLitCampfire(blockstate1);
				
			}
			
		}
		
		return false;
		
	}
	
	public static boolean isLitCampfire(BlockState p_226915_0_) {
		
		return p_226915_0_.hasProperty(LIT) && p_226915_0_.is(BlockTags.CAMPFIRES) && p_226915_0_.getValue(LIT);
		
	}
	
	@SuppressWarnings("deprecation")
	public FluidState getFluidState(BlockState state) {
		
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
		
	}
	
	public BlockState rotate(BlockState state, Rotation rot) {
		
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
		
	}
	
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
		
	}
	
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		
		builder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
		
	}
	
	public TileEntity newBlockEntity(IBlockReader worldIn) {
		
		return new ColoredCampfireTileEntity();
		
	}
	
	public boolean isPathfindable(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		
		return false;
		
	}
	
	public static boolean canLight(BlockState p_241470_0_) {
		
		return p_241470_0_.is(BlockTags.CAMPFIRES, (p_241469_0_) -> {
			
			return p_241469_0_.hasProperty(BlockStateProperties.WATERLOGGED) && p_241469_0_.hasProperty(BlockStateProperties.LIT);
			
		}) && !p_241470_0_.getValue(BlockStateProperties.WATERLOGGED) && !p_241470_0_.getValue(BlockStateProperties.LIT);
		
	}
	
	public static class CampfireParticlePlaceholder {
		
		private float red;
		private float green;
		private float blue;
		private float alpha;
		
		public CampfireParticlePlaceholder(int red, int green, int blue, float alpha) {
			
			this.red = red / 255F;
			this.green = green / 255F;
			this.blue = blue / 255F;
			this.alpha = alpha;
			
		}
		
		public float getRed() {
			
			return red;
			
		}
		
		public float getGreen() {
			
			return green;
			
		}
		
		public float getBlue() {
			
			return blue;
			
		}
		
		public float getAlpha() {
			
			return alpha;
			
		}
		
	}
	
}