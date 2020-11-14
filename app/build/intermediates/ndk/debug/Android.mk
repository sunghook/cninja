LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := mame4droid-jni
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	C:\Users\golia\AndroidStudioProjects\MAME4droid\app\src\main\jni\Android.mk \
	C:\Users\golia\AndroidStudioProjects\MAME4droid\app\src\main\jni\Application.mk \
	C:\Users\golia\AndroidStudioProjects\MAME4droid\app\src\main\jni\jni.sh \
	C:\Users\golia\AndroidStudioProjects\MAME4droid\app\src\main\jni\mame4droid-jni.c \

LOCAL_C_INCLUDES += C:\Users\golia\AndroidStudioProjects\MAME4droid\app\src\debug\jni
LOCAL_C_INCLUDES += C:\Users\golia\AndroidStudioProjects\MAME4droid\app\src\main\jni

include $(BUILD_SHARED_LIBRARY)
