package bikerboys.chesthighlighter;

import com.ibm.icu.impl.StringRange;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import static net.minecraft.server.command.CommandManager.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.common.base.Predicates;

import java.util.*;
import java.util.function.Predicate;

public class ChestHighlighter implements ModInitializer {
	public static final String MOD_ID = "chest-highlighter";
	public static ServerWorld world;
	public static Map<ChestBlockEntity, ChestTag> glowchestswhatever = new HashMap<>();
	public static Map<ChestBlockEntity, BlockPos> chestBlockEntity = new HashMap<>();



	public static boolean enableglow;
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {


		MidnightConfig.init(MOD_ID, Config.class);


		ServerTickEvents.START_WORLD_TICK.register((serverWorld -> world = serverWorld));





		CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(CommandManager.literal("giveInformATion").executes(context -> {

			System.out.println("glowchestswhatever  " + glowchestswhatever);
			System.out.println("chestblockentity  " + chestBlockEntity);

			glowchestswhatever.forEach((((chestBlockEntity1, chestTag) -> {

				System.out.println("chestblockentity items  " + chestTag.itemName);


				List<BlockDisplayEntity> what = world.getEntitiesByClass(BlockDisplayEntity.class, new Box(chestBlockEntity1.getPos()).expand(0.01), EntityPredicates.EXCEPT_SPECTATOR);









				System.out.println("blockentity inside? + " + !what.isEmpty());

			})));









			return 1;
		}))));





		CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").then(literal("glow").then(argument("color", IntegerArgumentType.integer()).then(argument("Item Name", StringArgumentType.string()).executes(this::glowchests))))));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").then(literal("remove").executes(this::killthem))));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(
						literal("hasblockdisplayentity")
								.then(argument("pos", BlockPosArgumentType.blockPos())
										.executes(ctx -> {
											ServerCommandSource source = ctx.getSource();
											ServerWorld world = source.getWorld();
											BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
											Box box = new Box(pos).expand(0.01);

											List<BlockDisplayEntity> found = world.getEntitiesByClass(
													BlockDisplayEntity.class,
													box,
													EntityPredicates.EXCEPT_SPECTATOR
											);

											if (!found.isEmpty()) {
												source.sendFeedback(() -> Text.literal("✅ Found " + found.size() + " BlockDisplayEntities at " + pos), false);
											} else {
												source.sendFeedback(() -> Text.literal("❌ No BlockDisplayEntities at " + pos), false);
											}
											return 1;
										})
								)
				)
		);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").then(literal("glowpos").then(argument("blockpos", BlockPosArgumentType.blockPos()).then(argument("color", IntegerArgumentType.integer()).executes(this::spawnblockpos))))));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").then(literal("removepos").then(argument("blockpos", BlockPosArgumentType.blockPos()).executes(this::killpos)))));


	//	CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
	//		dispatcher.register(
	//				literal("glowchests")
	//						.then(literal("update").executes(this::update))
	//		);
	//	});



		ServerTickEvents.START_WORLD_TICK.register(world -> {
			Set<BlockPos> seenPositions = new HashSet<>();

			for (Iterator<Map.Entry<ChestBlockEntity, BlockPos>> it = chestBlockEntity.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<ChestBlockEntity, BlockPos> entry = it.next();
				BlockPos pos = entry.getValue();





				// Deduplicate
				if (!seenPositions.add(pos)) {
					it.remove();
					continue;
				}

				// Validate block existence
				if (world.isChunkLoaded(pos) && !world.getBlockState(pos).isOf(Blocks.CHEST)) {
					it.remove();
				}
			}



		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {

			for (Iterator<Map.Entry<ChestBlockEntity, ChestTag>> it = glowchestswhatever.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<ChestBlockEntity, ChestTag> entry = it.next();
				BlockPos pos = entry.getValue().pos;




				List<String> itemName = entry.getValue().itemName;

				for (String itemname : itemName) {
					if (Config.stringList.contains(itemname)) {


						BlockState state = world.getBlockState(pos);

						if (state.isOf(Blocks.CHEST)) {
							Direction facing = ChestBlock.getFacing(state);



						Entity entity = getentity(123452, facing, pos);

						BlockPos offsetPos = getOffsetPosForFacing(pos, facing);
						Box box = new Box(offsetPos);

						List<BlockDisplayEntity> what = world.getEntitiesByClass(BlockDisplayEntity.class, box, EntityPredicates.EXCEPT_SPECTATOR);

						if (what.isEmpty()) {
							world.spawnEntity(entity);

						}
						}
					}
				}


				if (world.isChunkLoaded(pos) && !world.getBlockState(pos).isOf(Blocks.CHEST)) {
					it.remove();
				}
			}



            for (Map.Entry<ChestBlockEntity, BlockPos> entry : chestBlockEntity.entrySet()) {
                BlockPos pos = entry.getValue();

                BlockEntity chestBlock1 = world.getBlockEntity(pos);

                List<String> itemnamelist = new ArrayList<>();



				if (chestBlock1 instanceof ChestBlockEntity chestBlock) {


					for (int i = 0; i < chestBlock.size(); i++) {
						if (!chestBlock.getStack(i).isOf(Items.AIR)) {
							String string = chestBlock.getStack(i).getName().getString();
							itemnamelist.add(string);
						}


					}

					ChestTag chestTag = new ChestTag(pos, itemnamelist);


						glowchestswhatever.put(chestBlock, chestTag);



				}
            }

		});








	}

