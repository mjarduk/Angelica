package ru.marduk.heretic.client;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import org.lwjgl.MemoryUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL31;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import org.lwjglx.opengl.Display;
import ru.marduk.heretic.HereticConfig;
import ru.marduk.heretic.HereticMod;
import ru.marduk.heretic.client.openxr.Swapchain;
import ru.marduk.heretic.client.openxr.XRutil;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static com.gtnewhorizons.angelica.glsm.GLStateManager.glGetInteger;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.system.MemoryUtil.*;
import static ru.marduk.heretic.client.openxr.XRutil.*;
import static org.lwjgl.openxr.EXTDebugUtils.*;
import static org.lwjgl.openxr.KHROpenGLEnable.*;
import static org.lwjgl.openxr.XR10.*;

/**
 * The primary class responsible for
 * interacting with the HMD.
 */
public class HMDService {
    public static final HMDService INSTANCE = new HMDService();

    private static final int VIEW_CONFIG_TYPE = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;

    private XrInstance xrInstance;
    private long systemId;
    private XrSession session;
    private XrDebugUtilsMessengerEXT debugMessenger;
    private XrSpace appSpace;
    private long glColorFormat;
    private XrView.Buffer views;
    private Swapchain[] swapchains;
    private XrViewConfigurationView.Buffer viewConfigurations;


    private boolean canWeVRToBeginWith = false;
    private String whyCantWeVR = "";
    private boolean areWeVRYet = false;

