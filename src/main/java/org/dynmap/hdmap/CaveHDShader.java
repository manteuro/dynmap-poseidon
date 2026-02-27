package org.dynmap.hdmap;

import static org.dynmap.JSONUtils.s;
import org.bukkit.block.Biome;
import org.dynmap.Color;
import org.dynmap.ColorScheme;
import org.dynmap.ConfigurationNode;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.MapIterator;
import org.json.simple.JSONObject;

public class CaveHDShader implements HDShader {
    private String name;
    private boolean iflit; // Added for lit area rendering

    public CaveHDShader(ConfigurationNode configuration) {
        name = (String) configuration.get("name");
        // Load onlyiflit from configuration; default to false to maintain original behavior
        iflit = configuration.getBoolean("onlyiflit", false);
    }

    @Override
    public boolean isBiomeDataNeeded() {
        return false;
    }

    @Override
    public boolean isRawBiomeDataNeeded() {
        return false;
    }

    @Override
    public boolean isHightestBlockYDataNeeded() {
        return false;
    }

    @Override
    public boolean isBlockTypeDataNeeded() {
        return true;
    }

    @Override
    public boolean isSkyLightLevelNeeded() {
        return false;
    }

    @Override
    public boolean isEmittedLightLevelNeeded() {
        // Must return true if iflit is enabled so Dynmap loads light data
        return iflit;
    }

    @Override
    public String getName() {
        return name;
    }

    private class OurShaderState implements HDShaderState {
        private Color color;
        protected MapIterator mapiter;
        protected HDMap map;
        private boolean air;

        private OurShaderState(MapIterator mapiter, HDMap map) {
            this.mapiter = mapiter;
            this.map = map;
            this.color = new Color();
        }

        public HDShader getShader() {
            return CaveHDShader.this;
        }

        public HDMap getMap() {
            return map;
        }

        public HDLighting getLighting() {
            return map.getLighting();
        }

        public void reset(HDPerspectiveState ps) {
            color.setTransparent();
            air = true;
        }

        public boolean processBlock(HDPerspectiveState ps) {
            int blocktype = ps.getBlockTypeID();
            switch (blocktype) {
                case 0:
                case 20:
                case 18:
                case 17:
                case 78:
                case 79:
                    break;
                default:
                    air = false;
                    return false;
            }

            if (!air) {
                // --- NEW: Check for light if feature is enabled ---
                if (iflit) {
                    // Check emitted light (torches, lava, etc.)
                    if (mapiter.getBlockEmittedLight() == 0) {
                        return false; // Skip this block and continue searching
                    }
                }

                int cr, cg, cb;
                int mult = 256;

                if (mapiter.getY() < 64) {
                    cr = 0;
                    cg = 64 + mapiter.getY() * 3;
                    cb = 255 - mapiter.getY() * 4;
                } else {
                    cr = (mapiter.getY() - 64) * 4;
                    cg = 255;
                    cb = 0;
                }

                switch(ps.getLastBlockStep()) {
                    case X_PLUS:
                    case X_MINUS:
                        mult = 224;
                        break;
                    case Z_PLUS:
                    case Z_MINUS:
                        mult = 256;
                        break;
                    default:
                        mult = 160;
                        break;
                }
                cr = cr * mult / 256;
                cg = cg * mult / 256;
                cb = cb * mult / 256;

                color.setRGBA(cr, cg, cb, 255);
                return true;
            }
            return false;
        }

        public void rayFinished(HDPerspectiveState ps) {
        }

        public void getRayColor(Color c, int index) {
            c.setColor(color);
        }

        public void cleanup() {
        }
    }

    public HDShaderState getStateInstance(HDMap map, MapChunkCache cache, MapIterator mapiter) {
        return new OurShaderState(mapiter, map);
    }

    public void addClientConfiguration(JSONObject mapObject) {
        s(mapObject, "shader", name);
    }
}