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
import net.minecraft.block.BlockBush;
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
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.config.Property.Type;
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
	public static final String VERSION = "1.2.0";

	public static Configuration config;
	protected static int cactus, sugarcane;
	protected static boolean netherWart, melon, pumpkin, vine, lilyPad, deadBush, flowers, chorusFlower, mycelium, moss;

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

	protected static int getIntFormerBoolean(String key, String comment) {
		// This field was a boolean in a previous version of this mod.
		// If the user set the boolean to false, make this integer 0 instead of 16.
		boolean oldValue = true;
		ConfigCategory cat = config.getCategory(Configuration.CATEGORY_GENERAL);
		if (cat.containsKey(key))
		{
			Property prop = cat.get(key);
			if(prop.getType() == Type.BOOLEAN) {
				oldValue = prop.getBoolean();
				cat.remove(key);
			}
		}
		Property prop = config.get(Configuration.CATEGORY_GENERAL, key, 16, comment, 0, 16);
		if(!oldValue) {
			prop.set(0);
		}
		return prop.getInt();
	}

	protected static void syncConfig() {
		cactus = getIntFormerBoolean("cactus", "How many stages bone meal should cause cacti to grow. 16 means always grow a new block, and 0 means bone meal doesn't work on cacti.");
		sugarcane = getIntFormerBoolean("sugarcane", "How many stages bone meal should cause sugar canes to grow. 16 means always grow a new block, and 0 means bone meal doesn't work on sugar canes.");

		netherWart = config.get(Configuration.CATEGORY_GENERAL, "netherWart", true, "Whether using bone meal on nether wart should immediately advance it to the next growth stage").getBoolean();
		melon = config.get(Configuration.CATEGORY_GENERAL, "melon", true, "Whether using bone meal on a mature melon stem should immediately attempt to grow a melon").getBoolean();
		pumpkin = config.get(Configuration.CATEGORY_GENERAL, "pumpkin", true, "Whether using bone meal on a mature pumpkin stem should immediately attempt to grow a pumpkin").getBoolean();
		vine = config.get(Configuration.CATEGORY_GENERAL, "vine", true, "Whether using bone meal on a vine should immediately attempt to grow").getBoolean();
		lilyPad = config.get(Configuration.CATEGORY_GENERAL, "lilyPad", true, "Whether using bone meal on a lily pad should cause one to drop as an item").getBoolean();
		deadBush = config.get(Configuration.CATEGORY_GENERAL, "deadBush", true, "Whether using bone meal on sand or hardened clay should grow a dead bush").getBoolean();
		flowers = config.get(Configuration.CATEGORY_GENERAL, "flowers", true, "Whether using bone meal on flowers should cause one to drop as an item").getBoolean();
		chorusFlower = config.get(Configuration.CATEGORY_GENERAL, "chorusFlower", true, "Whether using bone meal on a chorus flower should immediately attempt to grow").getBoolean();
		mycelium = config.get(Configuration.CATEGORY_GENERAL, "mycelium", true, "Whether using bone meal on mycelium should cause mushrooms to grow").getBoolean();
		moss = config.get(Configuration.CATEGORY_GENERAL, "moss", true, "Whether using bone meal on cobblestone or stone bricks should cause moss to grow").getBoolean();

		if(config.hasChanged())
			config.save();
	}

	protected static void growCactusOrSugarcane(BonemealEvent event, IBlockState state, Block block, World world, PropertyInteger ageProperty, int stages) {
		BlockPos pos = event.getPos();
		// if the player uses bonemeal on the bottom of a 2-high plant, do what they meant
		while(world.getBlockState(pos.up()).getBlock() == block) {
			pos = pos.up();
		}
		// Advance the age by one less than bonemeal is supposed to advance by,
		// since the updateTick call will advance it by one. Also, never exceed the
		// maximum age of 15 (since pushing it to 16 in updateTick is what makes it grow).
		state = state.withProperty(ageProperty, Math.min(state.getValue(ageProperty) + stages - 1, 15));
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

	protected static void growMushrooms(BonemealEvent event) {
		BlockPos startpos = event.getPos().up();
		World world = event.getWorld();
		Random rand = world.rand;

		attemptLoop:
		for (int attempt = 0; attempt < 16; ++attempt) {
			BlockPos pos = startpos;
			for (int moves = 0; moves < attempt / 2; ++moves) {
				pos = pos.add(rand.nextInt(3) - 1, (rand.nextInt(3) - 1) * rand.nextInt(3) / 2, rand.nextInt(3) - 1);
				if (world.getBlockState(pos.down()).getBlock() != Blocks.MYCELIUM || world.getBlockState(pos).isNormalCube())
					continue attemptLoop;
			}

			if (world.isAirBlock(pos)) {
				BlockBush block = rand.nextInt(3) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM;
				IBlockState state = block.getDefaultState();
				if(block.canBlockStay(world, pos, state))
					world.setBlockState(pos, state);
			}
		}

		event.setResult(Result.ALLOW);
	}

	@SubscribeEvent
	public void onBonemeal(BonemealEvent event) {
		IBlockState state = event.getBlock();
		Block block = state.getBlock();
		World world = event.getWorld();
		// in the future, maybe add stuff for sponges, saplings, mushrooms, ferns, and dirt variants
		if(block == Blocks.CACTUS) {
			if(cactus > 0) growCactusOrSugarcane(event, state, block, world, BlockCactus.AGE, cactus);
		} else if(block == Blocks.REEDS) {
			if(sugarcane > 0) growCactusOrSugarcane(event, state, block, world, BlockReed.AGE, sugarcane);
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
		} else if(block == Blocks.MYCELIUM) {
			if(mycelium) growMushrooms(event);
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
