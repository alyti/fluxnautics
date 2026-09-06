package dev.alyti.fluxnautics.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.FastColor;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.awt.*;
import java.util.List;
import java.util.stream.Stream;


public class SFColors {
    public static int MEDIA_OURPLE = new Color(188, 118, 255, 255).getRGB();

    // cursed shit because awt color can't return RBG with A in any way as an int :D
    public static int MEDIA_OURPLE_ARGB = mekanism.common.lib.Color.rgb(new Color(188, 118, 255).getRGB()).alpha(1).argb();
    public static int DARK_GREY_ARGB = mekanism.common.lib.Color.rgb(new Color(28, 28, 35).getRGB()).alpha(0.2).argb();

    /**
     * <a href="https://bottosson.github.io/posts/oklab/">Oklab colour space</a>
     * @param lightness 0-1
     * @param chroma 0-1
     * @param hue modular 0-2pi (e.g. 0 == 2pi == 4pi etc)
     * @return a colour constructed from Oklab Lightness, Chroma, Hue
     */
    public static Color LChOklab(final float lightness, final float chroma, final float hue) {
        final double a = chroma * Math.cos(hue);
        final double b = chroma * Math.sin(hue);
        return fromOklab(lightness, (float)a, (float)b);
    }

    /**
     * <a href="https://bottosson.github.io/posts/oklab/">Oklab colour space</a>
     * @param a green/red amount
     * @param b blue/yellow amount
     */
    public static Color fromOklab(final float lightness, final float a, final float b) {
        final float l_ = lightness + 0.3963377774f * a + 0.2158037573f * b;
        final float m_ = lightness - 0.1055613458f * a - 0.0638541728f * b;
        final float s_ = lightness - 0.0894841775f * a - 1.2914855480f * b;

        final float l = l_*l_*l_;
        final float m = m_*m_*m_;
        final float s = s_*s_*s_;

        return new Color(
                Math.clamp(+4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s, 0, 1),
                Math.clamp(-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s, 0, 1),
                Math.clamp(-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s, 0, 1)
        );
    }

    /**
     * <a href="https://bottosson.github.io/posts/oklab/">Oklab colour space</a>
     * @return (Lightness, a: green/red, b: blue/yellow)
     */
    public static Vector3d toOklab(final Color c) {
        final float l = 0.4122214708f * c.getRed() + 0.5363325363f * c.getGreen() + 0.0514459929f * c.getBlue();
        final float m = 0.2119034982f * c.getRed() + 0.6806995451f * c.getGreen() + 0.1073969566f * c.getBlue();
        final float s = 0.0883024619f * c.getRed() + 0.2817188376f * c.getGreen() + 0.6299787005f * c.getBlue();

        final double l_ = Math.cbrt(l / 255);
        final double m_ = Math.cbrt(m / 255);
        final double s_ = Math.cbrt(s / 255);

        return new Vector3d(
                0.2104542553f*l_ + 0.7936177850f*m_ - 0.0040720468f*s_,
                1.9779984951f*l_ - 2.4285922050f*m_ + 0.4505937099f*s_,
                0.0259040371f*l_ + 0.7827717662f*m_ - 0.8086757660f*s_
        );
    }
    /**
     * <a href="https://bottosson.github.io/posts/oklab/">Oklab colour space</a>
     * @param Lab (Lightness, a: green/red, b: blue/yellow)
     * @return (Lightness, Chroma, hue)
     */
    public static Vector3d LabToLCh(final Vector3dc Lab) {
        return new Vector3d(
                Lab.x(),
                Math.sqrt(Lab.y()*Lab.y() + Lab.z()*Lab.z()),
                Math.atan2(Lab.y(), Lab.z())
        );
    }

