package net.darkhax.botanypots.tempshelf.math;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.darkhax.bookshelf.api.serialization.ISerializer;
import net.darkhax.bookshelf.api.serialization.NBTParseException;
import net.darkhax.bookshelf.api.serialization.Serializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.EnumUtils;

import java.util.Locale;

/**
 * This enum contains rotational state data that can be used to rotate a render while retaining alignment with the world
 * axis.
 */
public enum AxisAlignedRotation {

    X_0(RotationAxis.X, 0),
    X_90(RotationAxis.X, 1),
    X_180(RotationAxis.X, 2),
    X_270(RotationAxis.X, 3),

    Y_0(RotationAxis.Y, 0),
    Y_90(RotationAxis.Y, 1),
    Y_180(RotationAxis.Y, 2),
    Y_270(RotationAxis.Y, 3),

    Z_0(RotationAxis.Z, 0),
    Z_90(RotationAxis.Z, 1),
    Z_180(RotationAxis.Z, 2),
    Z_270(RotationAxis.Z, 3);

    public static final ISerializer<AxisAlignedRotation> SERIALIZER = new Serializer();

    /**
     * A Quaternion that contains the rotational information. In this case it represents a 0, 90, 180, or 270-degree
     * rotation along the X, Y, or Z axis.
     */
    private final Quaternion rotation;

    /**
     * A predetermined offset that will realign the render when translated.
     */
    private final Vector3f offset;

    AxisAlignedRotation(RotationAxis axis, int amount) {

        if (amount < 0 || amount > 3) {

            throw new IllegalArgumentException("Rotation amount " + amount + " is out of bounds. Must be 0-3. 0 = 0 degrees. 1 = 90 degrees. 2 = 180 degrees. 3 = 270 degrees.");
        }

        this.rotation = axis.quaternions[amount];
        this.offset = axis.offsets[amount];
    }

    public void apply(PoseStack pose) {

        // Applies the rotation to the render.
        pose.mulPose(this.rotation);

        // Realigns the render with the original axis-aligned position.
        pose.translate(this.offset.x(), this.offset.y(), this.offset.z());
    }

    /**
     * An enum containing information about each rotational axis.
     */
    public enum RotationAxis {

        X(Vector3f.XP, Vector3f.ZERO, new Vector3f(0, 0, -1), new Vector3f(0, -1, -1), new Vector3f(0, -1, 0)),
        Y(Vector3f.YP, Vector3f.ZERO, new Vector3f(-1, 0, 0), new Vector3f(-1, 0, -1), new Vector3f(0, 0, -1)),
        Z(Vector3f.ZP, Vector3f.ZERO, new Vector3f(0, -1, 0), new Vector3f(-1, -1, 0), new Vector3f(-1, 0, 0));

        /**
         * The rotation quaternions for 0, 90, 180, and 270 degrees along the axis.
         */
        private final Quaternion[] quaternions;

        /**
         * The translation offsets to snap the render back to the original axis aligned position.
         */
        private final Vector3f[] offsets;

        RotationAxis(Vector3f axisVect, Vector3f offsetA, Vector3f offsetB, Vector3f offsetC, Vector3f offsetD) {

            this.quaternions = new Quaternion[]{axisVect.rotationDegrees(0), axisVect.rotationDegrees(90f), axisVect.rotationDegrees(180f), axisVect.rotationDegrees(270f)};
            this.offsets = new Vector3f[]{offsetA, offsetB, offsetC, offsetD};
        }
    }

    public static class Serializer implements ISerializer<AxisAlignedRotation> {

        private Serializer() {

        }
        
        @Override
        public AxisAlignedRotation fromJSON(JsonElement json) {

            AxisAlignedRotation rotation = null;

            if (json instanceof JsonPrimitive primitive && primitive.isString()) {

                rotation = EnumUtils.getEnum(AxisAlignedRotation.class, primitive.getAsString().toUpperCase(Locale.ROOT));

                if (rotation == null) {

                    throw new JsonParseException("Unknown rotation name: " + primitive.getAsString().toUpperCase(Locale.ROOT));
                }
            }

            else if (json instanceof JsonObject obj) {

                final String axis = Serializers.STRING.fromJSON(obj, "axis");
                final int degrees = Serializers.INT.fromJSON(obj, "degrees", -1);

                if (axis == null) {

                    throw new JsonParseException("Axis name was not defined. Must be x, y, or z");
                }

                if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {

                    throw new JsonParseException("Invalid degrees. Must be 0, 90, 180, or 270.");
                }

                final String name = axis + "_" + degrees;
                rotation = EnumUtils.getEnum(AxisAlignedRotation.class, name.toUpperCase(Locale.ROOT));
            }

            if (rotation == null) {

                throw new JsonParseException("Invalid rotation defined.");
            }

            return rotation;
        }

        @Override
        public JsonElement toJSON(AxisAlignedRotation toWrite) {

            return new JsonPrimitive(toWrite.name());
        }

        @Override
        public AxisAlignedRotation fromByteBuf(FriendlyByteBuf buffer) {

            return AxisAlignedRotation.values()[buffer.readVarInt()];
        }

        @Override
        public void toByteBuf(FriendlyByteBuf buffer, AxisAlignedRotation toWrite) {

            buffer.writeVarInt(toWrite.ordinal());
        }

        @Override
        public Tag toNBT(AxisAlignedRotation toWrite) {

            return StringTag.valueOf(toWrite.name());
        }

        @Override
        public AxisAlignedRotation fromNBT(Tag nbt) {


            AxisAlignedRotation rotation = null;

            if (nbt instanceof StringTag stringTag) {

                rotation = EnumUtils.getEnum(AxisAlignedRotation.class, stringTag.getAsString().toUpperCase(Locale.ROOT));

                if (rotation == null) {

                    throw new NBTParseException("Unknown rotation name: " + stringTag.getAsString().toUpperCase(Locale.ROOT));
                }
            }

            else if (nbt instanceof CompoundTag tag) {

                final String axis = Serializers.STRING.fromNBT(tag, "axis");
                final int degrees = Serializers.INT.fromNBT(tag, "degrees", -1);

                if (axis == null) {

                    throw new NBTParseException("Axis name was not defined. Must be x, y, or z");
                }

                if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270) {

                    throw new NBTParseException("Invalid degrees. Must be 0, 90, 180, or 270.");
                }

                final String name = axis + "_" + degrees;
                rotation = EnumUtils.getEnum(AxisAlignedRotation.class, name.toUpperCase(Locale.ROOT));
            }

            if (rotation == null) {

                throw new NBTParseException("Invalid rotation defined.");
            }

            return rotation;
        }
    }
}