    public void init() {
        HereticMod.LOGGER.info("!! HereticVR init !!");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntObjectPair<XrExtensionProperties.Buffer> layerEnumerateResult = enumerateExtensions(stack);

            XrExtensionProperties.Buffer availExtensions = layerEnumerateResult.value();
            int extensionCount = layerEnumerateResult.keyInt();

            boolean hasGL = false;
            boolean hasDebugExt = false;

            StringBuilder extensionsString = new StringBuilder();
            for (int i = 0; i < extensionCount; i++) {
                String name = availExtensions.get(i).extensionNameString();

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

            IntObjectPair<XrApiLayerProperties.Buffer> apiEnumerateResult = enumerateApiLayers(stack);
            int layerCount = apiEnumerateResult.keyInt();
            XrApiLayerProperties.Buffer layers = apiEnumerateResult.value();

            boolean hasValidationLayer = false;

            StringBuilder layersString = new StringBuilder();
            for (int i = 0; i < layerCount; i++) {
                String name = layers.get(i).layerNameString();

                if (name.equals("XR_APILAYER_LUNARG_core_validation")) {
                    hasValidationLayer = true;
                }

                layersString.append(name);
                layersString.append(" ");
            }

            HereticMod.LOGGER.info("{} available API layers:", layerCount);
            HereticMod.LOGGER.info("{}", layersString);

            if (HereticConfig.DebugOptions.useValidationLayers && !hasValidationLayer) {
                whyCantWeVR = "Validation Layers (XR_APILAYER_LUNARG_core_validation) explicitly requested, but not found!";
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
        if (!canWeVRToBeginWith)
            throw new RuntimeException("Cannot launch HMD service without prior proper initialization.");

        HereticMod.LOGGER.info("Initializing HMDService...");

        whyCantWeVR = "";

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (!initXrInstance(stack))
                return;

            if (!initXrSystem(stack))
                return;

            if (!bindXr2GL(stack))
                return;

            if (!initReferenceSpace(stack))
                return;

            if (!initSwapchains(stack))
                return;
        }

        areWeVRYet = true;
    }

    /**
     * Stops & destroys the OpenXR instance
     * along with other used structures.
     * <p>
     * TODO
     *
     * @see HMDService#initHMD()
     */
    public void stopHMD() {
        if (debugMessenger != null) {
            xrDestroyDebugUtilsMessengerEXT(debugMessenger);
        }

        xrDestroySession(session);
        xrDestroyInstance(xrInstance);
        areWeVRYet = false;
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
     * Gets the raw {@link XrInstance}.
     * Tread lightly.
     */
    public XrInstance getXrInstance() {
        return xrInstance;
    }

    private boolean initXrInstance(MemoryStack stack) {
        PointerBuffer extensions = stack.mallocPointer(HereticConfig.DebugOptions.useDebugUtils ? 2 : 1);
        extensions.put(stack.UTF8(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME));
        if (HereticConfig.DebugOptions.useDebugUtils) {
            extensions.put(stack.UTF8(XR_EXT_DEBUG_UTILS_EXTENSION_NAME));
        }
        extensions.flip();

        PointerBuffer wantedLayers = null;
        if (HereticConfig.DebugOptions.useValidationLayers) {
            wantedLayers = stack.mallocPointer(1);
            wantedLayers.put(stack.UTF8("XR_APILAYER_LUNARG_core_validation"));
        }

        XrInstanceCreateInfo createInfo = XrInstanceCreateInfo.malloc(stack)
            .type$Default()
            .next(NULL)
            .createFlags(0)
            .applicationInfo(XrApplicationInfo.calloc(stack)
                .apiVersion(XR_MAKE_VERSION(1, 0, XR_VERSION_PATCH(XR_CURRENT_API_VERSION)))
                .applicationName(stack.UTF8("HereticVR"))
                .engineName(stack.UTF8("LWJGL")))
            .enabledApiLayerNames(wantedLayers)
            .enabledExtensionNames(extensions);

        PointerBuffer pp = stack.mallocPointer(1);
        int ret = xrCreateInstance(createInfo, pp);

        if (!checkXRError(ret, true)) {
            whyCantWeVR = "xrCreateInstance failure (error code " + ret + "); Is the HMD/runtime active?";

            HereticMod.LOGGER.error("{}", whyCantWeVR);
            return false;
        }

        xrInstance = new XrInstance(pp.get(0), createInfo);
        HereticMod.LOGGER.info("OpenXR instance successfully created!");

        return true;
    }

    private boolean initXrSystem(MemoryStack stack) {
        LongBuffer longBuf = stack.longs(0);

        checkXRError(xrGetSystem(
            xrInstance,
            XrSystemGetInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY),
            longBuf
        ));

        systemId = longBuf.get(0);
        if (systemId == 0) {
            whyCantWeVR = "No compatible VR headset found!";
            return false;
        }

        HereticMod.LOGGER.info("Found headset w/ System ID {}", systemId);

        return true;
    }

    private boolean bindXr2GL(MemoryStack stack) {
        XrGraphicsRequirementsOpenGLKHR graphicsRequirements =
            XrGraphicsRequirementsOpenGLKHR.malloc(stack)
                .type$Default()
                .next(NULL)
                .minApiVersionSupported(0)
                .maxApiVersionSupported(0);

        checkXRError(xrGetOpenGLGraphicsRequirementsKHR(xrInstance, systemId, graphicsRequirements));

        int glMinMajor = XR_VERSION_MAJOR(graphicsRequirements.minApiVersionSupported());
        int glMinMinor = XR_VERSION_MINOR(graphicsRequirements.minApiVersionSupported());

        int glMaxMajor = XR_VERSION_MAJOR(graphicsRequirements.maxApiVersionSupported());
        int glMaxMinor = XR_VERSION_MINOR(graphicsRequirements.maxApiVersionSupported());

        HereticMod.LOGGER.info("Current configuration supports OpenGL {}.{} thru {}.{}", glMinMajor, glMinMinor, glMaxMajor, glMaxMinor);

        int actualMajorVersion = glGetInteger(GL_MAJOR_VERSION);
        int actualMinorVersion = glGetInteger(GL_MINOR_VERSION);

        HereticMod.LOGGER.info("In turn, our max version is {}.{}", actualMajorVersion, actualMinorVersion);

        if (glMinMajor > actualMajorVersion || (glMinMajor == actualMajorVersion && glMinMinor > actualMinorVersion)) {
            whyCantWeVR = "XR requests at least OpenGL " + glMinMajor + "." + glMinMinor + ", but we only have " + actualMajorVersion + "." + actualMinorVersion;
            HereticMod.LOGGER.error(whyCantWeVR);
            return false;
        }

        PointerBuffer pp = stack.mallocPointer(1);
        try {
            if (!checkXRError(xrCreateSession(
                xrInstance,
                XRutil.createGraphicsBindingOpenGL(
                    XrSessionCreateInfo.malloc(stack)
                        .type$Default()
                        .next(NULL)
                        .createFlags(0)
                        .systemId(systemId),
                    stack,
                    Display.getWindow()
                ),
                pp
            ), true)) {
                whyCantWeVR = "Failed to bind OpenXR to the OpenGL context";
                return false;
            }
        } catch (Exception e) {
            whyCantWeVR = e.getMessage();
            return false;
        }

        session = new XrSession(pp.get(0), xrInstance);

        if (HereticConfig.DebugOptions.useDebugUtils) {
            XrDebugUtilsMessengerCreateInfoEXT ciDebugUtils = XrDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .type$Default()
                .messageSeverities(
                    XR_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                .messageTypes(
                    XR_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT |
                        XR_DEBUG_UTILS_MESSAGE_TYPE_CONFORMANCE_BIT_EXT
                )
                .userCallback((messageSeverity, messageTypes, pCallbackData, userData) -> {
                    try (XrDebugUtilsMessengerCallbackDataEXT callbackData = XrDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)) {
                        HereticMod.LOGGER.info("XR debug: {}", callbackData.messageString());
                        return 0;
                    }
                });

            if (checkXRError(xrCreateDebugUtilsMessengerEXT(xrInstance, ciDebugUtils, pp))) {
                HereticMod.LOGGER.info("Enabled debug utils messenger");
                debugMessenger = new XrDebugUtilsMessengerEXT(pp.get(0), xrInstance);
            }
        }

        return true;
    }

    private boolean initReferenceSpace(MemoryStack stack) {
        PointerBuffer pp = stack.mallocPointer(1);

        if (!checkXRError(xrCreateReferenceSpace(
            session,
            XrReferenceSpaceCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
                .poseInReferenceSpace(XrPosef.malloc(stack)
                    .orientation(XrQuaternionf.malloc(stack)
                        .x(0)
                        .y(0)
                        .z(0)
                        .w(1))
                    .position$(XrVector3f.calloc(stack))),
            pp
        ))) {
            whyCantWeVR = "Failed to create reference space!";
            return false;
        }

        appSpace = new XrSpace(pp.get(0), session);

        return true;
    }

    private boolean initSwapchains(MemoryStack stack) {

        XrSystemProperties systemProperties = XrSystemProperties.calloc(stack)
            .type$Default();
        checkXRError(xrGetSystemProperties(xrInstance, systemId, systemProperties));

        HereticMod.LOGGER.info("HMD: {} (vendor {})", systemProperties.systemNameString(), systemProperties.vendorId());
        HereticMod.LOGGER.info("=== SPECS ===");

        XrSystemTrackingProperties trackingProperties = systemProperties.trackingProperties();
        HereticMod.LOGGER.info("3DOF: {}; 6DOF: {}", trackingProperties.orientationTracking(), trackingProperties.positionTracking());

        XrSystemGraphicsProperties graphicsProperties = systemProperties.graphicsProperties();
        HereticMod.LOGGER.info("Max resolution: {}x{}", graphicsProperties.maxSwapchainImageWidth(), graphicsProperties.maxSwapchainImageHeight());
        HereticMod.LOGGER.info("Max composition layers: {}", graphicsProperties.maxLayerCount());

        IntBuffer intBuf = stack.mallocInt(1);

        checkXRError(xrEnumerateViewConfigurationViews(xrInstance, systemId, VIEW_CONFIG_TYPE, intBuf, null));

        viewConfigurations = XRutil.fill(
            XrViewConfigurationView.calloc(intBuf.get(0)),
            XrViewConfigurationView.TYPE,
            XR_TYPE_VIEW_CONFIGURATION_VIEW
        );

        checkXRError(xrEnumerateViewConfigurationViews(xrInstance, systemId, VIEW_CONFIG_TYPE, intBuf, viewConfigurations));
        int viewCount = intBuf.get(0);

        if (viewCount < 1) {
            whyCantWeVR = "xrEnumerateViewConfigurationViews returned >1 views!";
            return false;
        }

        views = XRutil.fill(
            XrView.calloc(viewCount),
            XrView.TYPE,
            XR_TYPE_VIEW
        );

        checkXRError(xrEnumerateSwapchainFormats(session, intBuf, null));
        LongBuffer swapchainFormats = stack.mallocLong(intBuf.get(0));

        long[] desiredSwapchainFormats = {
            GL_RGB10_A2,
            GL_SRGB8,
            GL_RGBA16F,
            // The two below should only be used as a fallback, as they are linear color formats without enough bits for color
            // depth, thus leading to banding.
            GL_RGBA8,
            GL31.GL_RGBA8_SNORM
        };

        out:
        for (long glFormatIter : desiredSwapchainFormats) {
            for (int i = 0; i < swapchainFormats.limit(); i++) {
                if (glFormatIter == swapchainFormats.get(i)) {
                    glColorFormat = glFormatIter;
                    break out;
                }
            }
        }

        if (glColorFormat == 0) {
            whyCantWeVR = "No compatible framebuffer format available!";
            return false;
        }

        swapchains = new Swapchain[viewCount];

        for (int i = 0; i < viewCount; i++) {
            XrViewConfigurationView viewConfig = viewConfigurations.get(i);

            Swapchain swapchainWrapper = new Swapchain();

            XrSwapchainCreateInfo swapchainCreateInfo = XrSwapchainCreateInfo.malloc(stack)
                .type$Default()
                .next(NULL)
                .createFlags(0)
                .usageFlags(XR_SWAPCHAIN_USAGE_SAMPLED_BIT | XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT)
                .format(glColorFormat)
                .sampleCount(viewConfig.recommendedSwapchainSampleCount())
                .width(viewConfig.recommendedImageRectWidth())
                .height(viewConfig.recommendedImageRectHeight())
                .faceCount(1)
                .arraySize(1)
                .mipCount(1);

            PointerBuffer pp = stack.mallocPointer(1);
            checkXRError(xrCreateSwapchain(session, swapchainCreateInfo, pp));

            swapchainWrapper.handle = new XrSwapchain(pp.get(0), session);
            swapchainWrapper.width = swapchainCreateInfo.width();
            swapchainWrapper.height = swapchainCreateInfo.height();

            checkXRError(xrEnumerateSwapchainImages(swapchainWrapper.handle, intBuf, null));
            int imageCount = intBuf.get(0);

            XrSwapchainImageOpenGLKHR.Buffer swapchainImageBuffer = XRutil.fill(
                XrSwapchainImageOpenGLKHR.calloc(imageCount),
                XrSwapchainImageOpenGLKHR.TYPE,
                XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR
            );

            checkXRError(xrEnumerateSwapchainImages(swapchainWrapper.handle, intBuf, XrSwapchainImageBaseHeader.create(swapchainImageBuffer)));
            swapchainWrapper.images = swapchainImageBuffer;
            swapchains[i] = swapchainWrapper;
        }

        return true;
    }
}
