package com.multicraft.registries;

import com.multicraft.Multicraft;

import net.minecraft.item.BlockNamedItem;
import net.minecraft.item.Foods;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ItemRegistry {
	
	public static Item CREATIVE_TAB_ITEM;
	
	public static Item BLUE_BERRIES;
	
	public static Item OAK_BARK;
	public static Item SPRUCE_BARK;
	public static Item BIRCH_BARK;
	public static Item JUNGLE_BARK;
	public static Item ACACIA_BARK;
	public static Item DARK_OAK_BARK;
	
	@SubscribeEvent
	public static void registerItems(final RegistryEvent.Register<Item> itemRegistryEvent) {
		
		itemRegistryEvent.getRegistry().registerAll(
				
				//BLUEBERRY_BUSH = new BlockItem(BlockRegistry.BLUEBERRY_BUSH, new Item.Properties().group(ItemGroup.BUILDING_BLOCKS)).setRegistryName(BlockRegistry.BLUEBERRY_BUSH.getRegistryName()),
				CREATIVE_TAB_ITEM = new Item((new Item.Properties())).setRegistryName(Multicraft.multicraftLocation("creative_tab_icon")),
				BLUE_BERRIES = new BlockNamedItem(BlockRegistry.BLUE_BERRY_BUSH, (new Item.Properties()).group(Multicraft.MULTICRAFT).food(Foods.SWEET_BERRIES)).setRegistryName(Multicraft.multicraftLocation("item_blue_berries")),
				OAK_BARK = new Item((new Item.Properties()).group(Multicraft.MULTICRAFT)).setRegistryName(Multicraft.multicraftLocation("item_bark_oak")),
				SPRUCE_BARK = new Item((new Item.Properties()).group(Multicraft.MULTICRAFT)).setRegistryName(Multicraft.multicraftLocation("item_bark_spruce")),
				BIRCH_BARK = new Item((new Item.Properties()).group(Multicraft.MULTICRAFT)).setRegistryName(Multicraft.multicraftLocation("item_bark_birch")),
				JUNGLE_BARK = new Item((new Item.Properties()).group(Multicraft.MULTICRAFT)).setRegistryName(Multicraft.multicraftLocation("item_bark_jungle")),
				ACACIA_BARK = new Item((new Item.Properties()).group(Multicraft.MULTICRAFT)).setRegistryName(Multicraft.multicraftLocation("item_bark_acacia")),
				DARK_OAK_BARK = new Item((new Item.Properties()).group(Multicraft.MULTICRAFT)).setRegistryName(Multicraft.multicraftLocation("item_bark_dark_oak"))
				
		);
		
	}
	
}