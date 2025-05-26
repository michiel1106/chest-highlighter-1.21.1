package bikerboys.chesthighlighter;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ChestTag {
    public final BlockPos pos;
    public final List<String> itemName;

    public ChestTag(BlockPos pos, List<String> itemName) {
        this.pos = pos;
        this.itemName = itemName;
    }
}
