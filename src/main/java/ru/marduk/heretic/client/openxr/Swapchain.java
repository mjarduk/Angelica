package ru.marduk.heretic.client.openxr;

import org.lwjgl.openxr.XrSwapchain;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

public class Swapchain {
    public XrSwapchain handle;
    public int width;
    public int height;
    public XrSwapchainImageOpenGLKHR.Buffer images;
}
