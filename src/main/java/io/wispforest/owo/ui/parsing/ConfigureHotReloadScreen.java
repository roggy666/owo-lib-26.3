package io.wispforest.owo.ui.parsing;

import io.wispforest.owo.Owo;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.util.CommandOpenedScreen;
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ConfigureHotReloadScreen extends BaseUIModelScreen<FlowLayout> implements CommandOpenedScreen {

    private final @Nullable Screen parent;

    private final Identifier modelId;
    private @Nullable Path reloadLocation;

    private LabelComponent fileNameLabel;
    private boolean dialogOpen = false;

    public ConfigureHotReloadScreen(Identifier modelId, @Nullable Screen parent) {
        super(FlowLayout.class, DataSource.asset(Owo.id("configure_hot_reload")));
        this.parent = parent;

        this.modelId = modelId;
        this.reloadLocation = UIModelLoader.getHotReloadPath(this.modelId);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.childById(LabelComponent.class, "ui-model-label").text(Component.translatable("text.owo.configure_hot_reload.model", this.modelId));
        this.fileNameLabel = rootComponent.childById(LabelComponent.class, "file-name-label");
        this.updateFileNameLabel();

        rootComponent.childById(ButtonComponent.class, "choose-button").onPress(button -> this.openFileDialog());

        rootComponent.childById(ButtonComponent.class, "save-button").onPress(button -> {
            UIModelLoader.setHotReloadPath(this.modelId, this.reloadLocation);
            this.onClose();
        });

        rootComponent.childById(LabelComponent.class, "close-label").mouseDown().subscribe((click, doubled) -> {
            UISounds.playInteractionSound();
            this.onClose();
            return true;
        });
    }

    /**
     * Opens the platform's native file chooser through SDL. The result is delivered
     * asynchronously (possibly on another thread), so it is handed back to the client
     * thread before the screen state is touched
     */
    private void openFileDialog() {
        if (this.dialogOpen) return;
        this.dialogOpen = true;

        var client = Minecraft.getInstance();
        var callbackHolder = new SDL_DialogFileCallback[1];

        callbackHolder[0] = SDL_DialogFileCallback.create((userdata, fileList, filterIndex) -> {
            String selected = null;
            if (fileList != MemoryUtil.NULL) {
                // fileList is a NULL-terminated array of UTF-8 strings, we only asked for a single file
                var first = MemoryUtil.memGetAddress(fileList);
                if (first != MemoryUtil.NULL) {
                    selected = MemoryUtil.memUTF8(first);
                }
            }

            var result = selected;
            client.execute(() -> {
                callbackHolder[0].free();
                this.dialogOpen = false;

                if (result != null) {
                    try {
                        this.reloadLocation = Path.of(result);
                    } catch (InvalidPathException e) {
                        Owo.LOGGER.error("Failed to parse path '{}' chosen for hot reload", result, e);
                    }
                }

                this.updateFileNameLabel();
            });
        });

        SDLDialog.SDL_ShowOpenFileDialog(
            callbackHolder[0],
            MemoryUtil.NULL,
            client.getWindow().handle(),
            null,
            (CharSequence) null,
            false
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    private void updateFileNameLabel() {
        this.fileNameLabel.text(Component.translatable(
                "text.owo.configure_hot_reload.reload_from",
                this.reloadLocation == null ? Component.translatable("text.owo.configure_hot_reload.reload_from.unset") : this.reloadLocation
        ));
    }
}
