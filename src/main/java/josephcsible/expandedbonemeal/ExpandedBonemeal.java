/*
ExpandedBonemeal Minecraft Mod
Copyright (C) 2016 Joseph C. Sible

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package josephcsible.expandedbonemeal;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.BonemealEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ExpandedBonemeal.MODID, version = ExpandedBonemeal.VERSION, guiFactory = "josephcsible.expandedbonemeal.ExpandedBonemealGuiFactory")
public class ExpandedBonemeal
{
	// XXX duplication with mcmod.info and build.gradle
	public static final String MODID = "expandedbonemeal";
	public static final String VERSION = "1.0.0";

	public static Configuration config;
	protected static boolean cactus, sugarcane, netherWart, melon, pumpkin, vine, lilyPad, deadBush, flowers, moss;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		syncConfig();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(this); // for onBonemeal
		FMLCommonHandler.instance().bus().register(this); // for onConfigChanged
	}

	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent eventArgs) {
		if(eventArgs.modID.equals(MODID))
			syncConfig();
	}

	protected static void syncConfig() {
		cactus = config.get(Configuration.CATEGORY_GENERAL, "cactus", true, "Whether using bone meal on a cactus should immediately attempt to add an additional block").getBoolean();
		sugarcane = config.get(Configuration.CATEGORY_GENERAL, "sugarcane", true, "Whether using bone meal on sugar canes should immediately attempt to add an additional block").getBoolean();
		netherWart = config.get(Configuration.CATEGORY_GENERAL, "netherWart", true, "Whether using bone meal on nether wart should immediately advance it to the next growth stage").getBoolean();
		melon = config.get(Configuration.CATEGORY_GENERAL, "melon", true, "Whether using bone meal on a mature melon stem should immediately attempt to grow a melon").getBoolean();
		pumpkin = config.get(Configuration.CATEGORY_GENERAL, "pumpkin", true, "Whether using bone meal on a mature pumpkin stem should immediately attempt to grow a pumpkin").getBoolean();
		vine = config.get(Configuration.CATEGORY_GENERAL, "vine", true, "Whether using bone meal on a vine should immediately attempt to grow").getBoolean();
		lilyPad = config.get(Configuration.CATEGORY_GENERAL, "lilyPad", true, "Whether using bone meal on a lily pad should cause one to drop as an item").getBoolean();
		deadBush = config.get(Configuration.CATEGORY_GENERAL, "deadBush", true, "Whether using bone meal on sand or hardened clay should grow a dead bush").getBoolean();
		flowers = config.get(Configuration.CATEGORY_GENERAL, "flowers", true, "Whether using bone meal on flowers should cause one to drop as an item").getBoolean();
		moss = config.get(Configuration.CATEGORY_GENERAL, "moss", true, "Whether using bone meal on cobblestone or stone bricks should cause moss to grow").getBoolean();
		if(config.hasChanged())
			config.save();
	}

	protected static void growCactusOrSugarcane(BonemealEvent event) {
		int y = event.y;
		// if the player uses bonemeal on the bottom of a 2-high plant, do what they meant
		while(event.world.getBlock(event.x, y + 1, event.z) == event.block) {
			++y;
		}
		// don't bother even to notify the client here, since it's going to change again during updateTick
		event.world.setBlockMetadataWithNotify(event.x, y, event.z, 15, 4);
		event.block.updateTick(event.world, event.x, y, event.z, event.world.rand);
		event.setResult(Result.ALLOW);
	}

	protected static void growMelonOrPumpkin(BonemealEvent event) {
		// if the metadata isn't 7, vanilla already does what we want
		if(event.world.getBlockMetadata(event.x, event.y, event.z) == 7) {
			if(!event.world.isRemote) {
				// BlockStem.updateTick generates one random number (with a varying maximum),
				// and if it's zero, then it grows or makes a melon/pumpkin. rather than coremod or
				// reimplement its growth code, just make sure it always gets zero.
				// later it's used to pick a direction, so go back to real random for that.
				event.block.updateTick(event.world, event.x, event.y, event.z, new ZeroFirstIntRandom());
			}
			event.setResult(Result.ALLOW);
		}
	}

	// doesn't cause a block update, as nothing that calls it needs one
	protected static void changeMetadataFromTo(BonemealEvent event, int from, int to) {
		if(event.world.getBlockMetadata(event.x, event.y, event.z) == from) {
			event.world.setBlockMetadataWithNotify(event.x, event.y, event.z, to, 2);
			event.setResult(Result.ALLOW);
		}
	}

	@SubscribeEvent
	public void onBonemeal(BonemealEvent event) {
		// in the future, maybe add stuff for saplings, mushrooms, ferns, and dirt variants
		if(event.block == Blocks.cactus) {
			if(cactus) growCactusOrSugarcane(event);
		} else if(event.block == Blocks.reeds) {
			if(sugarcane) growCactusOrSugarcane(event);
		} else if(event.block == Blocks.nether_wart) {
			if(netherWart) {
				int meta = event.world.getBlockMetadata(event.x, event.y, event.z);
				if(meta < 3) {
					event.world.setBlockMetadataWithNotify(event.x, event.y, event.z, meta + 1, 2);
					event.setResult(Result.ALLOW);
				}
			}
		} else if(event.block == Blocks.melon_stem) {
			if(melon) growMelonOrPumpkin(event);
		} else if(event.block == Blocks.pumpkin_stem) {
			if(pumpkin) growMelonOrPumpkin(event);
		} else if(event.block == Blocks.vine) {
			if(vine) {
				// immediately try to grow new vines
				Random rand = event.world.rand;
				event.world.rand = new ZeroFirstIntRandom();
				event.block.updateTick(event.world, event.x, event.y, event.z, rand);
				event.world.rand = rand;
				event.setResult(Result.ALLOW);
			}
		} else if(event.block == Blocks.waterlily) {
			if(lilyPad) {
				// drop as an item without removing the block, like sunflowers do in vanilla
				event.block.dropBlockAsItem(event.world, event.x, event.y, event.z, event.world.getBlockMetadata(event.x, event.y, event.z), 0);
				event.setResult(Result.ALLOW);
			}
		} else if(event.block == Blocks.sand || event.block == Blocks.hardened_clay || event.block == Blocks.stained_hardened_clay) { // intentionally excluding dirt, as I have other future plans in mind for it
			if(deadBush && event.world.isAirBlock(event.x, event.y + 1, event.z)) {
				event.world.setBlock(event.x, event.y + 1, event.z, Blocks.deadbush);
				event.setResult(Result.ALLOW);
			}
		} else if(event.block == Blocks.red_flower || event.block == Blocks.yellow_flower) {
			if(flowers) {
				// drop as an item without removing the block, like sunflowers do in vanilla
				event.block.dropBlockAsItem(event.world, event.x, event.y, event.z, event.world.getBlockMetadata(event.x, event.y, event.z), 0);
				event.setResult(Result.ALLOW);
			}
		} else if(moss) {
			if(event.block == Blocks.cobblestone) {
				event.world.setBlock(event.x, event.y, event.z, Blocks.mossy_cobblestone, 0, 2);
				event.setResult(Result.ALLOW);
			} else if(event.block == Blocks.cobblestone_wall || event.block == Blocks.stonebrick) {
				changeMetadataFromTo(event, 0, 1);
			} else if(event.block == Blocks.monster_egg) {
				changeMetadataFromTo(event, 2, 3);
			}
		}
	}
}
