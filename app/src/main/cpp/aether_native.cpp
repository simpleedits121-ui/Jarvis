#include <jni.h>
#include <android/log.h>
#include <vulkan/vulkan.h>

#include <algorithm>
#include <cstdint>
#include <sstream>
#include <string>

#define AETHER_TAG "AETHER"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, AETHER_TAG, __VA_ARGS__)

namespace {

std::string probeVulkan() {
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "AETHER";
    appInfo.applicationVersion = VK_MAKE_VERSION(0, 1, 0);
    appInfo.pEngineName = "AETHER Adaptive Character Engine";
    appInfo.engineVersion = VK_MAKE_VERSION(0, 1, 0);
    appInfo.apiVersion = VK_API_VERSION_1_0;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;

    VkInstance instance = VK_NULL_HANDLE;
    const VkResult createResult = vkCreateInstance(&createInfo, nullptr, &instance);
    if (createResult != VK_SUCCESS || instance == VK_NULL_HANDLE) {
        std::ostringstream out;
        out << "Vulkan unavailable (vkCreateInstance=" << static_cast<int>(createResult)
            << "). Diagnostics-only fallback is active.";
        return out.str();
    }

    uint32_t count = 0;
    VkResult result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
    if (result != VK_SUCCESS || count == 0) {
        vkDestroyInstance(instance, nullptr);
        return "Vulkan loader found, but no physical device was enumerated.";
    }

    VkPhysicalDevice devices[8]{};
    const uint32_t capacity = std::min(count, 8u);
    uint32_t deviceCount = capacity;
    result = vkEnumeratePhysicalDevices(instance, &deviceCount, devices);
    if (result != VK_SUCCESS || capacity == 0 || deviceCount == 0) {
        vkDestroyInstance(instance, nullptr);
        return "Vulkan physical-device enumeration failed.";
    }
    const VkPhysicalDevice physical = devices[0];

    VkPhysicalDeviceProperties properties{};
    vkGetPhysicalDeviceProperties(physical, &properties);

    uint32_t queueCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physical, &queueCount, nullptr);
    VkQueueFamilyProperties queues[32]{};
    const uint32_t queueCapacity = std::min(queueCount, 32u);
    if (queueCapacity > 0) {
        vkGetPhysicalDeviceQueueFamilyProperties(physical, &queueCount, queues);
    }
    bool graphicsQueue = false;
    for (uint32_t i = 0; i < std::min(queueCount, 32u); ++i) {
        if ((queues[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
            graphicsQueue = true;
            break;
        }
    }

    VkPhysicalDeviceMemoryProperties memory{};
    vkGetPhysicalDeviceMemoryProperties(physical, &memory);
    uint64_t deviceLocalBytes = 0;
    for (uint32_t i = 0; i < memory.memoryHeapCount; ++i) {
        if ((memory.memoryHeaps[i].flags & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
            deviceLocalBytes += memory.memoryHeaps[i].size;
        }
    }

    std::ostringstream out;
    out << "Vulkan " << VK_VERSION_MAJOR(properties.apiVersion) << "."
        << VK_VERSION_MINOR(properties.apiVersion)
        << " | " << properties.deviceName
        << " | graphics queue: " << (graphicsQueue ? "yes" : "no")
        << " | device-local heap: " << (deviceLocalBytes / (1024ull * 1024ull)) << " MB"
        << " | max 2D image: " << properties.limits.maxImageDimension2D;
    vkDestroyInstance(instance, nullptr);
    return out.str();
}

float clampf(float value, float low, float high) {
    return std::max(low, std::min(value, high));
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_jarvis_aether_AetherNative_nativeProbe(JNIEnv* env, jobject) {
    const std::string result = probeVulkan();
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_jarvis_aether_AetherNative_nativePlan(
        JNIEnv* env, jobject, jint memoryClassMb, jboolean lowMemory,
        jint thermalStatus, jboolean foreground, jboolean batterySaver,
        jint trimPressure) {

    // This is an app-owned working-set target, not a system/GPU memory reservation.
    const float appClass = static_cast<float>(std::max(64, memoryClassMb));
    float budgetMb = clampf(appClass * 0.20f, 48.0f, 192.0f);
    if (lowMemory == JNI_TRUE) budgetMb *= 0.65f;
    if (trimPressure >= 2) budgetMb *= 0.70f;
    if (trimPressure >= 3) budgetMb *= 0.55f;

    // Android thermal status: NONE=0, LIGHT=1, MODERATE=2, SEVERE=3,
    // CRITICAL=4, EMERGENCY=5, SHUTDOWN=6.
    float renderScale = 1.0f;
    int targetFps = 30;
    if (thermalStatus >= 1) { renderScale = 0.92f; targetFps = 30; }
    if (thermalStatus >= 2) { renderScale = 0.82f; targetFps = 30; }
    if (thermalStatus >= 3) { renderScale = 0.68f; targetFps = 24; }
    if (thermalStatus >= 4) { renderScale = 0.55f; targetFps = 15; }
    if (thermalStatus >= 5) { renderScale = 0.40f; targetFps = 10; }
    if (batterySaver == JNI_TRUE) {
        renderScale = std::min(renderScale, 0.75f);
        targetFps = std::min(targetFps, 24);
    }
    if (foreground != JNI_TRUE) {
        renderScale = 0.0f;
        targetFps = 0;
        budgetMb = std::min(budgetMb, 24.0f);
    }
    if (trimPressure >= 3) {
        renderScale = std::min(renderScale, 0.70f);
        targetFps = std::min(targetFps, 20);
    }

    const float maxResidentMb = clampf(budgetMb, 24.0f, 192.0f);
    const float maxDrawCalls = clampf(maxResidentMb * 42.0f, 900.0f, 8000.0f);
    const float maxTriangles = clampf(maxResidentMb * 18000.0f, 250000.0f, 3500000.0f);
    const jfloat values[5] = {
        renderScale, static_cast<float>(targetFps), maxResidentMb,
        maxDrawCalls, maxTriangles
    };

    jfloatArray output = env->NewFloatArray(5);
    if (output != nullptr) env->SetFloatArrayRegion(output, 0, 5, values);
    ALOGI("Plan: scale=%.2f fps=%d budget=%.1fMB", renderScale, targetFps,
          static_cast<int>(maxResidentMb));
    return output;
}
