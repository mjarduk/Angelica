package ru.marduk.heretic.client;

import org.lwjgl.openxr.XrExtensionProperties;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import ru.marduk.heretic.HereticConfig;
import ru.marduk.heretic.HereticMod;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openxr.EXTDebugUtils.*;
import static org.lwjgl.openxr.KHROpenGLEnable.*;
import static org.lwjgl.openxr.XR10.*;

/**
 * The primary class responsible for
 * interacting with the HMD.
 */
public class HMDService {
    public static final HMDService INSTANCE = new HMDService();
    private boolean canWeVRToBeginWith = false;
    private String whyCantWeVR = "";
    private boolean areWeVRYet = false;

    public void init() {
        HereticMod.LOGGER.info("!! HereticVR init !!");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer intBuf = stack.mallocInt(1);

            checkXRError(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, intBuf, null));
            int extensionCount = intBuf.get(0);

            XrExtensionProperties.Buffer properties = fill(
                XrExtensionProperties.calloc(extensionCount, stack),
                XrExtensionProperties.TYPE,
                XR_TYPE_EXTENSION_PROPERTIES
            );

            checkXRError(xrEnumerateInstanceExtensionProperties((ByteBuffer) null, intBuf, properties));

            boolean hasGL = false;
            boolean hasDebugExt = false;

            StringBuilder extensionsString = new StringBuilder();
            for (int i = 0; i < extensionCount; i++){
                String name = properties.get(i).extensionNameString();

                if (name.equals(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME))
                    hasGL = true;

                if (HereticConfig.DebugOptions.useDebugUtils && name.equals(XR_EXT_DEBUG_UTILS_EXTENSION_NAME))
                    hasDebugExt = true;

                extensionsString.append(name);
                extensionsString.append(" ");
            }

            HereticMod.LOGGER.info("{} available extensions:", extensionCount);
            HereticMod.LOGGER.info("{}", extensionsString);

            if (!hasGL) {
                whyCantWeVR = XR_KHR_OPENGL_ENABLE_EXTENSION_NAME + " is unavailable!";
                HereticMod.LOGGER.fatal("{}", whyCantWeVR);
                return;
            }

            if (HereticConfig.DebugOptions.useDebugUtils && !hasDebugExt) {
                whyCantWeVR = XR_EXT_DEBUG_UTILS_EXTENSION_NAME + " explicitly requested, but not found!";
                HereticMod.LOGGER.fatal("{}", whyCantWeVR);
                return;
            }

            canWeVRToBeginWith = true;
        }
    }

    /**
     * Starts the OpenXR instance along
     * with the other structures.
     * <p>
     * TODO
     * note: should probs be able to
     * start/restart as many times as you want,
     * also fast enough to e.g. put it on in a second
     */
    public void initHMD() {

    }

    /**
     * Stops & destroys the OpenXR instance
     * along with other used structures.
     * <p>
     * TODO
     * @see HMDService#initHMD()
     */
    public void stopHMD() {

    }

    /**
     * Whether the HMD is online.
     */
    public boolean isAreWeVRYet() {
        return areWeVRYet;
    }

    /**
     * Whether OpenXR successfully initialized.
     */
    public boolean canWeVRToBeginWith() {
        return canWeVRToBeginWith;
    }

    /**
     * Returns an error message explaining why init failed.
     */
    public String whyCantWeVR() {
        return whyCantWeVR;
    }

    /**
     * Prepend this to every OpenXR call you make to make sure
     * that errors will be handled.
     * @param result function output goes here
     */
    public void checkXRError(int result) {
        if (!HereticConfig.DebugOptions.errorChecking)
            return;

        if (XR_SUCCEEDED(result))
            return;

        String desc = "unknown";

        HereticMod.LOGGER.error("########## XR ERROR ##########");
        HereticMod.LOGGER.error("{}: {}", result, desc);
    }

    // Excerpt from XRHelper.java
    private static <S extends Struct<S>, T extends StructBuffer<S, T>> T fill(T buffer, int offset, int value) {
        long ptr    = buffer.address() + offset;
        int  stride = buffer.sizeof();
        for (int i = 0; i < buffer.limit(); i++) {
            MemoryUtil.memPutInt(ptr + (long) i * stride, value);
        }
        return buffer;
    }
}
