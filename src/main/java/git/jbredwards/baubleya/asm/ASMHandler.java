/*
 * Copyright (c) 2023. jbredwards
 * All rights reserved.
 */

package git.jbredwards.baubleya.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("Baubley Aether Plugin")
public final class ASMHandler implements IFMLLoadingPlugin
{
    /**
     * This class exists because the launcher doesn't allow {@link IClassTransformer IClassTransformers}
     * to be the same class as {@link IFMLLoadingPlugin IFMLLoadingPlugins}
     */
    @SuppressWarnings("unused") // called via asm
    public static final class Transformer implements IClassTransformer, Opcodes
    {
        @Nonnull
        @Override
        public byte[] transform(@Nonnull final String name, @Nonnull final String transformedName, @Nonnull final byte[] basicClass) {
            // PlayerAether
            if("com.gildedgames.the_aether.player.PlayerAether".equals(transformedName)) {
                @Nonnull final ClassNode classNode = new ClassNode();
                new ClassReader(basicClass).accept(classNode, ClassReader.SKIP_FRAMES);
                for(@Nonnull final MethodNode method : classNode.methods) {
                    /*
                     * Constructor:
                     * Old code:
                     * this.accessories = new InventoryAccessories(player);
                     *
                     * New code:
                     * // Use a baubles wrapper instead of a separate inventory, should add Aether + Baubles compatibility with minimal code changes
                     * this.accessories = new git.jbredwards.baubleya.util.BaublesAccessoryInventory(player);
                     */
                    if("<init>".equals(method.name)) {
                        for(@Nonnull final AbstractInsnNode insn : method.instructions.toArray()) {
                            if(insn.getOpcode() == NEW
                                    && "com/gildedgames/the_aether/containers/inventory/InventoryAccessories".equals(((TypeInsnNode)insn).desc))
                                ((TypeInsnNode)insn).desc = "git/jbredwards/baubleya/util/BaublesAccessoryInventory";

                            else if(insn.getOpcode() == INVOKESPECIAL
                                    && "com/gildedgames/the_aether/containers/inventory/InventoryAccessories".equals(((MethodInsnNode)insn).owner)) {
                                ((MethodInsnNode)insn).owner = "git/jbredwards/baubleya/util/BaublesAccessoryInventory";
                                break;
                            }
                        }
                    }
                    /*
                     * updateAccessories:
                     * Old code:
                     * if (!this.thePlayer.world.isRemote)
                     * {
                     *     AetherNetworkingManager.sendToAll(new PacketAccessory(this));
                     * }
                     *
                     * New code:
                     * // Remove Aether's accessory sync code, this is now handled by Baubles
                     * -----
                     */
                    else if("updateAccessories".equals(method.name)) {
                        method.instructions.clear();
                        method.instructions.add(new InsnNode(RETURN));
                        break;
                    }
                }

                // writes the changes
                @Nonnull final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(writer);
                return writer.toByteArray();
            }
            // AccessoriesLayer | LayerElytraAether | PlayerGloveRenderer | AbilityAccessories
            else if("com.gildedgames.the_aether.client.renders.entities.layer.AccessoriesLayer".equals(transformedName)
            || "com.gildedgames.the_aether.client.renders.entities.layer.LayerElytraAether".equals(transformedName)
            || "com.gildedgames.the_aether.client.PlayerGloveRenderer".equals(transformedName)
            || "com.gildedgames.the_aether.player.abilities.AbilityAccessories".equals(transformedName)) {
                @Nonnull final ClassNode classNode = new ClassNode();
                new ClassReader(basicClass).accept(classNode, ClassReader.SKIP_FRAMES);
                /*
                 * Old code:
                 * accessories.getStackInSlot(...)
                 *
                 * New code:
                 * // Change hardcoded indexes checker to use bauble types instead
                 * ((git.jbredwards.baubleya.util.BaublesAccessoryInventory)accessories).findStackForType(...)
                 */
                for(@Nonnull final MethodNode method : classNode.methods) {
                    for(@Nonnull final AbstractInsnNode insn : method.instructions.toArray()) {
                        if(insn instanceof MethodInsnNode && ((MethodInsnNode)insn).name.equals(FMLLaunchHandler.isDeobfuscatedEnvironment() ? "getStackInSlot" : "func_70301_a")) {
                            @Nonnull final InsnList list = new InsnList();
                            list.add(new TypeInsnNode(CHECKCAST, "git/jbredwards/baubleya/util/BaublesAccessoryInventory"));
                            list.add(new FieldInsnNode(GETSTATIC, "baubles/api/BaubleType", getBaubleFor(insn.getPrevious()), "Lbaubles/api/BaubleType;"));
                            list.add(new MethodInsnNode(INVOKEVIRTUAL, "git/jbredwards/baubleya/util/BaublesAccessoryInventory", "findStackForType", "(Lbaubles/api/BaubleType;)Lnet/minecraft/item/ItemStack;", false));

                            method.instructions.insert(insn, list);
                            method.instructions.remove(insn.getPrevious());
                            method.instructions.remove(insn);
                        }
                    }
                }

                // writes the changes
                @Nonnull final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(writer);
                return writer.toByteArray();
            }

            return basicClass;
        }

        @Nonnull
        static String getBaubleFor(@Nonnull final AbstractInsnNode insn) {
            // "pendant", "cape", "shield", "misc", "ring", "ring", "gloves", "misc"
            switch(insn instanceof IntInsnNode ? ((IntInsnNode)insn).operand : (insn.getOpcode() - 3)) {
                case 0: return "AMULET";
                case 1: return "BODY";
                case 2: return "HEAD";
                // case 3: return "CHARM";
                case 4:
                case 5: return "RING";
                case 6: return "BELT";
                default: return "CHARM";
            }
        }
    }

    @Nonnull
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"git.jbredwards.baubleya.asm.ASMHandler$Transformer"};
    }

    @Nullable
    @Override
    public String getModContainerClass() { return null; }

    @Nullable
    @Override
    public String getSetupClass() { return null; }

    @Override
    public void injectData(@Nonnull final Map<String, Object> map) {}

    @Nullable
    @Override
    public String getAccessTransformerClass() { return null; }
}
