package net.darkhax.botanypots.tempshelf;


import net.darkhax.bookshelf.api.serialization.ISerializer;
import net.minecraft.resources.ResourceLocation;

public interface DisplayStateSerializer<T extends DisplayState> extends ISerializer<T> {

    ResourceLocation getId();
}