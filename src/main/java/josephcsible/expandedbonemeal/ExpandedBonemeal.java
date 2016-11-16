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

import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockChorusFlower;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.BlockWall;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ExpandedBonemeal.MODID, version = ExpandedBonemeal.VERSION, acceptedMinecraftVersions = "[1.9,)", guiFactory = "josephcsible.expandedbonemeal.ExpandedBonemealGuiFactory")
public class ExpandedBonemeal
{
	// XXX duplication with mcmod.info and build.gradle
	public static final String MODID = "expandedbonemeal";
	public static final String VERSION = "1.0.0";

	public static Configuration config;
	protected static boolean cactus, sugarcane, netherWart, melon, pumpkin, vine, lilyPad, deadBush, flowers, chorusFlower, moss;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		syncConfig();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent eventArgs) {
		if(eventArgs.getModID().equals(MODID))
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
		chorusFlower = config.get(Configuration.CATEGORY_GENERAL, "chorusFlower", true, "Whether using bone meal on a chorus flower should immediately attempt to grow").getBoolean();
		moss = config.get(Configuration.CATEGORY_GENERAL, "moss", true, "Whether using bone meal on cobblestone or stone bricks should cause moss to grow").getBoolean();
		if(config.hasChanged())
			config.save();
	}

	protected static void growCactusOrSugarcane(BonemealEvent event, Block block, World world, PropertyInteger ageProperty) {
		BlockPos pos = event.getPos();
		// if the player uses bonemeal on the bottom of a 2-high plant, do what they meant
		while(world.getBlockState(pos.up()).getBlock() == block) {
			pos = pos.up();
		}
		IBlockState state = world.getBlockState(pos).withProperty(ageProperty, 15);
		// don't bother even to notify the client here, since it's going to change again during updateTick
		world.setBlockState(pos, state, 4);
		block.updateTick(world, pos, state, world.rand);
		event.setResult(Result.ALLOW);
	}

	protected static void growMelonOrPumpkin(BonemealEvent event) {
		// if the age isn't 7, vanilla already does what we want
		if(event.getBlock().getValue(BlockStem.AGE) == 7) {
			if(!event.getWorld().isRemote) {
				// BlockStem.updateTick generates one random number (with a varying maximum),
				// and if it's zero, then it grows or makes a melon/pumpkin. rather than coremod or
				// reimplement its growth code, just make sure it always gets zero.
				// later it's used to pick a direction, so go back to real random for that.
				event.getBlock().getBlock().updateTick(event.getWorld(), event.getPos(), event.getBlock(), new ZeroFirstIntRandom());
			}
			event.setResult(Result.ALLOW);
		}
	}

	// doesn't cause a block update, as nothing that calls it needs one
	protected static <T extends Comparable<T>> void changePropertyFromTo(BonemealEvent event, IProperty<T> property, T from, T to) {
		if (event.getBlock().getValue(property) == from) {
			event.getWorld().setBlockState(event.getPos(), event.getBlock().withProperty(property, to), 2);
			event.setResult(Result.ALLOW);
		}
	}

	@SubscribeEvent
	public void onBonemeal(BonemealEvent event) {
		IBlockState state = event.getBlock();
		Block block = state.getBlock();
		World world = event.getWorld();
		// in the future, maybe add stuff for sponges, saplings, mushrooms, ferns, and dirt variants
		if(block == Blocks.CACTUS) {
			if(cactus) growCactusOrSugarcane(event, block, world, BlockCactus.AGE);
		} else if(block == Blocks.REEDS) {
			if(sugarcane) growCactusOrSugarcane(event, block, world, BlockReed.AGE);
		} else if(block == Blocks.NETHER_WART) {
			if(netherWart) {
				int age = state.getValue(BlockNetherWart.AGE);
				if(age < 3) {
					world.setBlockState(event.getPos(), state.withProperty(BlockNetherWart.AGE, age + 1), 2);
					event.setResult(Result.ALLOW);
				}
			}
		} else if(block == Blocks.MELON_STEM) {
			if(melon) growMelonOrPumpkin(event);
		} else if(block == Blocks.PUMPKIN_STEM) {
			if(pumpkin) growMelonOrPumpkin(event);
		} else if(block == Blocks.VINE) {
			if(vine) {
				// immediately try to grow new vines. only tries if the first randInt is 0, so make sure that's the case
				Random rand = world.rand;
				world.rand = new ZeroFirstIntRandom();
				block.updateTick(world, event.getPos(), state, rand);
				world.rand = rand;
				event.setResult(Result.ALLOW);
			}
		} else if(block == Blocks.WATERLILY) {
			if(lilyPad) {
				// drop as an item without removing the block, like sunflowers do in vanilla
				block.dropBlockAsItem(world, event.getPos(), state, 0);
				event.setResult(Result.ALLOW);
			}
		} else if(block == Blocks.SAND || block == Blocks.HARDENED_CLAY || block == Blocks.STAINED_HARDENED_CLAY) { // intentionally excluding dirt, as I have other future plans in mind for it
			if(deadBush && world.isAirBlock(event.getPos().up())) {
				world.setBlockState(event.getPos().up(), Blocks.DEADBUSH.getDefaultState());
				event.setResult(Result.ALLOW);
			}
		} else if(block == Blocks.RED_FLOWER || block == Blocks.YELLOW_FLOWER) {
			if(flowers) {
				// drop as an item without removing the block, like sunflowers do in vanilla
				block.dropBlockAsItem(world, event.getPos(), state, 0);
				event.setResult(Result.ALLOW);
			}
		} else if(block == Blocks.CHORUS_FLOWER) {
			if(chorusFlower) {
				int age = state.getValue(BlockChorusFlower.AGE);
				if(age < 5) {
					if(!world.isRemote) {
						block.updateTick(world, event.getPos(), state, new ZeroFirstIntRandom());
					}
					event.setResult(Result.ALLOW);
				}
			}
		} else if(moss) {
			if(block == Blocks.COBBLESTONE) {
				world.setBlockState(event.getPos(), Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
				event.setResult(Result.ALLOW);
			} else if(block == Blocks.COBBLESTONE_WALL) {
				changePropertyFromTo(event, BlockWall.VARIANT, BlockWall.EnumType.NORMAL, BlockWall.EnumType.MOSSY);
			} else if(block == Blocks.STONEBRICK) {
				changePropertyFromTo(event, BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.DEFAULT, BlockStoneBrick.EnumType.MOSSY);
			} else if(block == Blocks.MONSTER_EGG) {
				changePropertyFromTo(event, BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONEBRICK, BlockSilverfish.EnumType.MOSSY_STONEBRICK);
			}
		}
	}
}
