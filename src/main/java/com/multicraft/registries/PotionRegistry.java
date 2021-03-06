package com.multicraft.registries;

import com.multicraft.Multicraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.*;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public final class PotionRegistry
{
	public static final DeferredRegister<Potion> POTIONS = new DeferredRegister<>(ForgeRegistries.POTION_TYPES, Multicraft.MODID);
	
	public static final RegistryObject<Potion> LEVITATION = POTIONS.register("levitation", () -> new Potion(new EffectInstance(Effects.LEVITATION, 300)));
	public static final RegistryObject<Potion> LONG_LEVITATION = POTIONS.register("long_levitation", () -> new Potion("levitation", new EffectInstance(Effects.LEVITATION, 600)));
	
	public static void registerBrewingRecipes()
	{
		final ItemStack LEVITATION_POTION = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Objects.requireNonNull(PotionRegistry.LEVITATION.get()));
		final ItemStack LONG_LEVITATION_POTION = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Objects.requireNonNull(PotionRegistry.LONG_LEVITATION.get()));
		
		PotionBrewing.addMix(Potions.AWKWARD, Objects.requireNonNull(ItemRegistry.BAT_WING.get()), PotionRegistry.LEVITATION.get());
		BrewingRecipeRegistry.addRecipe(Ingredient.fromStacks(LEVITATION_POTION), Ingredient.fromItems(Items.REDSTONE), LONG_LEVITATION_POTION);
	}
}