    public static final Codec<Integer> COLOR_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<com.mojang.datafixers.util.Pair<Integer, T>> decode(DynamicOps<T> ops, T input) {
            // Try as int (raw ARGB)
            DataResult<Number> numResult = ops.getNumberValue(input);
            if (numResult.isSuccess()) {
                return DataResult.success(com.mojang.datafixers.util.Pair.of(numResult.getOrThrow().intValue(), ops.empty()));
            }

            // Try as string (hex)
            DataResult<String> strResult = ops.getStringValue(input);
            if (strResult.isSuccess()) {
                try {
                    return DataResult.success(com.mojang.datafixers.util.Pair.of(parseHex(strResult.getOrThrow()), ops.empty()));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e::getMessage);
                }
            }

            // Try as list (RGBA floats 0-1 or ints 0-255)
            DataResult<Stream<com.mojang.datafixers.util.Pair<T, T>>> mapResult = ops.getMapValues(input);
            if (mapResult.isSuccess()) {
                try {
                    List<Number> nums = mapResult.getOrThrow()
                            .sorted((a, b) -> {
                                double ka = ops.getNumberValue(a.getFirst()).getOrThrow().doubleValue();
                                double kb = ops.getNumberValue(b.getFirst()).getOrThrow().doubleValue();
                                return Double.compare(ka, kb);
                            })
                            .map(e -> ops.getNumberValue(e.getSecond()).getOrThrow())
                            .toList();

                    if (nums.size() < 3 || nums.size() > 4) {
                        return DataResult.error(() -> "Color map must have 3-4 entries, got " + nums.size());
                    }

                    return DataResult.success(com.mojang.datafixers.util.Pair.of(parseArray(nums), ops.empty()));
                } catch (Exception e) {
                    return DataResult.error(e::getMessage);
                }
            }

            return DataResult.error(() -> "Color must be an int, hex string (#ARGB, #RGB, 0xARGB, 0xRGB), or [R,G,B,(A)] array");
        }

        @Override
        public <T> DataResult<T> encode(Integer value, DynamicOps<T> ops, T rest) {
            return Codec.INT.encode(value, ops, rest);
        }
    };

    private static int parseHex(String hex) {
        hex = hex.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.toLowerCase().startsWith("0x")) hex = hex.substring(2);

        return switch (hex.length()) {
            case 3 -> { // #RGB -> ARGB
                int r = Integer.parseInt(hex.charAt(0) + "" + hex.charAt(0), 16);
                int g = Integer.parseInt(hex.charAt(1) + "" + hex.charAt(1), 16);
                int b = Integer.parseInt(hex.charAt(2) + "" + hex.charAt(2), 16);
                yield 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            case 4 -> { // #ARGB
                int a = Integer.parseInt(hex.charAt(0) + "" + hex.charAt(0), 16);
                int r = Integer.parseInt(hex.charAt(1) + "" + hex.charAt(1), 16);
                int g = Integer.parseInt(hex.charAt(2) + "" + hex.charAt(2), 16);
                int b = Integer.parseInt(hex.charAt(3) + "" + hex.charAt(3), 16);
                yield (a << 24) | (r << 16) | (g << 8) | b;
            }
            case 6 -> { // #RRGGBB -> ARGB
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                yield 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            case 8 -> // #AARRGGBB
                    (int) Long.parseLong(hex, 16);
            default -> throw new IllegalArgumentException("Invalid hex color length: " + hex.length());
        };
    }

    private static int parseArray(List<Number> nums) {
        // Detect float mode: if first value is <= 1.0, treat as 0-1 range
        boolean isFloat = nums.get(0).doubleValue() <= 1.0;

        if (isFloat) {
            float r = nums.get(0).floatValue();
            float g = nums.get(1).floatValue();
            float b = nums.get(2).floatValue();
            float a = nums.size() > 3 ? nums.get(3).floatValue() : 1.0f;
            return FastColor.ARGB32.colorFromFloat(a, r, g, b);
        } else {
            int r = nums.get(0).intValue();
            int g = nums.get(1).intValue();
            int b = nums.get(2).intValue();
            int a = nums.size() > 3 ? nums.get(3).intValue() : 255;
            return FastColor.ARGB32.color(a, r, g, b);
        }
    }
}