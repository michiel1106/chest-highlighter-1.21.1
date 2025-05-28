package bikerboys.chesthighlighter;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.util.Identifier;

import java.util.List;

public class Config extends MidnightConfig {

    @Entry(name = "Item List") public static List<String> stringList = Lists.newArrayList(); // Array String Lists are also supported

    @Entry(name = "Chest Color") public static int ChestColor = 232312;


}