//	private int update(CommandContext<ServerCommandSource> context) {
//		Iterator<Map.Entry<Entity, ChestTag>> iterator = glowchestswhatever.entrySet().iterator();
//
//		while (iterator.hasNext()) {
//			Map.Entry<Entity, ChestTag> entry = iterator.next();
//			Entity entity = entry.getKey();
//			ChestTag tag = entry.getValue();
//
//			BlockEntity blockEntity = context.getSource().getWorld().getBlockEntity(tag.pos);
//			boolean containsItem = false;
//
//			if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
//				for (int i = 0; i < chestBlockEntity.size(); i++) {
//					ItemStack itemStack = chestBlockEntity.getStack(i);
//					if (!itemStack.isEmpty()) {
//						String itemString = itemStack.getName().getString();
//						if (itemString.equals(tag.itemName)) {
//							containsItem = true;
//							break;
//						}
//					}
//				}
//			}
//
//			if (!containsItem) {
//				entity.kill(); // or entity.discard()
//				iterator.remove();
//			}
//		}
//
//		return 1;
//	}

	private int killpos(CommandContext<ServerCommandSource> context) {
		if (context.getSource().hasPermissionLevel(3)) {

			BlockPos blockpos = BlockPosArgumentType.getBlockPos(context, "blockpos");

			int x = blockpos.getX();
			int y = blockpos.getY();
			int z = blockpos.getZ();



			ParseResults<ServerCommandSource> parseResults =
					context.getSource().getServer().getCommandManager().getDispatcher().parse("execute positioned " + x + " " + y + " " + z + " run kill @e[type=minecraft:block_display, x=" + x + ", y=" + y + ", z=" + z + ", dx=-0.1, dy=-0.1, dz=-0.1]",
									context.getSource());

			context.getSource().getServer().getCommandManager().execute(parseResults, "execute positioned " + x + " " + y + " " + z + " run kill @e[type=minecraft:block_display, x=" + x + ", y=" + y + ", z=" + z + ", dx=-0.1, dy=-0.1, dz=-0.1]");



		}

		return 0;
	}

	private int spawnblockpos(CommandContext<ServerCommandSource> context) {
		if (context.getSource().hasPermissionLevel(3)) {

			BlockPos blockpos = BlockPosArgumentType.getBlockPos(context, "blockpos");
			int color = IntegerArgumentType.getInteger(context, "color");

			BlockState state = context.getSource().getWorld().getBlockState(blockpos);
			Direction facing = ChestBlock.getFacing(state);



			context.getSource().getWorld().spawnEntity(getentity(color, facing, blockpos));

			return 1;
		}

		return 0;
	}

	private int killthem(CommandContext<ServerCommandSource> context) {

		if (context.getSource().hasPermissionLevel(3)) {



					ParseResults<ServerCommandSource> parseResults =
							context.getSource().getServer().getCommandManager().getDispatcher().parse("kill @e[type=minecraft:block_display,nbt={block_state:{Name:\"minecraft:chest\",Properties:{}}}]", context.getSource());

					context.getSource().getServer().getCommandManager().execute(parseResults, "kill @e[type=minecraft:block_display,nbt={block_state:{Name:\"minecraft:chest\",Properties:{}}}]");








		}

		return 1;
	}

	private int glowchests(CommandContext<ServerCommandSource> context) {
//		if (context.getSource().hasPermissionLevel(3)) {
//			int color = IntegerArgumentType.getInteger(context, "color");
//			String itemname = StringArgumentType.getString(context, "Item Name");
//
//			if (!glowchestswhatever.isEmpty()) {
//				for (Map.Entry<Entity, ChestTag> chest : glowchestswhatever.entrySet()) {
//					boolean isitemitemname = false;
//					BlockPos pos = chest.getValue().pos;
//					BlockEntity blockEntity = context.getSource().getWorld().getBlockEntity(pos);
//
//					if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
//						for (int i = 0; i < chestBlockEntity.size(); i++) {
//							ItemStack itemStack = chestBlockEntity.getStack(i);
//							if (!itemStack.isEmpty()) {
//								String itemString = itemStack.getName().getString();
//								if (itemString.equals(itemname)) {
//									isitemitemname = true;
//									break;
//								}
//							}
//						}
//					}
//
//					if (isitemitemname) {
//						BlockState state = context.getSource().getWorld().getBlockState(pos);
//						Direction facing = ChestBlock.getFacing(state);
//						Entity entity = getentity(color, facing, pos);
//
//						AbstractList<String> itemnames = new ArrayList<>();
//						itemnames.add(itemname);
//
//						glowchestswhatever.put(entity, new ChestTag(pos, itemnames));
//						context.getSource().getWorld().spawnEntity(entity);
//					}
//				}
//			}
//		}
		return 1;
	}


	public static void spawnBoxParticles(ServerWorld world, Box box) {
		double step = 0.2; // reasonable spacing to reduce clutter

		// Draw corners and edges by spawning particles at box bounds
		for (double x = box.minX; x <= box.maxX; x += step) {
			for (double z = box.minZ; z <= box.maxZ; z += step) {
				world.spawnParticles(ParticleTypes.END_ROD, x, box.minY, z, 1, 0, 0, 0, 0);
				world.spawnParticles(ParticleTypes.END_ROD, x, box.maxY, z, 1, 0, 0, 0, 0);
			}
		}

		for (double y = box.minY; y <= box.maxY; y += step) {
			for (double x = box.minX; x <= box.maxX; x += step) {
				world.spawnParticles(ParticleTypes.END_ROD, x, y, box.minZ, 1, 0, 0, 0, 0);
				world.spawnParticles(ParticleTypes.END_ROD, x, y, box.maxZ, 1, 0, 0, 0, 0);
			}
			for (double z = box.minZ; z <= box.maxZ; z += step) {
				world.spawnParticles(ParticleTypes.END_ROD, box.minX, y, z, 1, 0, 0, 0, 0);
				world.spawnParticles(ParticleTypes.END_ROD, box.maxX, y, z, 1, 0, 0, 0, 0);
			}
		}
	}





	public Entity getentity(int color, Direction facing, BlockPos pos) {
		NbtCompound nbt = new NbtCompound();


		nbt.putInt("glow_color_override", color);
		nbt.putBoolean("Glowing", true);
		nbt.put("block_state", NbtHelper.fromBlockState(Blocks.CHEST.getDefaultState()));
		nbt.putString("id", EntityType.BLOCK_DISPLAY.getRegistryEntry().registryKey().getValue().toString());

		Entity display = EntityType.loadEntityWithPassengers(nbt, ChestHighlighter.world, entity -> {

			if (facing.equals(Direction.NORTH)) {
				entity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ() + 1, facing.getOpposite().asRotation() - 90, 0);
			} else if (facing.equals(Direction.EAST)) {
				entity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), facing.getOpposite().asRotation() - 90, 0);
			} else if (facing.equals(Direction.SOUTH)) {
				entity.refreshPositionAndAngles(pos.getX() + 1, pos.getY(), pos.getZ(), facing.getOpposite().asRotation() - 90, 0);
			} else if (facing.equals(Direction.WEST)) {
				entity.refreshPositionAndAngles(pos.getX() + 1, pos.getY(), pos.getZ() + 1, facing.getOpposite().asRotation() - 90, 0);
			}


			return entity;
		});


		return display;

	}

	public BlockPos getOffsetPosForFacing(BlockPos pos, Direction facing) {
		// Matches your offset logic in getentity()
		if (facing == Direction.NORTH) {
			return pos.add(0, 0, 1);
		} else if (facing == Direction.EAST) {
			return pos;
		} else if (facing == Direction.SOUTH) {
			return pos.add(1, 0, 0);
		} else if (facing == Direction.WEST) {
			return pos.add(1, 0, 1);
		} else {
			return pos; // fallback no offset
		}
	}





}