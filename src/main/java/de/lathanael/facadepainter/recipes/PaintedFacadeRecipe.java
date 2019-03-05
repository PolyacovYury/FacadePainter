package de.lathanael.facadepainter.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import crazypants.enderio.base.conduit.facade.ItemConduitFacade;
import crazypants.enderio.base.paint.PaintUtil;
import crazypants.enderio.base.recipe.IMachineRecipe;
import crazypants.enderio.base.recipe.MachineRecipeRegistry;
import crazypants.enderio.base.recipe.painter.AbstractPainterTemplate;

import de.lathanael.facadepainter.config.Configs;
import de.lathanael.facadepainter.init.ItemRegistry;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

public class PaintedFacadeRecipe extends ShapelessRecipes {

    public PaintedFacadeRecipe(@Nullable final ResourceLocation group, final NonNullList<Ingredient> input, final ItemStack output) {
        super(group.toString(), output, input);
    }

    @Override
    public boolean matches(final InventoryCrafting inventory, final World worldIn) {
        int ingredientCount = 0;
        ItemStack facade = null;
        ItemStack paintSource = null;
        ItemStack chamaeleo = null;
        
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                ++ingredientCount;
                // Get the Facade ItemStack
                if (itemstack.getItem() instanceof ItemConduitFacade) {
                    facade = itemstack;
                    continue;
                }
                // Get the Chamaeleo Paint item
                if (itemstack.getItem() == ItemRegistry.itemChamaeleoPaint) {
                    chamaeleo = itemstack;
                    continue;
                }
                // Since it is neither the facade nor the Chamaeleo Paint it must be the intended paint source,
                // we check later if it is valid! This gets overwritten if more than one different stack is
                // present in the crafting grid but the size check will catch that.
                paintSource = itemstack;
            }
        }
        if (ingredientCount < 2) {
            return false;
        } else if (ingredientCount > 3) {
            return false;
        } else if (ingredientCount < 3 && Configs.recipes.useChamaeleoPaint) {
            return false;
        } else if (ingredientCount > 2 && !Configs.recipes.useChamaeleoPaint) {
            return false;
        }
        if (facade == null || paintSource == null) {
            return false;
        }
        if (Configs.recipes.useChamaeleoPaint && chamaeleo == null) {
            return false;
        }

        // Check the EIO painter recipe registry if a valid recipe exists
        Map<String, IMachineRecipe> painterRecipes = MachineRecipeRegistry.instance.getRecipesForMachine(MachineRecipeRegistry.PAINTER);
        for (IMachineRecipe rec : painterRecipes.values()) {
            if (rec instanceof AbstractPainterTemplate<?>) {
                AbstractPainterTemplate<?> temp = (AbstractPainterTemplate<?>) rec;
                if (temp.isPartialRecipe(paintSource, facade)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final InventoryCrafting inventory) {
        NonNullList<ItemStack> returnStack = NonNullList.<ItemStack>withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < returnStack.size(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                if (itemstack.getItem() instanceof ItemConduitFacade || itemstack.getItem() == ItemRegistry.itemChamaeleoPaint) {
                    continue;
                }
                // TODO: Find a better (?) solution for stacks with a size greater than 1.
                if (itemstack.getCount() > 1) {
                    itemstack = itemstack.copy();
                    itemstack.setCount(1);
                    returnStack.set(i, itemstack);
                } else {
                    returnStack.set(i, itemstack.copy());
                }
            }
        }

        return returnStack;
    }

    @Override
    public ItemStack getCraftingResult(final InventoryCrafting inventory) {
        ItemStack facade = null;
        ItemStack paintSource = null;
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
                if (itemstack.getItem() instanceof ItemConduitFacade) {
                    facade = itemstack.copy();
                    continue;
                }
                if (itemstack.getItem() == ItemRegistry.itemChamaeleoPaint) {
                    continue;
                }
                paintSource = itemstack;
            }
        }
        PaintUtil.setPaintSource(facade, paintSource);
        Block paintBlock = PaintUtil.getBlockFromItem(paintSource);
        if (paintBlock == null) {
            return super.getCraftingResult(inventory);
        }
        IBlockState paintState = PaintUtil.Block$getBlockFromItem_stack$getItem___$getStateFromMeta_stack$getMetadata___(paintSource, paintBlock);
        if (paintState == null) {
            return super.getCraftingResult(inventory);
        }
        PaintUtil.setSourceBlock(facade, paintState);
        facade.setCount(1);

        return facade;
    }

    @Override
    public boolean canFit(final int width, final int height) {
        if (Configs.recipes.useChamaeleoPaint) {
            return width * height > 2;
        }
        return width * height > 1;
    }
    
    public static class Factory implements IRecipeFactory {

        @Override
        public IRecipe parse(final JsonContext context, final JsonObject json) {
            final String group = JsonUtils.getString(json, "group", "");
            final ItemStack result = CraftingHelper.getItemStack(JsonUtils.getJsonObject(json, "result"), context);

            final NonNullList<Ingredient> ingredients = NonNullList.create();
            for (JsonElement element : JsonUtils.getJsonArray(json, "ingredients")) {
                ingredients.add(CraftingHelper.getIngredient(element, context));
            }
            if (ingredients.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            }

            return new PaintedFacadeRecipe(group.isEmpty() ? null : new ResourceLocation(group), ingredients, result);
        }
    }
}