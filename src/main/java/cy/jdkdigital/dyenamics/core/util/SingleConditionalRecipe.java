package cy.jdkdigital.dyenamics.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.ConditionalAdvancement;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SingleConditionalRecipe
{
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder
    {
        private final List<ICondition> conditions = new ArrayList<>();
        private FinishedRecipe recipe = null;
        private ResourceLocation advId;
        private ConditionalAdvancement.Builder adv;

        public Builder addCondition(ICondition condition) {
            conditions.add(condition);
            return this;
        }

        public Builder setRecipe(Consumer<Consumer<FinishedRecipe>> callable) {
            callable.accept(this::setRecipe);
            return this;
        }

        public Builder setRecipe(BiConsumer<Consumer<FinishedRecipe>, ResourceLocation> callable, ResourceLocation recipeId) {
            callable.accept(this::setRecipe, recipeId);
            return this;
        }

        public Builder setRecipe(FinishedRecipe recipe) {
            if (conditions.isEmpty()) {
                throw new IllegalStateException("Cannot add a recipe with no conditions.");
            }
            this.recipe = recipe;
            return this;
        }

        public Builder generateAdvancement(@Nullable ResourceLocation id) {
            ConditionalAdvancement.Builder builder = ConditionalAdvancement.builder();
            for (ICondition cond : conditions) {
                builder = builder.addCondition(cond);
            }
            builder = builder.addAdvancement(recipe);
            return setAdvancement(id, builder);
        }

        public Builder setAdvancement(ConditionalAdvancement.Builder advancement) {
            return setAdvancement(null, advancement);
        }

        public Builder setAdvancement(@Nullable ResourceLocation id, ConditionalAdvancement.Builder advancement) {
            if (this.adv != null) {
                throw new IllegalStateException("Invalid SingleConditionalRecipeBuilder, Advancement already set");
            }
            this.advId = id;
            this.adv = advancement;
            return this;
        }

        public void build(Consumer<FinishedRecipe> consumer, String namespace, String path) {
            build(consumer, new ResourceLocation(namespace, path));
        }

        public void build(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
            if (recipe == null) {
                throw new IllegalStateException("Invalid SingleConditionalRecipe builder, No recipe");
            }
            if (advId == null && adv != null) {
                advId = new ResourceLocation(id.getNamespace(), "recipes/" + id.getPath());
            }

            consumer.accept(new Finished(id, conditions, recipe, advId, adv));
        }
    }

    private record Finished(ResourceLocation id,
                            List<ICondition> conditions,
                            FinishedRecipe recipe,
                            ResourceLocation advId,
                            ConditionalAdvancement.Builder adv) implements FinishedRecipe
    {
        @Override
        public void serializeRecipeData(JsonObject json) {
        }

        @Override
        public JsonObject serializeRecipe() {
            JsonObject json = recipe.serializeRecipe();

            JsonArray jsonConditions = new JsonArray();
            for (ICondition condition : this.conditions) {
                jsonConditions.add(CraftingHelper.serialize(condition));
            }
            json.add("conditions", jsonConditions);

            return json;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return recipe.getType();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return adv == null ? null : adv.write();
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return advId;
        }
    }
}
