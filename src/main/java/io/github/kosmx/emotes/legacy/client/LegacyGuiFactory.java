package io.github.kosmx.emotes.legacy.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import java.util.Collections;
import java.util.Set;
public final class LegacyGuiFactory implements IModGuiFactory {
    public void initialize(Minecraft minecraft) { }
    public boolean hasConfigGui(){return true;}
    public GuiScreen createConfigGui(GuiScreen parent){return new EmoteSettingsScreen(parent);}
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories(){return Collections.emptySet();}
}
