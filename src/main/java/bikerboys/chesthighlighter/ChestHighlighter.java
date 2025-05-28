package bikerboys.chesthighlighter;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import static net.minecraft.server.command.CommandManager.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChestHighlighter implements ModInitializer {
	public static final String MOD_ID = "chest-highlighter";
	public static ServerWorld world;
	public static Map<ChestBlockEntity, ChestTag> glowchestswhatever = new HashMap<>();
	public static Map<ChestBlockEntity, BlockPos> chestBlockEntity = new HashMap<>();
	public static Map<ChestBlockEntity, Entity> haveblockdisplay = new HashMap<>();



	public static boolean enableglow;
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {


		ServerLifecycleEvents.SERVER_STOPPING.register(((server1) -> {

			List<? extends BlockDisplayEntity> entitiesByType = server1.getOverworld().getEntitiesByType(EntityType.BLOCK_DISPLAY, EntityPredicates.EXCEPT_SPECTATOR);


			for (BlockDisplayEntity blockDisplayEntity : entitiesByType) {
				blockDisplayEntity.kill();
			}


		}));


		MidnightConfig.init(MOD_ID, Config.class);


		ServerTickEvents.START_WORLD_TICK.register((serverWorld -> world = serverWorld));










	CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").requires(source -> source.hasPermissionLevel(3)).then(literal("remove").executes(this::killthem))));



		CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").requires(source -> source.hasPermissionLevel(3)).then(literal("glowpos").then(argument("blockpos", BlockPosArgumentType.blockPos()).then(argument("color", IntegerArgumentType.integer()).executes(this::spawnblockpos))))));







		ServerTickEvents.START_WORLD_TICK.register(world -> {
			Set<BlockPos> seenPositions = new HashSet<>();

			for (Iterator<Map.Entry<ChestBlockEntity, BlockPos>> it = chestBlockEntity.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<ChestBlockEntity, BlockPos> entry = it.next();
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





				// Deduplicate
				if (!seenPositions.add(pos)) {
					it.remove();
					continue;
				}

				// Validate block existence
				if (world.isChunkLoaded(pos) && !world.getBlockState(pos).isOf(Blocks.CHEST)) {
					haveblockdisplay.remove(entry.getKey());
					it.remove();
				}
			}



		});

		ServerTickEvents.END_WORLD_TICK.register(world -> {


			Map<String, Integer> nameToColor = new HashMap<>();
			for (String entry : Config.stringList) {
				String[] parts = entry.split(":", 2);
				if (parts.length == 2) {
					try {
						nameToColor.put(parts[0], Integer.parseInt(parts[1]));
					} catch (NumberFormatException ignored) {
					}
				}
			}



			for (Iterator<Map.Entry<ChestBlockEntity, ChestTag>> it = glowchestswhatever.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<ChestBlockEntity, ChestTag> entry = it.next();
				BlockPos pos = entry.getValue().pos;
				List<String> itemNames = entry.getValue().itemName;

				boolean shouldGlow = false;
				int color = 0xFFFFFF; // default white
				for (String itemname : itemNames) {
					if (nameToColor.containsKey(itemname)) {
						shouldGlow = true;
						color = nameToColor.get(itemname);
						break; // use the first match
					}
				}

				BlockState state = world.getBlockState(pos);
				if (state.isOf(Blocks.CHEST)) {
					if (shouldGlow) {
						if (!haveblockdisplay.containsKey(entry.getKey())) {
							Direction facing = ChestBlock.getFacing(state);
							Entity entity = getentity(color, facing, pos, world); // color is used here
							haveblockdisplay.put(entry.getKey(), entity);
							world.spawnEntity(entity);
						}
					} else {
						Entity entity = haveblockdisplay.remove(entry.getKey());
						if (entity != null) {
							entity.kill(); // safely remove just this one
						}
					}
				}

				// Remove glowchests entry if chest is gone
				if (world.isChunkLoaded(pos) && !state.isOf(Blocks.CHEST)) {
					Entity entity = haveblockdisplay.remove(entry.getKey());
					if (entity != null) {
						entity.kill();
					}
					it.remove();
				}
			}
		});
	}



	private int spawnblockpos(CommandContext<ServerCommandSource> context) {
		if (context.getSource().hasPermissionLevel(3)) {

			BlockPos blockpos = BlockPosArgumentType.getBlockPos(context, "blockpos");
			int color = IntegerArgumentType.getInteger(context, "color");

			BlockState state = context.getSource().getWorld().getBlockState(blockpos);
			Direction facing = ChestBlock.getFacing(state);



			context.getSource().getWorld().spawnEntity(getentity(color, facing, blockpos, world));

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










	public Entity getentity(int color, Direction facing, BlockPos pos, ServerWorld world) {
		NbtCompound nbt = new NbtCompound();


		nbt.putInt("glow_color_override", color);
		nbt.putBoolean("Glowing", true);
		nbt.put("block_state", NbtHelper.fromBlockState(Blocks.CHEST.getDefaultState()));
		nbt.putString("id", EntityType.BLOCK_DISPLAY.getRegistryEntry().registryKey().getValue().toString());

		Entity display = EntityType.loadEntityWithPassengers(nbt, world, entity -> {

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





}