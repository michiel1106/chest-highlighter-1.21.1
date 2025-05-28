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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChestHighlighter implements ModInitializer {
	public static final String MOD_ID = "chest-highlighter";

	public static ServerWorld chestblockserverworld;
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


		ServerLifecycleEvents.SERVER_STARTED.register((minecraft) -> {
			chestblockserverworld = minecraft.getOverworld();
		});

		ServerWorldEvents.LOAD.register(((server, world) -> {
			chestblockserverworld = world;

		}));


		MidnightConfig.init(MOD_ID, Config.class);




		CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(CommandManager.literal("info").executes(context -> {


			System.out.println(glowchestswhatever);
			System.out.println(chestBlockEntity);
			System.out.println(haveblockdisplay);


			return 1;
		}))));









	CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").requires(source -> source.hasPermissionLevel(3)).then(literal("remove").executes(this::killthem))));



		CommandRegistrationCallback.EVENT.register((dispatcher, registryaccess, enviroment) -> dispatcher.register(literal("glowchests").requires(source -> source.hasPermissionLevel(3)).then(literal("glowpos").then(argument("blockpos", BlockPosArgumentType.blockPos()).then(argument("color", IntegerArgumentType.integer()).executes(this::spawnblockpos))))));


		ServerTickEvents.START_WORLD_TICK.register(world -> {
			Set<BlockPos> seenPositions = new HashSet<>();
			Map<ChestBlockEntity, ChestTag> newTags = new HashMap<>();
			Map<String, Integer> nameToColor = new HashMap<>();
			Set<ChestBlockEntity> chestsToRemove = new HashSet<>();

			// Parse config
			for (String entry : Config.stringList) {
				String[] parts = entry.split(":", 2);
				if (parts.length == 2) {
					try {
						nameToColor.put(parts[0], Integer.parseInt(parts[1]));
					} catch (NumberFormatException ignored) {}
				}
			}

			// Work from a snapshot of the chest map
			for (Map.Entry<ChestBlockEntity, BlockPos> entry : new HashMap<>(chestBlockEntity).entrySet()) {
				ChestBlockEntity chestBlock = entry.getKey();
				BlockPos pos = entry.getValue();

				// Deduplicate
				if (!seenPositions.add(pos)) {
					chestsToRemove.add(chestBlock);
					continue;
				}

				BlockState state = world.getBlockState(pos);
				BlockEntity blockEntity = world.getBlockEntity(pos);

				// Remove if not a valid chest
				if (!state.isOf(Blocks.CHEST) || !(blockEntity instanceof ChestBlockEntity)) {
					Entity entity = haveblockdisplay.get(chestBlockEntity);
					if (entity != null) entity.kill();
					chestsToRemove.add(chestBlock);
					continue;
				}

				// Build item name list
				List<String> itemnamelist = new ArrayList<>();
				for (int i = 0; i < chestBlock.size(); i++) {
					if (!chestBlock.getStack(i).isOf(Items.AIR)) {
						itemnamelist.add(chestBlock.getStack(i).getName().getString());
					}
				}

				// Store chest tag
				ChestTag tag = new ChestTag(pos, itemnamelist);
				newTags.put(chestBlock, tag);

				// Glow logic
				boolean shouldGlow = false;
				int color = 0xFFFFFF;
				for (String itemName : itemnamelist) {
					if (nameToColor.containsKey(itemName)) {
						shouldGlow = true;
						color = nameToColor.get(itemName);
						break;
					}
				}

				// Handle glowing
				if (shouldGlow) {
					if (!haveblockdisplay.containsKey(chestBlock)) {
						Direction facing = ChestBlock.getFacing(state);
						Entity displayEntity = getentity(color, facing, pos, world);
						haveblockdisplay.put(chestBlock, displayEntity);
						world.spawnEntity(displayEntity);
					}
				} else {
					Entity entity = haveblockdisplay.remove(chestBlock);
					if (entity != null) entity.kill();
				}
			}

			// Clean up chests to remove
			for (ChestBlockEntity toRemove : chestsToRemove) {
				chestBlockEntity.remove(toRemove);
			}

			// Apply new tags

			glowchestswhatever.putAll(newTags);
		});





	}



	private int spawnblockpos(CommandContext<ServerCommandSource> context) {
		if (context.getSource().hasPermissionLevel(3)) {

			BlockPos blockpos = BlockPosArgumentType.getBlockPos(context, "blockpos");
			int color = IntegerArgumentType.getInteger(context, "color");

			BlockState state = context.getSource().getWorld().getBlockState(blockpos);
			Direction facing = ChestBlock.getFacing(state);



			context.getSource().getWorld().spawnEntity(getentity(color, facing, blockpos, context.getSource().getWorld()));

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