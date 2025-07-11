package ru.marduk.heretic.client.openxr;

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.lwjgl.openxr.*;
import org.lwjgl.system.*;
import org.lwjgl.system.linux.XVisualInfo;
import ru.marduk.heretic.HereticConfig;
import ru.marduk.heretic.HereticMod;
import ru.marduk.heretic.client.HMDService;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeGLX.*;
import static org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext;
import static org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window;
import static org.lwjgl.glfw.GLFWNativeX11.*;
import static org.lwjgl.opengl.GLX.glXGetCurrentDrawable;
import static org.lwjgl.opengl.GLX13.glXGetVisualFromFBConfig;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.system.windows.User32.GetDC;

public class XRutil {
    private static final HMDService HMD = HMDService.INSTANCE;

    public static IntObjectPair<XrExtensionProperties.Buffer> enumerateExtensions(MemoryStack stack) {
        IntBuffer intBuf = stack.mallocInt(1);

        checkXRError(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, intBuf, null));
        int extensionCount = intBuf.get(0);

        XrExtensionProperties.Buffer properties = fill(XrExtensionProperties.calloc(extensionCount, stack), XrExtensionProperties.TYPE, XR_TYPE_EXTENSION_PROPERTIES);

        checkXRError(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, intBuf, properties));

        return new IntObjectImmutablePair<>(extensionCount, properties);
    }

    public static IntObjectPair<XrApiLayerProperties.Buffer> enumerateApiLayers(MemoryStack stack) {
        IntBuffer intBuf = stack.mallocInt(1);

        checkXRError(xrEnumerateApiLayerProperties(intBuf, null));
        int layerCount = intBuf.get(0);

        XrApiLayerProperties.Buffer layers = fill(XrApiLayerProperties.calloc(layerCount, stack), XrApiLayerProperties.TYPE, XR_TYPE_API_LAYER_PROPERTIES);

        checkXRError(xrEnumerateApiLayerProperties(intBuf, layers));

        return new IntObjectImmutablePair<>(layerCount, layers);
    }

    /**
     * Prepend this to every OpenXR call you make to make sure
     * that errors will be handled.
     */
    public static boolean checkXRError(int result) {
        return checkXRError(result, false);
    }

    /**
     * Prepend this to every OpenXR call you make to make sure
     * that errors will be handled.
     */
    public static boolean checkXRError(int result, boolean force) {
        if (!HereticConfig.DebugOptions.errorChecking && !force) return true;

        if (XR_SUCCEEDED(result)) return true;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            String desc = "unknown";

            if (HMD.getXrInstance() != null) {
                ByteBuffer str = stack.malloc(XR_MAX_RESULT_STRING_SIZE);

                if (xrResultToString(HMD.getXrInstance(), result, str) >= 0) {
                    desc = MemoryUtil.memUTF8(str);
                }
            }

            HereticMod.LOGGER.error("########## XR ERROR ##########");
            HereticMod.LOGGER.error("{}: {}", result, desc);
        }

        return false;
    }

    public static XrSessionCreateInfo createGraphicsBindingOpenGL(XrSessionCreateInfo sessionCreateInfo, MemoryStack stack, long window) throws IllegalStateException {
        switch (Platform.get()) {
            case LINUX:
                int platform = glfwGetPlatform();
                if (platform == GLFW_PLATFORM_X11) {
                    long display = glfwGetX11Display();
                    long glxConfig = glfwGetGLXFBConfig(window);

                    try (XVisualInfo visualInfo = glXGetVisualFromFBConfig(display, glxConfig)) {
                        if (visualInfo == null) {
                            throw new IllegalStateException("Failed to get visual info");
                        }
                        long visualid = visualInfo.visualid();

                        HereticMod.LOGGER.info("Using XrGraphicsBindingOpenGLXlibKHR to create the session");
                        return sessionCreateInfo.next(XrGraphicsBindingOpenGLXlibKHR.malloc(stack).type$Default().xDisplay(display).visualid((int) visualid).glxFBConfig(glxConfig).glxDrawable(glXGetCurrentDrawable()).glxContext(glfwGetGLXContext(window)));
                    }
                } else {
                    throw new IllegalStateException("X11 is the only Linux windowing system with explicit OpenXR support. All other Linux systems must use EGL.");
                }
            case WINDOWS:
                HereticMod.LOGGER.info("Using XrGraphicsBindingOpenGLWin32KHR to create the session");
                return sessionCreateInfo.next(XrGraphicsBindingOpenGLWin32KHR.malloc(stack).type$Default().hDC(GetDC(glfwGetWin32Window(window))).hGLRC(glfwGetWGLContext(window)));
            default:
                throw new IllegalStateException("Windows and Linux are the only platforms with explicit OpenXR support. All other platforms must use EGL.");
        }
    }

    // Excerpt from XRHelper.java
    public static <S extends Struct<S>, T extends StructBuffer<S, T>> T fill(T buffer, int offset, int value) {
        long ptr = buffer.address() + offset;
        int stride = buffer.sizeof();
        for (int i = 0; i < buffer.limit(); i++) {
            MemoryUtil.memPutInt(ptr + (long) i * stride, value);
        }
        return buffer;
    }
}
