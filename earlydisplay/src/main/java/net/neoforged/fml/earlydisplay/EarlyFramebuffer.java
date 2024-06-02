/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.earlydisplay;

import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL32C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL32C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL32C.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL32C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL32C.GL_NEAREST;
import static org.lwjgl.opengl.GL32C.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL32C.GL_RGBA;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL32C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL32C.glActiveTexture;
import static org.lwjgl.opengl.GL32C.glBindFramebuffer;
import static org.lwjgl.opengl.GL32C.glBindTexture;
import static org.lwjgl.opengl.GL32C.glBlitFramebuffer;
import static org.lwjgl.opengl.GL32C.glClear;
import static org.lwjgl.opengl.GL32C.glClearColor;
import static org.lwjgl.opengl.GL32C.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL32C.glDeleteTextures;
import static org.lwjgl.opengl.GL32C.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL32C.glGenFramebuffers;
import static org.lwjgl.opengl.GL32C.glGenTextures;
import static org.lwjgl.opengl.GL32C.glReadBuffer;
import static org.lwjgl.opengl.GL32C.glReadPixels;
import static org.lwjgl.opengl.GL32C.glTexImage2D;
import static org.lwjgl.opengl.GL32C.glTexParameterIi;

public class EarlyFramebuffer {
    private final int framebuffer;
    private final int texture;

    private final RenderElement.DisplayContext context;

    EarlyFramebuffer(final RenderElement.DisplayContext context) {
        this.context = context;
        this.framebuffer = glGenFramebuffers();
        this.texture = glGenTextures();
        glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, this.texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, context.scaledWidth(), context.scaledHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (IntBuffer) null);
        glTexParameterIi(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterIi(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.texture, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    void activate() {
        glBindFramebuffer(GL_FRAMEBUFFER, this.framebuffer);
    }

    void deactivate() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    void draw(int windowFBWidth, int windowFBHeight) {
        var wscale = ((float) windowFBWidth / this.context.width());
        var hscale = ((float) windowFBHeight / this.context.height());
        var scale = this.context.scale() * Math.min(wscale, hscale) / 2f;
        var wleft = (int) (windowFBWidth * 0.5f - scale * this.context.width());
        var wtop = (int) (windowFBHeight * 0.5f - scale * this.context.height());
        var wright = (int) (windowFBWidth * 0.5f + scale * this.context.width());
        var wbottom = (int) (windowFBHeight * 0.5f + scale * this.context.height());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.framebuffer);
        final var colour = this.context.colourScheme().background();
        glClearColor(colour.redf(), colour.greenf(), colour.bluef(), 1f);
        glClear(GL_COLOR_BUFFER_BIT);
        // src Y are flipped, since our FB is flipped
        glBlitFramebuffer(0, this.context.height() * this.context.scale(), this.context.width() * this.context.scale(), 0, RenderElement.clamp(wleft, 0, windowFBWidth), RenderElement.clamp(wtop, 0, windowFBHeight), RenderElement.clamp(wright, 0, windowFBWidth), RenderElement.clamp(wbottom, 0, windowFBHeight), GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    long takeScreenshot() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.framebuffer);
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        var pixels = MemoryUtil.nmemAlloc(context.scaledWidth() * context.scaledHeight() * 4L);
        glReadPixels(0, 0, context.scaledWidth(), context.scaledHeight(), GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return pixels;
    }

    int getTexture() {
        return this.texture;
    }

    public void close() {
        glDeleteTextures(this.texture);
        glDeleteFramebuffers(this.framebuffer);
    }
}
