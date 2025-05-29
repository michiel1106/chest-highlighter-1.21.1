package bikerboys.chesthighlighter;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ChestHighlighter implements ModInitializer {
	public static final String MOD_ID = "chest-highlighter";


	public static Map<BlockPos, ChestBlockEntity> LoadedchestBlockEntities = new HashMap<>();
	public static Map<ChestBlockEntity, ChestTag> ValidChestTagItems = new HashMap<>();
	public static Map<ChestBlockEntity, Entity> AssociatedEntity = new HashMap<>();


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

		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(((blockEntity, world) -> {
			if (blockEntity instanceof ChestBlockEntity chestBlock) {




				if (!LoadedchestBlockEntities.containsKey(blockEntity.getPos())) {

					try {
						LoadedchestBlockEntities.put(blockEntity.getPos(), chestBlock);
					} catch (ConcurrentModificationException e) {

						System.out.println("man shits goin downnn");
					}


				}

			}

		}));


		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(((blockEntity, world) -> {
			if (blockEntity instanceof ChestBlockEntity chestBlock) {

                LoadedchestBlockEntities.remove(chestBlock.getPos());

			}
		}));


		ServerTickEvents.END_SERVER_TICK.register((world) -> {
			Map<String, Integer> nameToColor = new HashMap<>();
			Map<BlockPos, ChestBlockEntity> tempLoadedBlockEntities = LoadedchestBlockEntities;


			for (String entry : Config.stringList) {
				String[] parts = entry.split(":", 2);
				if (parts.length == 2) {
					try {
						nameToColor.put(parts[0], Integer.parseInt(parts[1]));
					} catch (NumberFormatException ignored) {}
				}
			}




			tempLoadedBlockEntities.forEach(((blockPos, chestBlockEntity) -> {
				List<String> item = new ArrayList<>();

				for (int i = 0; i < chestBlockEntity.size(); i++) {
					ItemStack stack = chestBlockEntity.getStack(i);
					item.add(stack.getName().getString());
				}

				ChestTag chestTag = new ChestTag(blockPos, item);

				ValidChestTagItems.put(chestBlockEntity, chestTag);

			}));


			Map<ChestBlockEntity, ChestTag> tempValidChestTagItems = ValidChestTagItems;

			tempValidChestTagItems.forEach(((chestBlockEntity, chestTag) -> {

				AtomicBoolean shouldGlow = new AtomicBoolean(false);

				AtomicInteger color = new AtomicInteger(0);


				chestTag.itemName.forEach((string -> {
					if (nameToColor.containsKey(string)) {
						shouldGlow.set(true);
						color.set(nameToColor.get(string));
					}

				}));


				if (shouldGlow.get()) {

					BlockState blockState = world.getOverworld().getBlockState(chestTag.pos);

					if (!(blockState.getBlock() instanceof ChestBlock)) {
						return; // Skip if it's not a chest anymore
					}

					Direction facing = ChestBlock.getFacing(blockState);


					Entity entity = getentity(color.get(), facing, chestTag.pos, world.getOverworld());

					if (!AssociatedEntity.containsKey(chestBlockEntity)) {

						AssociatedEntity.put(chestBlockEntity, entity);


						world.getOverworld().spawnEntity(entity);
					}

				}
			}));





			Map<ChestBlockEntity, Entity> tempAssociatedEntity = AssociatedEntity;

			Iterator<Map.Entry<ChestBlockEntity, Entity>> iterator = tempAssociatedEntity.entrySet().iterator();


			while (iterator.hasNext()) {
				Map.Entry<ChestBlockEntity, Entity> entry = iterator.next();
				ChestBlockEntity chestBlockEntity = entry.getKey();
				Entity entity = entry.getValue();
				boolean remove = false;


				BlockState state = world.getOverworld().getBlockState(chestBlockEntity.getPos());
				BlockEntity blockEntity = world.getOverworld().getBlockEntity(chestBlockEntity.getPos());



				if (!state.isOf(Blocks.CHEST) || !(blockEntity instanceof ChestBlockEntity)) {
					entity.kill();
					remove = true; // ✅ Remove safely during iteration

				}

				boolean containsitem = false;
				for (int i = 0; i < chestBlockEntity.size(); i++) {


					ItemStack stack = chestBlockEntity.getStack(i);
					String string = stack.getName().getString();

					if (nameToColor.containsKey(string)) {
						containsitem = true;
					}
				}

				if (!containsitem) {
					entity.kill();
					remove = true;
				}

				if (remove) {
					iterator.remove();
				}
				AssociatedEntity = tempAssociatedEntity;
			}








		});
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




